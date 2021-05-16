package com.playmonumenta.networkrelay;

import java.util.Set;

import com.google.gson.JsonObject;

public class NetworkRelayAPI {
	public static final String COMMAND_CHANNEL = "monumentanetworkrelay.command";
	public static final String HEARTBEAT_CHANNEL = "monumentanetworkrelay.heartbeat";

	public static void sendMessage(String destination, String channel, JsonObject data) throws Exception {
		getInstance().sendNetworkMessage(destination, channel, data);
	}

	public static void sendBroadcastMessage(String channel, JsonObject data) throws Exception {
		sendMessage("*", channel, data);
	}

	public static void sendCommand(String destination, String command) throws Exception {
		JsonObject data = new JsonObject();
		data.addProperty("command", command);
		sendMessage(destination, COMMAND_CHANNEL, data);
	}

	public static void sendBroadcastCommand(String command) throws Exception {
		sendCommand("*", command);
	}

	public static String getShardName() throws Exception {
		return getInstance().getShardName();
	}

	public static Set<String> getOnlineShardNames() throws Exception {
		return getInstance().getOnlineShardNames();
	}

	private static RabbitMQManager getInstance() throws Exception {
		RabbitMQManager instance = RabbitMQManager.getInstance();
		if (instance == null) {
			throw new Exception("NetworkRelay is not connected to rabbitmq");
		}
		return instance;
	}
}
