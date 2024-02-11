package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
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
		mPluginData.putAll(gatherPluginData());
	}

	protected RemotePlayerAbstraction(UUID uuid, String name, JsonObject remoteData) {
		mUuid = uuid;
		mName = name;

		mPluginData = deserializePluginData(remoteData);
	}

	protected abstract Map<String, JsonObject> gatherPluginData();

	public static RemotePlayerAbstraction from(JsonObject remoteData) {
		String serverType = remoteData.get("serverType").getAsString();
		switch (serverType) {
			case RemotePlayerPaper.SERVER_TYPE:
				return RemotePlayerPaper.paperFrom(remoteData);
			case RemotePlayerBungee.SERVER_TYPE:
				return RemotePlayerBungee.bungeeFrom(remoteData);
			default:
				return RemotePlayerGeneric.genericFrom(serverType, remoteData);
		}
	}

	@Nullable
	public JsonObject getPluginData(String pluginId) {
		return mPluginData.get(pluginId);
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
}
