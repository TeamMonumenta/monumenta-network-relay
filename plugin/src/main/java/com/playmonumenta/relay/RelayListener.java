package com.playmonumenta.relay;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class RelayListener implements Listener {
	Plugin mPlugin = null;

	public RelayListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (ServerProperties.getJoinMessagesEnabled() == false) {
			event.setJoinMessage("");
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (!ServerProperties.getJoinMessagesEnabled()) {
			event.setQuitMessage("");
		}

		Player player = event.getPlayer();
	}

	// The player has died
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
	}
}
