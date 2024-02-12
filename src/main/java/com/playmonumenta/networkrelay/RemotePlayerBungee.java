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
	protected void broadcast() {
		JsonObject remotePlayerData = new JsonObject();
		remotePlayerData.addProperty("serverType", getServerType());
		remotePlayerData.addProperty("playerUuid", mUuid.toString());
		remotePlayerData.addProperty("playerName", mName);
		// remotePlayerData.addProperty("isHidden", mIsHidden);
		remotePlayerData.addProperty("isOnline", mIsOnline);
		remotePlayerData.addProperty("proxy", mProxy);
		// remotePlayerData.addProperty("shard", mShard);
		// remotePlayerData.addProperty("world", mWorld);
		remotePlayerData.add("plugins", serializePluginData());

		try {
			NetworkRelayAPI.sendExpiringBroadcastMessage(RemotePlayerManagerAbstraction.REMOTE_PLAYER_UPDATE_CHANNEL,
				remotePlayerData,
				RemotePlayerManagerAbstraction.REMOTE_PLAYER_MESSAGE_TTL);
		} catch (Exception e) {
			MMLog.severe("Failed to broadcast " + RemotePlayerManagerAbstraction.REMOTE_PLAYER_UPDATE_CHANNEL);
		}
	}

	@Override
	protected void update(RemotePlayerAbstraction remote) {
		if (remote == null) {
			return;
		}
		mIsHidden = remote.mIsHidden;
		if (remote.mShard != null) {
			mShard = remote.mShard;
		}
		if (remote.mWorld != null) {
			mWorld = remote.mWorld;
		}
	}

	@Override
	public String getServerType() {
		return SERVER_TYPE;
	}
}
