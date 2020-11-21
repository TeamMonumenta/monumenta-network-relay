package com.playmonumenta.relay;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.Plugin;

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

		player.sendMessage(advancementId);
	}
}
