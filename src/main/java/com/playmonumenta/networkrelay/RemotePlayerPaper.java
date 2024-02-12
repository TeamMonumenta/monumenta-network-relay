package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.Map;
import java.util.UUID;

public class RemotePlayerPaper extends RemotePlayerAbstraction {
	protected static final String SERVER_TYPE = "minecraft";

	protected RemotePlayerPaper(UUID uuid, String name, boolean isHidden, boolean isOnline, String shard, String world) {
		super(uuid, name);
		mIsOnline = isOnline;
		mIsHidden = isHidden;
		mShard = shard;
		mWorld = world;

		MMLog.fine("Created RemotePlayerState for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
	}

	protected RemotePlayerPaper(UUID uuid, String name, boolean isHidden, boolean isOnline, String shard, String world, JsonObject remoteData) {
		super(uuid, name, remoteData);
		mIsOnline = isOnline;
		mIsHidden = isHidden;
		mShard = shard;
		mWorld = world;

		MMLog.fine("Received RemotePlayerState for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
	}

	public static RemotePlayerPaper from(JsonObject remoteData) {
		UUID uuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
		String name = remoteData.get("playerName").getAsString();
		boolean isOnline = remoteData.get("isOnline").getAsBoolean();
		boolean isHidden = remoteData.get("isHidden").getAsBoolean();
		String shard = remoteData.get("shard").getAsString();
		String world = remoteData.get("world").getAsString();
		return new RemotePlayerPaper(uuid, name, isHidden, isOnline, shard, world, remoteData);
	}

	@Override
	protected void broadcast() {
		JsonObject remotePlayerData = new JsonObject();
		remotePlayerData.addProperty("serverType", getServerType());
		remotePlayerData.addProperty("playerUuid", mUuid.toString());
		remotePlayerData.addProperty("playerName", mName);
		remotePlayerData.addProperty("isHidden", mIsHidden);
		remotePlayerData.addProperty("isOnline", mIsOnline);
		// remotePlayerData.addProperty("proxy", mProxy);
		remotePlayerData.addProperty("shard", mShard);
		remotePlayerData.addProperty("world", mWorld);
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
	public String getServerType() {
		return SERVER_TYPE;
	}
}
