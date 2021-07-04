package com.playmonumenta.networkrelay;

import java.util.logging.Logger;

import com.google.gson.JsonObject;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class BungeeNetworkMessageListener implements Listener {
	private final Logger mLogger;

	protected BungeeNetworkMessageListener(Logger logger) {
		mLogger = logger;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void networkRelayMessageEvent(NetworkRelayMessageEventBungee event) {
		if (!event.getChannel().equals(NetworkRelayAPI.COMMAND_CHANNEL)) {
			return;
		}

		JsonObject data = event.getData();
		if (!data.has("command") ||
			!data.get("command").isJsonPrimitive() ||
			!data.getAsJsonPrimitive("command").isString()) {
			mLogger.warning("Got invalid command message with no actual command");
			return;
		}

		final String command = data.get("command").getAsString();
		mLogger.fine("Executing command'" + command + "' from source '" + event.getSource() + "'");

		ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), data.get("command").getAsString());
	}
}
