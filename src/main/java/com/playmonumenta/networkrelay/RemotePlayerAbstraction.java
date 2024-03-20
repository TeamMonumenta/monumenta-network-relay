package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.Nullable;

public abstract class RemotePlayerAbstraction {
	// The ID of the server ID reporting this information for a given server type
	// For example "proxy" could report "bungee-13", or "minecraft" could report "valley-2"
	// This is not used for proxies to report which Minecraft instance is most relevant.
	protected final String mServerId;
	// The player's Minecraft UUID
	protected final UUID mUuid;
	// The player's name
	protected final String mName;
	// Whether the player is online; this is broadcast as offline to remove remote players from local caches
	protected final boolean mIsOnline;
	// Whether the player is visible to most players or not
	protected final boolean mIsHidden;
	// Data provided by other plugins
	protected final Map<String, JsonObject> mPluginData;

	// Data from this (local) server
	protected RemotePlayerAbstraction(String serverId, UUID uuid, String name, boolean isOnline, boolean isHidden) {
		mServerId = serverId;
		mUuid = uuid;
		mName = name;
		mIsOnline = isOnline;
		mIsHidden = isHidden;
		mPluginData = new HashMap<>();
	}

	// Data from a remote server of any type (subclasses provide extra information)
	public RemotePlayerAbstraction(JsonObject remoteData) {
		mServerId = remoteData.get("serverId").getAsString();
		mUuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
		mName = remoteData.get("playerName").getAsString();
		mIsOnline = remoteData.get("isOnline").getAsBoolean();
		mIsHidden = remoteData.get("isHidden").getAsBoolean();
		mPluginData = new HashMap<>();
		JsonObject pluginData = remoteData.getAsJsonObject("pluginData");
		for (String key : pluginData.keySet()) {
			mPluginData.put(key, pluginData.getAsJsonObject(key));
		}
	}

	// Determine the appropriate remote player data type to use
	public static RemotePlayerAbstraction from(JsonObject remoteData) {
		String serverType = remoteData.get("serverType").getAsString();
		switch (serverType) {
			case RemotePlayerPaper.SERVER_TYPE:
				return new RemotePlayerPaper(remoteData);
			case RemotePlayerBungee.SERVER_TYPE:
				return new RemotePlayerBungee(remoteData);
			default:
				return new RemotePlayerGeneric(remoteData);
		}
	}

	// Serializes player data to be broadcast to remote servers
	public JsonObject toJson() {
		JsonObject playerData = new JsonObject();
		playerData.addProperty("serverType", getServerType());
		playerData.addProperty("serverId", mServerId);
		playerData.addProperty("playerUuid", mUuid.toString());
		playerData.addProperty("playerName", mName);
		playerData.addProperty("isHidden", mIsHidden);
		playerData.addProperty("isOnline", mIsOnline);
		playerData.add("pluginData", serializePluginData());
		return playerData;
	}

	// Get a given plugin's data, if available
	@Nullable
	public JsonObject getPluginData(String pluginId) {
		return mPluginData.get(pluginId);
	}

	protected JsonObject serializePluginData() {
		JsonObject pluginData = new JsonObject();
		for (Map.Entry<String, JsonObject> entry : mPluginData.entrySet()) {
			pluginData.add(entry.getKey(), entry.getValue());
		}
		return pluginData;
	}

	public abstract String getServerType();

	public String getServerId() {
		return mServerId;
	}

	public UUID getUuid() {
		return mUuid;
	}

	public String getName() {
		return mName;
	}

	protected void broadcast() {
		JsonObject playerData = toJson();

		try {
			NetworkRelayAPI.sendExpiringBroadcastMessage(RemotePlayerManagerAbstraction.REMOTE_PLAYER_UPDATE_CHANNEL,
				playerData,
				RemotePlayerManagerAbstraction.REMOTE_PLAYER_MESSAGE_TTL);
		} catch (Exception e) {
			MMLog.severe("Failed to broadcast " + RemotePlayerManagerAbstraction.REMOTE_PLAYER_UPDATE_CHANNEL);
		}
	}

	@Override
	public String toString() {
		return toJson().toString();
	}
}
