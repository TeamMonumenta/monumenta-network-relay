package com.playmonumenta.networkrelay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class NetworkRelayAPI {
	public enum ServerType {
		BUNGEE("bungee"),
		MINECRAFT("minecraft"),
		ALL("all");

		final String mId;

		ServerType(String id) {
			mId = id;
		}

		@Override
		public String toString() {
			return mId;
		}

		public static ServerType fromString(@Nullable String id) {
			for (ServerType serverType : ServerType.values()) {
				if (serverType.toString().equals(id)) {
					return serverType;
				}
			}
			return ALL;
		}
	}

	public static final String COMMAND_CHANNEL = "monumentanetworkrelay.command";
	public static final String HEARTBEAT_CHANNEL = "monumentanetworkrelay.heartbeat";
	protected static final String NETWORK_RELAY_HEARTBEAT_IDENTIFIER = "monumentanetworkrelay";

	public static void sendMessage(String destination, String channel, JsonObject data) throws Exception {
		getInstance().sendNetworkMessage(destination, channel, data);
	}

	public static void sendBroadcastMessage(String channel, JsonObject data) throws Exception {
		sendMessage("*", channel, data);
	}

	public static void sendCommand(String destination, String command) throws Exception {
		sendCommand(destination, command, ServerType.ALL);
	}

	public static void sendCommand(String destination, String command, ServerType serverType) throws Exception {
		JsonObject data = new JsonObject();
		data.addProperty("server_type", serverType.toString());
		data.addProperty("command", command);
		sendMessage(destination, COMMAND_CHANNEL, data);
	}

	public static void sendBroadcastCommand(String command) throws Exception {
		sendCommand("*", command, ServerType.ALL);
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
		sendExpiringCommand(destination, command, ttlSeconds, ServerType.ALL);
	}

	public static void sendExpiringCommand(String destination, String command, long ttlSeconds, ServerType serverType) throws Exception {
		JsonObject data = new JsonObject();
		data.addProperty("server_type", serverType.toString());
		data.addProperty("command", command);
		sendExpiringMessage(destination, COMMAND_CHANNEL, data, ttlSeconds);
	}

	public static void sendExpiringBroadcastCommand(String command, long ttlSeconds) throws Exception {
		sendExpiringCommand("*", command, ttlSeconds, ServerType.ALL);
	}

	public static void sendExpiringBroadcastCommand(String command, long ttlSeconds, ServerType serverType) throws Exception {
		sendExpiringCommand("*", command, ttlSeconds, serverType);
	}

	public static String getShardName() {
		return getInstance().getShardName();
	}

	public static Set<String> getOnlineShardNames() {
		return getInstance().getOnlineShardNames();
	}

	public static Set<String> getOnlinePlayerNames(boolean visibleOnly) {
		RemotePlayerManager manager = RemotePlayerManager.getInstance();
		if (manager == null) {
			return new HashSet<>();
		}
		return manager.getAllOnlinePlayersName(visibleOnly);
	}

	public static Optional<String> getPlayerShard(String playerName) {
		RemotePlayerManager manager = RemotePlayerManager.getInstance();
		if (manager == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(manager.getPlayerShard(playerName));
	}

	public static Optional<String> getPlayerShard(UUID playerUuid) {
		RemotePlayerManager manager = RemotePlayerManager.getInstance();
		if (manager == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(manager.getPlayerShard(playerUuid));
	}

	public static Optional<RemotePlayerManager.RemotePlayer> getRemotePlayer(String playerName) {
		RemotePlayerManager manager = RemotePlayerManager.getInstance();
		if (manager == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(manager.getRemotePlayer(playerName));
	}

	public static Optional<RemotePlayerManager.RemotePlayer> getRemotePlayer(UUID playerUuid) {
		RemotePlayerManager manager = RemotePlayerManager.getInstance();
		if (manager == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(manager.getRemotePlayer(playerUuid));
	}

	public static Optional<Boolean> isPlayerVanished(UUID playerUuid) {
		RemotePlayerManager manager = RemotePlayerManager.getInstance();
		if (manager == null) {
			return Optional.empty();
		}
		return Optional.of(manager.isPlayerVisible(playerUuid));
	}

	public static Optional<Boolean> isPlayerVanished(String playerName) {
		RemotePlayerManager manager = RemotePlayerManager.getInstance();
		if (manager == null) {
			return Optional.empty();
		}
		return Optional.of(manager.isPlayerVisible(playerName));
	}

	/**
	 * Gets the most recent plugin data provided via heartbeat
	 * <p>
	 * Throws an exception only if the plugin isn't loaded or connected to the network relay
	 *
	 * @param shardName Name of the shard to retrieve data for
	 * @param pluginIdentifier Plugin identifier passed to ShardGatherHeartbeatDataEvent
	 *
	 * @return JsonObject stored in the most recent heartbeat, or null if either shardName or pluginIdentifier not found
	 */
	public static @Nullable JsonObject getHeartbeatPluginData(String shardName, String pluginIdentifier) {
		@Nullable JsonObject allShardData = getInstance().getOnlineShardHeartbeatData().get(shardName);
		if (allShardData != null) {
			JsonElement element = allShardData.get(pluginIdentifier);
			if (element != null && element.isJsonObject()) {
				return element.getAsJsonObject();
			}
		}
		return null;
	}

	private static RabbitMQManager getInstance() {
		return RabbitMQManager.getInstance();
	}
}
