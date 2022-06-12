package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class BungeeNetworkMessageListener implements Listener {
	private static final List<NetworkRelayAPI.ServerType> ACCEPTED_SERVER_TYPES;

	static {
		List<NetworkRelayAPI.ServerType> acceptedServerTypes = new ArrayList<>();
		acceptedServerTypes.add(NetworkRelayAPI.ServerType.ALL);
		acceptedServerTypes.add(NetworkRelayAPI.ServerType.BUNGEE);
		ACCEPTED_SERVER_TYPES = acceptedServerTypes;
	}

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

		@Nullable JsonPrimitive serverTypeJson = data.getAsJsonPrimitive("server_type");
		if (serverTypeJson != null) {
			@Nullable String serverTypeString = serverTypeJson.getAsString();
			if (serverTypeString != null) {
				NetworkRelayAPI.ServerType commandType;
				commandType = NetworkRelayAPI.ServerType.fromString(serverTypeString);
				if (!ACCEPTED_SERVER_TYPES.contains(commandType)) {
					return;
				}
			}
		}

		final String command = data.get("command").getAsString();
		mLogger.fine("Executing command'" + command + "' from source '" + event.getSource() + "'");

		ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), data.get("command").getAsString());
	}
}
