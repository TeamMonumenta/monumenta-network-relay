package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RemotePlayerBungee extends RemotePlayerAbstraction {
	protected static final String SERVER_TYPE = "proxy";

	protected RemotePlayerBungee(UUID uuid, String name, boolean isOnline, String shard, String proxy) {
		super(uuid, name);
		super.mIsOnline = isOnline;
		super.mShard = shard;
		super.mProxy = proxy;

		MMLog.fine("Created RemotePlayerBungee for " + mName + " from " + mProxy + ": " + (mIsOnline ? "online" : "offline"));
	}

	protected RemotePlayerBungee(UUID uuid, String name, boolean isOnline, String shard, String proxy, JsonObject remoteData) {
		super(uuid, name, remoteData);
		mProxy = proxy;
		mShard = shard;

		MMLog.fine("Received RemotePlayerBungee for " + mName + " from " + mProxy + ": " + (mIsOnline ? "online" : "offline"));
	}

	@Override
	protected Map<String, JsonObject> gatherPluginData() {
		return new HashMap<>();
	}

	protected static RemotePlayerBungee bungeeFrom(JsonObject remoteData) {
		UUID uuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
		String name = remoteData.get("playerName").getAsString();
		boolean isOnline = remoteData.get("isOnline").getAsBoolean();
		String proxy = remoteData.get("proxy").getAsString();
		String shard = remoteData.get("shard").getAsString();
		return new RemotePlayerBungee(uuid, name, isOnline, shard, proxy, remoteData);
	}

	@Override
	public String getServerType() {
		return SERVER_TYPE;
	}

	public String getProxy() {
		return mProxy;
	}

	public String getShard() {
		return mShard;
	}
}
