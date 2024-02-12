package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RemotePlayerBungee extends RemotePlayerAbstraction {
	protected static final String SERVER_TYPE = "proxy";

	protected RemotePlayerBungee(UUID uuid, String name, boolean isOnline, String proxy) {
		super(uuid, name);
		mIsOnline = isOnline;
		mProxy = proxy;

		MMLog.fine("Created RemotePlayerBungee for " + mName + " from " + mProxy + ": " + (mIsOnline ? "online" : "offline"));
	}

	protected RemotePlayerBungee(UUID uuid, String name, boolean isOnline, String proxy, JsonObject remoteData) {
		super(uuid, name, remoteData);
		mIsOnline = isOnline;
		mProxy = proxy;

		MMLog.fine("Received RemotePlayerBungee for " + mName + " from " + mProxy + ": " + (mIsOnline ? "online" : "offline"));
	}

	public static RemotePlayerBungee from(JsonObject remoteData) {
		UUID uuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
		String name = remoteData.get("playerName").getAsString();
		boolean isOnline = remoteData.get("isOnline").getAsBoolean();
		String proxy = remoteData.get("proxy").getAsString();
		return new RemotePlayerBungee(uuid, name, isOnline, proxy, remoteData);
	}

	@Override
	public String getServerType() {
		return SERVER_TYPE;
	}
}
