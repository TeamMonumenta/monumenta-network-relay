package com.playmonumenta.relay;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.Map;
import java.util.Set;

import com.playmonumenta.relay.network.SocketManager;
import com.playmonumenta.relay.utils.DataPackUtils;
import com.playmonumenta.relay.utils.FileUtils;

import org.bukkit.advancement.Advancement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AdvancementManager implements Listener {
	private static AdvancementManager INSTANCE = null;
	private static Plugin mPlugin = null;
	private static File mConfigFile;
	private static Map<String, String> mWatchedTeams = new HashMap<String, String>();
	private static Map<String, AdvancementRecord> mRecords = new HashMap<String, AdvancementRecord>();

	private AdvancementManager(Plugin plugin) {
		INSTANCE = this;
		mPlugin = plugin;
		mConfigFile = new File(plugin.getDataFolder(), "advancementRecords.json");
	}

	public static AdvancementManager getInstance() {
		return INSTANCE;
	}

	public static AdvancementManager getInstance(Plugin plugin) {
		if (INSTANCE == null) {
			INSTANCE = new AdvancementManager(plugin);
		}
		return INSTANCE;
	}

	public void reload() {
		// Replace current state with previously saved state.
		mWatchedTeams.clear();
		mRecords.clear();

		// Load all records as json
		JsonObject allRecords;
		try {
			allRecords = FileUtils.readJson(mConfigFile.getPath());
		} catch (Exception e) {
			mPlugin.getLogger().warning("No advancement records could be loaded - assuming the plugin was not previously installed.");
			return;
		}

		// Load the json into our local copy of advancement records
		try {
			JsonObject teams = allRecords.getAsJsonObject("teams");
			for (Map.Entry<String, JsonElement> teamPair : teams.entrySet()) {
				String teamId = teamPair.getKey();
				String teamDisplayName = teamPair.getValue().getAsJsonObject().getAsJsonPrimitive("displayName").getAsString();
				mWatchedTeams.put(teamId, teamDisplayName);
			}

			JsonObject records = allRecords.getAsJsonObject("records");
			for (Map.Entry<String, JsonElement> entry : records.entrySet()) {
				String advancementId = entry.getKey();

				JsonObject recordJsonObject = entry.getValue().getAsJsonObject();
				AdvancementRecord record = new AdvancementRecord(recordJsonObject);

				mRecords.put(advancementId, record);
			}
		} catch (Exception e) {
			mPlugin.getLogger().log(Level.SEVERE, "Failed to load at least one advancement record. Aborting load.");
			return;
		}

		broadcastAllAdvancementRecords();

		try {
			SocketManager.broadcastAdvancementRecordRequest(mPlugin);
		} catch (Exception e) {
			mPlugin.getLogger().log(Level.SEVERE, "Failed to request remote records.");
		}
	}

	public void saveState() {
		JsonObject allRecords = new JsonObject();

		JsonObject teamsJson = new JsonObject();
		for (Map.Entry<String, String> entry : mWatchedTeams.entrySet()) {
			String teamId = entry.getKey();
			String teamDisplayName = entry.getValue();
			Team team = DataPackUtils.getTeam(teamId);

			String teamColor = null;
			String teamPrefix = null;
			String teamSuffix = null;
			JsonArray teamMembers = new JsonArray();
			if (team != null) {
				if (team != null) {
					if (team.getColor() != null) {
						teamColor = team.getColor().name().toLowerCase();
					}
					teamPrefix = team.getPrefix();
					teamSuffix = team.getSuffix();
					for (String member : team.getEntries()) {
						teamMembers.add(member);
					}
				}
			}
			if (teamColor == null) {
				teamColor = "reset";
			}
			if (teamPrefix == null) {
				teamPrefix = "";
			}
			if (teamSuffix == null) {
				teamSuffix = "";
			}

			JsonObject teamJson = new JsonObject();
			teamJson.addProperty("displayName", teamDisplayName);
			teamJson.addProperty("prefix", teamPrefix);
			teamJson.addProperty("suffix", teamSuffix);
			teamJson.addProperty("color", teamColor);
			teamJson.add("members", teamMembers);

			teamsJson.add(teamId, teamJson);
		}
		allRecords.add("teams", teamsJson);

		JsonObject records = new JsonObject();
		for (Map.Entry<String, AdvancementRecord> entry : mRecords.entrySet()) {
			String advancementId = entry.getKey();

			AdvancementRecord record = entry.getValue();
			JsonObject recordObject = record.toJson();

			records.add(advancementId, recordObject);
		}
		allRecords.add("records", records);

		try {
			FileUtils.writeJson(mConfigFile.getPath(), allRecords);
		} catch (Exception e) {
			mPlugin.getLogger().log(Level.SEVERE, "Failed to save advancement records");
		}
	}

	public void watchTeamId(String teamId, String displayName) {
		if (teamId == null) {
			return;
		}
		mWatchedTeams.put(teamId, displayName);
	}

	public void broadcastAllAdvancementRecords() {
		// Broadcast local advancement records; remote servers will ignore non-changes
		for (Map.Entry<String, AdvancementRecord> recordPair : mRecords.entrySet()) {
			String advancementId = recordPair.getKey();
			AdvancementRecord record = recordPair.getValue();

			try {
				SocketManager.broadcastAdvancementRecord(mPlugin, advancementId, record);
			} catch (Exception e) {
				mPlugin.getLogger().warning("Failed to broadcast record");
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void worldSaveEvent(WorldSaveEvent event) {
		saveState();
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerAdvancementDoneEvent(PlayerAdvancementDoneEvent event) throws Exception {
		Player player = event.getPlayer();
		Team team = DataPackUtils.getTeam(player);
		String teamId = "NoTeam";
		if (team != null) {
			teamId = team.getName();
		}

		if (!mWatchedTeams.containsKey(teamId)) {
			return;
		}

		Advancement advancement = event.getAdvancement();
		String advancementId = advancement.getKey().toString();

		// Announce that the player earned the advancement to other shards
		JsonObject advancementJson = null;
		boolean announceByTeam = false;
		String announcementCommand = null;
		for (JsonObject testAdvancementJson : DataPackUtils.getAdvancementJsonObjects(advancement)) {
			if (DataPackUtils.isTeamAnnouncedToChat(testAdvancementJson)) {
				announceByTeam = true;
			} else if (DataPackUtils.isAnnouncedToChat(testAdvancementJson)) {
				announceByTeam = false;
			} else {
				continue;
			}

			announcementCommand = DataPackUtils.getChatAnnouncement(player, testAdvancementJson);
			if (announcementCommand != null) {
				advancementJson = testAdvancementJson;
				break;
			}
		}
		if (announcementCommand == null) {
			return;
		}

		// Process the advancement records
		AdvancementRecord newRecord = new AdvancementRecord(player, advancement);
		AdvancementRecord oldRecord = mRecords.get(advancementId);
		if (oldRecord == null) {
			// First time this advancement was earned! As far as we know anyways.
			String announceElsewhereCommand = "execute unless entity " + player.getName() + " run " + announcementCommand;
			SocketManager.broadcastCommand(mPlugin, announceElsewhereCommand);

			mRecords.put(advancementId, newRecord);
			runRecordChangeFunctions(advancementId, advancementJson, newRecord, null);
			SocketManager.broadcastAdvancementRecord(mPlugin, advancementId, newRecord);
		} else {
			// Not the first, but credit where it's due.
			if (announceByTeam) {
				if (false /*Confirm this is the first time it's earned for the team*/) {
					SocketManager.broadcastCommand(mPlugin, announcementCommand);
				}
			} else {
				String announceElsewhereCommand = "execute unless entity " + player.getName() + " run " + announcementCommand;
				SocketManager.broadcastCommand(mPlugin, announceElsewhereCommand);
			}

			AdvancementRecord updatedRecord = oldRecord.cloneAndUpdate(newRecord);
			mRecords.put(advancementId, updatedRecord);
			runRecordChangeFunctions(advancementId, advancementJson, newRecord, oldRecord);
			SocketManager.broadcastAdvancementRecord(mPlugin, advancementId, updatedRecord);
		}

		saveState();
	}

	public void addRemoteRecord(String advancementId, AdvancementRecord remoteRecord) {
		if (advancementId == null || remoteRecord == null) {
			return;
		}

		JsonObject advancementJson = null;
		Advancement advancement = Bukkit.getAdvancement​(DataPackUtils.getNamespacedKey(advancementId));
		for (JsonObject testAdvancementJson : DataPackUtils.getAdvancementJsonObjects(advancement)) {
			if (!DataPackUtils.isTeamAnnouncedToChat(testAdvancementJson) && !DataPackUtils.isAnnouncedToChat(testAdvancementJson)) {
				continue;
			}

			JsonObject chatAdvancement = DataPackUtils.getChatAdvancement(testAdvancementJson);
			if (chatAdvancement != null) {
				advancementJson = testAdvancementJson;
				break;
			}
		}

		AdvancementRecord localRecord = mRecords.get(advancementId);
		if (localRecord == null) {
			// The other server got it first! I'm sure we'll go first next time.
			mRecords.put(advancementId, remoteRecord);
			runRecordChangeFunctions(advancementId, advancementJson, remoteRecord, null);
		} else {
			AdvancementRecord updatedRecord = localRecord.cloneAndUpdate(remoteRecord);
			mRecords.put(advancementId, updatedRecord);
			runRecordChangeFunctions(advancementId, advancementJson, remoteRecord, localRecord);
		}

		saveState();
	}

	private void runRecordChangeFunctions(String advancementId, JsonObject advancementJson, AdvancementRecord newRecord, AdvancementRecord oldRecord) {
		if (advancementId == null || newRecord == null) {
			return;
		}

		applyFunctionsToPlayerTeams(advancementId, advancementJson, newRecord.getNewlyFirstPlayers(oldRecord), "rivals", "advancement/first_player", true);
		applyFunctionsToPlayerTeams(advancementId, advancementJson, newRecord.getNewlyLaterPlayers(oldRecord), "rivals", "advancement/later_player", true);
		applyFunctionsToPlayerTeams(advancementId, advancementJson, newRecord.getCorrectedPlayers(oldRecord), "rivals", "advancement/correct_player", true);
		applyFunctionsToTeams(advancementId, advancementJson, newRecord.getNewlyFirstTeams(oldRecord), "rivals", "advancement/first_team", true);
		applyFunctionsToTeams(advancementId, advancementJson, newRecord.getNewlyLaterTeams(oldRecord), "rivals", "advancement/later_team", true);
		applyFunctionsToTeams(advancementId, advancementJson, newRecord.getCorrectedTeams(oldRecord), "rivals", "advancement/correct_team", true);
	}

	private void applyFunctionsToPlayerTeams(String advancementId, JsonObject advancementJson, Set<Map.Entry<String, Set<String>>> playerTeams, String namespace, String functionKey, boolean functionTag) {
		for (Map.Entry<String, Set<String>> entry : playerTeams) {
			String teamId = entry.getKey();
			for (String playerName : entry.getValue()) {
				DataPackUtils.runFunctionWithReplacements(namespace,
				                                          functionKey,
				                                          functionTag,
				                                          getCommandReplacements(advancementId, advancementJson, teamId, playerName));
			}
		}
	}

	private void applyFunctionsToTeams(String advancementId, JsonObject advancementJson, Set<String> teams, String namespace, String functionKey, boolean functionTag) {
		for (String teamId : teams) {
			DataPackUtils.runFunctionWithReplacements(namespace,
			                                          functionKey,
			                                          functionTag,
			                                          getCommandReplacements(advancementId, advancementJson, teamId));
		}
	}

	private Map<String, String> getCommandReplacements(String advancementId, JsonObject advancementJson, String teamId, String playerName) {
		Team team = DataPackUtils.getTeam(teamId);
		String teamColor = null;
		String teamDisplayName = mWatchedTeams.get(teamId);
		String teamPrefix = null;
		String teamSuffix = null;
		if (team != null) {
			if (team.getColor() != null) {
				teamColor = team.getColor().name().toLowerCase();
			}
			if (teamDisplayName == null) {
				teamDisplayName = team.getDisplayName();
			}
			teamPrefix = team.getPrefix();
			teamSuffix = team.getSuffix();
		}
		if (teamColor == null) {
			teamColor = "reset";
		}
		if (teamDisplayName == null) {
			teamDisplayName = teamId;
		}
		if (teamPrefix == null) {
			teamPrefix = "";
		}
		if (teamSuffix == null) {
			teamSuffix = "";
		}

		String rawJsonAdvancement = null;
		if (advancementJson != null) {
			rawJsonAdvancement = DataPackUtils.getChatAdvancement(advancementJson).toString();
		}
		if (rawJsonAdvancement == null) {
			rawJsonAdvancement = "\"" + advancementId + "\"";
		}

		Map<String, String> commandReplacements = new HashMap<String, String>();
		commandReplacements.put("__advancement__", advancementId);
		commandReplacements.put("\"__raw_json_advancement__\"", rawJsonAdvancement);
		commandReplacements.put("__player__", playerName);
		commandReplacements.put("__team_id__", teamId);
		commandReplacements.put("__team_color__", teamColor);
		commandReplacements.put("\"__team_display_name__\"", teamDisplayName);
		commandReplacements.put("__team_prefix__", teamPrefix);
		commandReplacements.put("__team_suffix__", teamSuffix);

		return commandReplacements;
	}

	private Map<String, String> getCommandReplacements(String advancementId, JsonObject advancementJson, String teamId) {
		Team team = DataPackUtils.getTeam(teamId);
		String teamColor = null;
		String teamDisplayName = mWatchedTeams.get(teamId);
		String teamPrefix = null;
		String teamSuffix = null;
		if (team != null) {
			if (team.getColor() != null) {
				teamColor = team.getColor().name().toLowerCase();
			}
			if (teamDisplayName == null) {
				teamDisplayName = team.getDisplayName();
			}
			teamPrefix = team.getPrefix();
			teamSuffix = team.getSuffix();
		}
		if (teamColor == null) {
			teamColor = "reset";
		}
		if (teamDisplayName == null) {
			teamDisplayName = teamId;
		}
		if (teamPrefix == null) {
			teamPrefix = "";
		}
		if (teamSuffix == null) {
			teamSuffix = "";
		}

		String rawJsonAdvancement = null;
		if (advancementJson != null) {
			rawJsonAdvancement = DataPackUtils.getChatAdvancement(advancementJson).toString();
		}
		if (rawJsonAdvancement == null) {
			rawJsonAdvancement = "\"" + advancementId + "\"";
		}

		Map<String, String> commandReplacements = new HashMap<String, String>();
		commandReplacements.put("__advancement__", advancementId);
		commandReplacements.put("\"__raw_json_advancement__\"", rawJsonAdvancement);
		commandReplacements.put("__team_id__", teamId);
		commandReplacements.put("__team_color__", teamColor);
		commandReplacements.put("\"__team_display_name__\"", teamDisplayName);
		commandReplacements.put("__team_prefix__", teamPrefix);
		commandReplacements.put("__team_suffix__", teamSuffix);

		return commandReplacements;
	}
}
