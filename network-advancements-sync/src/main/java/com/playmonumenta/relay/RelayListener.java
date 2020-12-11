package com.playmonumenta.relay;

import com.playmonumenta.relay.network.SocketManager;

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
	public void playerJoinEvent(PlayerJoinEvent event) throws Exception {
		Player player = event.getPlayer();
		String message = event.getJoinMessage();

		if (ServerProperties.getJoinMessagesEnabled() && message != null && !message.isEmpty()) {
			SocketManager.broadcastCommand(mPlugin, "tellraw @a \"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
		}
		event.setJoinMessage("");
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerQuitEvent(PlayerQuitEvent event) throws Exception {
		Player player = event.getPlayer();
		String message = event.getQuitMessage();

		if (ServerProperties.getJoinMessagesEnabled() && message != null && !message.isEmpty()) {
			SocketManager.broadcastCommand(mPlugin, "tellraw @a \"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
		}
		event.setQuitMessage("");
	}

	// The player has died
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerDeathEvent(PlayerDeathEvent event) throws Exception {
		Player player = event.getEntity();
		String message = event.getDeathMessage();

		if (message != null && !message.isEmpty()) {
			SocketManager.broadcastCommand(mPlugin, "tellraw @a \"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
		}
		event.setDeathMessage("");
	}
}
