package com.playmonumenta.networkrelay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;
import javax.annotation.Nullable;

public class NetworkRelayAPI {
	public enum ServerType {
		BUNGEE("bungee"),
		MINECRAFT("minecraft");

		final String mId;

		ServerType(String id) {
			mId = id;
		}

		@Override
		public String toString() {
			return mId;
		}

		public static @Nullable ServerType fromString(String id) {
			for (ServerType serverType : ServerType.values()) {
				if (serverType.toString().equals(id)) {
					return serverType;
				}
			}
			return null;
		}
	}

	public static final String COMMAND_CHANNEL = "monumentanetworkrelay.command";
	public static final String HEARTBEAT_CHANNEL = "monumentanetworkrelay.heartbeat";

	public static void sendMessage(String destination, String channel, JsonObject data) throws Exception {
		getInstance().sendNetworkMessage(destination, channel, data);
	}

	public static void sendBroadcastMessage(String channel, JsonObject data) throws Exception {
		sendMessage("*", channel, data);
	}

	public static void sendCommand(String destination, String command) throws Exception {
		sendCommand(destination, command, null);
	}

	public static void sendCommand(String destination, String command, ServerType serverType) throws Exception {
		JsonObject data = new JsonObject();
		if (serverType != null) {
			data.addProperty("server_type", serverType.toString());
		}
		data.addProperty("command", command);
		sendMessage(destination, COMMAND_CHANNEL, data);
	}

	public static void sendBroadcastCommand(String command) throws Exception {
		sendCommand("*", command, null);
	}

	public static void sendBroadcastCommand(String command, ServerType serverType) throws Exception {
		sendCommand("*", command, serverType);
	}

	public static void sendExpiringMessage(String destination, String channel, JsonObject data, long ttlSeconds) throws Exception {
		getInstance().sendExpiringNetworkMessage(destination, channel, data, ttlSeconds);
	}

	public static void sendExpiringBroadcastMessage(String channel, JsonObject data, long ttlSeconds) throws Exception {
		sendExpiringMessage("*", channel, data, ttlSeconds);
	}

	public static void sendExpiringCommand(String destination, String command, long ttlSeconds) throws Exception {
		sendExpiringCommand(destination, command, ttlSeconds, null);
	}

	public static void sendExpiringCommand(String destination, String command, long ttlSeconds, @Nullable ServerType serverType) throws Exception {
		JsonObject data = new JsonObject();
		if (serverType != null) {
			data.addProperty("server_type", serverType.toString());
		}
		data.addProperty("command", command);
		sendExpiringMessage(destination, COMMAND_CHANNEL, data, ttlSeconds);
	}

	public static void sendExpiringBroadcastCommand(String command, long ttlSeconds) throws Exception {
		sendExpiringCommand("*", command, ttlSeconds, null);
	}

	public static void sendExpiringBroadcastCommand(String command, long ttlSeconds, ServerType serverType) throws Exception {
		sendExpiringCommand("*", command, ttlSeconds, serverType);
	}

	public static String getShardName() throws Exception {
		return getInstance().getShardName();
	}

	public static Set<String> getOnlineShardNames() throws Exception {
		return getInstance().getOnlineShardNames();
	}

	/**
	 * Gets the most recent plugin data provided via heartbeat
	 *
	 * Throws an exception only if the plugin isn't loaded or connected to the network relay
	 *
	 * @param shardName Name of the shard to retrieve data for
	 * @param pluginIdentifier Plugin identifier passed to ShardGatherHeartbeatDataEvent
	 *
	 * @return JsonObject stored in the most recent heartbeat, or null if either shardName or pluginIdentifier not found
	 */
	public static JsonObject getHeartbeatPluginData(String shardName, String pluginIdentifier) throws Exception {
		JsonObject allShardData = getInstance().getOnlineShardHeartbeatData().get(shardName);
		if (allShardData != null) {
			JsonElement element = allShardData.get(pluginIdentifier);
			if (element != null && element.isJsonObject()) {
				return element.getAsJsonObject();
			}
		}
		return null;
	}

	private static RabbitMQManager getInstance() throws Exception {
		RabbitMQManager instance = RabbitMQManager.getInstance();
		if (instance == null) {
			throw new Exception("NetworkRelay is not connected to rabbitmq");
		}
		return instance;
	}
}
