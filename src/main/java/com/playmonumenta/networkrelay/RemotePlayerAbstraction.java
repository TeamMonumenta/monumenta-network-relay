package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.Nullable;

public abstract class RemotePlayerAbstraction {
	protected final UUID mUuid;
	protected final String mName;
	protected boolean mIsOnline = true;
	protected boolean mIsHidden = false;
	/*
	 * What proxy the player is on:
	 * This should never be null unless it is a fakeplayer from RemotePlayerGeneric
	 */
	@Nullable protected String mProxy = null;
	/*
	 * What shard the player is on
	 * This can be null since the player could still be in the server selection phase when they first connect
	 */
	@Nullable protected String mShard = null;
	/*
	 * What world the player is on
	 * This can be null since the player could still be in the server selection phase when they first connect
	 */
	@Nullable protected String mWorld = null;
	private final ConcurrentMap<String, JsonObject> mPluginData;

	protected RemotePlayerAbstraction(UUID uuid, String name) {
		mUuid = uuid;
		mName = name;

		mPluginData = new ConcurrentHashMap<>();
	}

	protected RemotePlayerAbstraction(UUID uuid, String name, JsonObject remoteData) {
		mUuid = uuid;
		mName = name;

		mPluginData = deserializePluginData(remoteData);
	}

	public static RemotePlayerAbstraction from(JsonObject remoteData) {
		String serverType = remoteData.get("serverType").getAsString();
		switch (serverType) {
			case RemotePlayerPaper.SERVER_TYPE:
				return RemotePlayerPaper.from(remoteData);
			case RemotePlayerBungee.SERVER_TYPE:
				return RemotePlayerBungee.from(remoteData);
			default:
				return RemotePlayerGeneric.from(remoteData);
		}
	}

	@Nullable
	public JsonObject getPluginData(String pluginId) {
		return mPluginData.get(pluginId);
	}

	public void setPluginData(Map<String, JsonObject> data) {
		mPluginData.clear();
		mPluginData.putAll(data);
	}

	protected ConcurrentMap<String, JsonObject> deserializePluginData(JsonObject remoteData) {
		ConcurrentMap<String, JsonObject> pluginDataMap = new ConcurrentHashMap<>();
		// "plugins": {"plugin-name": {}}
		JsonObject pluginData = remoteData.getAsJsonObject("plugins");
		for (String key : pluginData.keySet()) {
			pluginDataMap.put(key, pluginData.getAsJsonObject(key));
		}
		return pluginDataMap;
	}

	protected JsonObject serializePluginData() {
		JsonObject pluginData = new JsonObject();
		for (Map.Entry<String, JsonObject> entry : mPluginData.entrySet()) {
			pluginData.add(entry.getKey(), entry.getValue());
		}
		return pluginData;
	}

	public abstract String getServerType();

	public UUID getUuid() {
		return mUuid;
	}

	public String getName() {
		return mName;
	}

	protected void broadcast() {
		JsonObject remotePlayerData = new JsonObject();
		remotePlayerData.addProperty("serverType", getServerType());
		remotePlayerData.addProperty("playerUuid", mUuid.toString());
		remotePlayerData.addProperty("playerName", mName);
		remotePlayerData.addProperty("isOnline", mIsOnline);
		remotePlayerData.add("plugins", serializePluginData());

		try {
			NetworkRelayAPI.sendExpiringBroadcastMessage(RemotePlayerManagerAbstraction.REMOTE_PLAYER_UPDATE_CHANNEL,
				remotePlayerData,
				RemotePlayerManagerAbstraction.REMOTE_PLAYER_MESSAGE_TTL);
		} catch (Exception e) {
			MMLog.severe("Failed to broadcast " + RemotePlayerManagerAbstraction.REMOTE_PLAYER_UPDATE_CHANNEL);
		}
	}

	protected void update(RemotePlayerAbstraction player) {
		if (player == null) {
			return;
		}
		if (player.mProxy != null && mProxy == null) {
			mProxy = player.mProxy;
		}
		if (player.mShard != null && mShard == null) {
			mShard = player.mShard;
		}
		if (player.mWorld != null && mWorld == null) {
			mWorld = player.mWorld;
		}
	}
}
