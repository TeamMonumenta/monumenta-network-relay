package com.playmonumenta.relay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
	Plugin mPlugin = null;
	Map<String, AdvancementRecord> mRecords = new HashMap<String, AdvancementRecord>();

	public AdvancementListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerAdvancementDoneEvent(PlayerAdvancementDoneEvent event) {
		Player player = event.getPlayer();

		Advancement advancement = event.getAdvancement();
		String advancementId = advancement.getKey().toString();

		if (!DataPackUtils.isAnnouncedToChat(advancement)) {
			return;
		}

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

		// Check if this was the first time this advancement was earned (that we know of)
		Instant instant = DataPackUtils.getEarnedInstant(player, advancement);

		// Announce that the player earned the advancement to other shards
		String announceElsewhereCommand = "broadcastcommand execute unless entity " + player.getName() + " run " + announcementCommand;
		mPlugin.getServer().dispatchCommand(mPlugin.getServer().getConsoleSender(), announceElsewhereCommand);

		// Prepare to run the appropriate function for earning this advancement
		String playerName = player.getName();
		String playerTeam = "NoTeam";
		if (DataPackUtils.getTeam(player) != null) {
			playerTeam = DataPackUtils.getTeam(player).getName();
		}
		Map<String, String> commandReplacements = new HashMap<String, String>();
		commandReplacements.put("[Player]", playerName);
		commandReplacements.put("[Team]", playerTeam);

		AdvancementRecord record = mRecords.get(advancementId);
		if (record == null) {
			// First time this advancement was earned! As far as we know anyways.
			record = new AdvancementRecord(player, advancement);
			mRecords.put(advancementId, record);
			DataPackUtils.runFunctionWithReplacements("rivals", "advancement/earned_first", commandReplacements);
		} else {
			// Not the first, but credit where it's due.
			DataPackUtils.runFunctionWithReplacements("rivals", "advancement/earned_later", commandReplacements);
		}
	}
}
