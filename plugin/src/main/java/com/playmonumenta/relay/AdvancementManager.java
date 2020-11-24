package com.playmonumenta.relay;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.Map;
import java.util.Set;

import com.playmonumenta.relay.network.SocketManager;
import com.playmonumenta.relay.utils.DataPackUtils;
import com.playmonumenta.relay.utils.FileUtils;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.Plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AdvancementManager implements Listener {
	private static AdvancementManager INSTANCE = null;
	private static Plugin mPlugin = null;
	private static File mConfigFile;
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
			for (Map.Entry<String, JsonElement> entry : allRecords.entrySet()) {
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
		for (Map.Entry<String, AdvancementRecord> entry : mRecords.entrySet()) {
			String advancementId = entry.getKey();

			AdvancementRecord record = entry.getValue();
			JsonObject recordObject = record.toJson();

			allRecords.add(advancementId, recordObject);
		}
		try {
			FileUtils.writeJson(mConfigFile.getPath(), allRecords);
		} catch (Exception e) {
			mPlugin.getLogger().log(Level.SEVERE, "Failed to save advancement records");
		}
	}

	public void broadcastAllAdvancementRecords() {
		// Broadcast local advancement records; remote servers will ignore non-changes
		for (AdvancementRecord record : mRecords.values()) {
			try {
				SocketManager.broadcastAdvancementRecord(mPlugin, record);
			} catch (Exception e) {
				mPlugin.getLogger().warning("Failed to broadcast record");
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerAdvancementDoneEvent(PlayerAdvancementDoneEvent event) throws Exception {
		Player player = event.getPlayer();

		Advancement advancement = event.getAdvancement();
		String advancementId = advancement.getKey().toString();

		if (!DataPackUtils.isAnnouncedToChat(advancement)) {
			return;
		}

		// Announce that the player earned the advancement to other shards
		String announcementCommand = null;
		for (JsonObject advancementJson : DataPackUtils.getAdvancementJsonObjects(advancement)) {
			announcementCommand = DataPackUtils.getChatAnnouncement(player, advancementJson);
			if (announcementCommand != null) {
				break;
			}
		}
		if (announcementCommand == null || announcementCommand.isEmpty()) {
			return;
		}

		String announceElsewhereCommand = "broadcastcommand execute unless entity " + player.getName() + " run " + announcementCommand;
		mPlugin.getServer().dispatchCommand(mPlugin.getServer().getConsoleSender(), announceElsewhereCommand);

		// Process the advancement records
		AdvancementRecord newRecord = new AdvancementRecord(player, advancement);
		AdvancementRecord oldRecord = mRecords.get(advancementId);
		if (oldRecord == null) {
			// First time this advancement was earned! As far as we know anyways.
			mRecords.put(advancementId, newRecord);

			applyEarnedFirst(newRecord.getFirstPlayerTeams().entrySet());

			SocketManager.broadcastAdvancementRecord(mPlugin, newRecord);
		} else {
			// Not the first, but credit where it's due.
			AdvancementRecord updatedRecord = oldRecord.cloneAndUpdate(newRecord);
			mRecords.put(advancementId, updatedRecord);

			applyEarnedFirst(oldRecord.getNewlyFirstPlayerTeams(newRecord));
			applyEarnedLater(oldRecord.getNewlyLaterPlayerTeams(newRecord));
			applyCorrectedLater(oldRecord.getCorrectedLaterPlayerTeams(newRecord));

			SocketManager.broadcastAdvancementRecord(mPlugin, updatedRecord);
		}
	}

	public void addRemoteRecord(AdvancementRecord remoteRecord) {
		if (remoteRecord == null) {
			return;
		}

		String advancementId = remoteRecord.getAdvancement();

		AdvancementRecord localRecord = mRecords.get(advancementId);
		if (localRecord == null) {
			// The other server got it first! I'm sure we'll go first next time.
			mRecords.put(advancementId, remoteRecord);
			applyEarnedFirst(remoteRecord.getFirstPlayerTeams().entrySet());
			applyEarnedLater(remoteRecord.getLaterPlayerTeams().entrySet());
		} else {
			AdvancementRecord updatedRecord = localRecord.cloneAndUpdate(remoteRecord);
			mRecords.put(advancementId, updatedRecord);
			applyEarnedFirst(localRecord.getNewlyFirstPlayerTeams(remoteRecord));
			applyEarnedLater(localRecord.getNewlyLaterPlayerTeams(remoteRecord));
			applyCorrectedLater(localRecord.getCorrectedLaterPlayerTeams(remoteRecord));
		}
	}

	private void applyEarnedFirst(Set<Map.Entry<String, String>> playerTeams) {
		for (Map.Entry<String, String> entry : playerTeams) {
			DataPackUtils.runFunctionWithReplacements("rivals",
			                                          "advancement/earned_first",
			                                          getCommandReplacements(entry));
		}
	}

	private void applyEarnedLater(Set<Map.Entry<String, String>> playerTeams) {
		for (Map.Entry<String, String> entry : playerTeams) {
			DataPackUtils.runFunctionWithReplacements("rivals",
			                                          "advancement/earned_later",
			                                          getCommandReplacements(entry));
		}
	}

	private void applyCorrectedLater(Set<Map.Entry<String, String>> playerTeams) {
		for (Map.Entry<String, String> entry : playerTeams) {
			DataPackUtils.runFunctionWithReplacements("rivals",
			                                          "advancement/not_actually_earned_first",
			                                          getCommandReplacements(entry));
		}
	}

	private Map<String, String> getCommandReplacements(Map.Entry<String, String> playerTeamPair) {
		String playerName = playerTeamPair.getKey();
		String playerTeam = playerTeamPair.getValue();

		Map<String, String> commandReplacements = new HashMap<String, String>();
		commandReplacements.put("[Player]", playerName);
		commandReplacements.put("[Team]", playerTeam);

		return commandReplacements;
	}
}
