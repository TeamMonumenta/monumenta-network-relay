package com.playmonumenta.relay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.playmonumenta.relay.network.SocketManager;
import com.playmonumenta.relay.utils.DataPackUtils;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.Plugin;

import com.google.gson.JsonObject;

public class AdvancementListener implements Listener {
	private Plugin mPlugin = null;
	private static AdvancementListener INSTANCE = null;
	private Map<String, AdvancementRecord> mRecords = new HashMap<String, AdvancementRecord>();

	private AdvancementListener(Plugin plugin) {
		mPlugin = plugin;
		INSTANCE = this;
	}

	public static AdvancementListener getInstance() {
		return INSTANCE;
	}

	public static AdvancementListener getInstance(Plugin plugin) {
		if (INSTANCE == null) {
			INSTANCE = new AdvancementListener(plugin);
		}
		return INSTANCE;
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

			SocketManager.recordAdvancement(mPlugin, newRecord);
		} else {
			// Not the first, but credit where it's due.
			AdvancementRecord updatedRecord = oldRecord.cloneAndUpdate(newRecord);
			mRecords.put(advancementId, updatedRecord);

			applyEarnedFirst(oldRecord.getNewlyFirstPlayerTeams(newRecord));
			applyEarnedLater(oldRecord.getNewlyLaterPlayerTeams(newRecord));
			applyCorrectedLater(oldRecord.getCorrectedLaterPlayerTeams(newRecord));

			SocketManager.recordAdvancement(mPlugin, updatedRecord);
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
