package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RemotePlayerGeneric extends RemotePlayerAbstraction {
	protected final String mServerType;
	protected final String mShard;

	protected RemotePlayerGeneric(UUID uuid, String name, boolean isOnline, String serverType, String shard) {
		super(uuid, name, isOnline);
		mServerType = serverType;
		mShard = shard;

		MMLog.fine("Created RemotePlayerBungee for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
	}

	protected RemotePlayerGeneric(UUID uuid, String name, boolean isOnline, String serverType, String shard, JsonObject remoteData) {
		super(uuid, name, isOnline, remoteData);
		mServerType = serverType;
		mShard = shard;

		MMLog.fine("Received RemotePlayerBungee for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
	}

	@Override
	protected Map<String, JsonObject> gatherPluginData() {
		return new HashMap<>();
	}

	protected static RemotePlayerGeneric genericFrom(JsonObject remoteData) {
		UUID uuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
		String name = remoteData.get("playerName").getAsString();
		boolean isOnline = remoteData.get("isOnline").getAsBoolean();
		String shard = remoteData.get("shard").getAsString();
		return new RemotePlayerGeneric(uuid, name, isOnline, shard, remoteData);
	}

	@Override
	public String getServerType() {
		return mServerType;
	}

	public String getShard() {
		return mShard;
	}
}
