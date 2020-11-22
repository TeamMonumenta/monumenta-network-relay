package com.playmonumenta.relay;

import java.time.Instant;

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

		// Get timestamp player earned the advancement
		Instant instant = DataPackUtils.getEarnedInstant(player, advancement);

		String announceElsewhereCommand = "broadcastcommand execute unless entity " + player.getName() + " run " + announcementCommand;
		mPlugin.getServer().dispatchCommand(mPlugin.getServer().getConsoleSender(), announceElsewhereCommand);

		player.sendMessage("You earned " + advancementId + " at " + instant.toString() + ", or as a timestamp " + Long.toString(instant.toEpochMilli()));
	}
}
