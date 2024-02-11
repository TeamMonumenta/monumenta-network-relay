package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;

public class RemotePlayerPaper extends RemotePlayerAbstraction {
	protected static final String SERVER_TYPE = "minecraft";

	protected RemotePlayerPaper(UUID uuid, String name, boolean isHidden, boolean isOnline, String shard, String world) {
		super(uuid, name);
		super.mIsOnline = isOnline;
		super.mIsHidden = isHidden;
		super.mShard = shard;
		super.mWorld = world;

		MMLog.fine("Created RemotePlayerState for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
	}

	protected RemotePlayerPaper(UUID uuid, String name, boolean isHidden, boolean isOnline, String shard, String world, JsonObject remoteData) {
		super(uuid, name, remoteData);
		super.mIsOnline = isOnline;
		super.mIsHidden = isHidden;
		super.mShard = shard;
		super.mWorld = world;

		MMLog.fine("Received RemotePlayerState for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
	}

	@Override
	protected Map<String, JsonObject> gatherPluginData() {
		GatherRemotePlayerDataEvent event = new GatherRemotePlayerDataEvent();
		Bukkit.getPluginManager().callEvent(event);
		return event.getPluginData();
	}

	protected static RemotePlayerPaper paperFrom(JsonObject remoteData) {
		UUID uuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
		String name = remoteData.get("playerName").getAsString();
		boolean isOnline = remoteData.get("isOnline").getAsBoolean();
		boolean isHidden = remoteData.get("isHidden").getAsBoolean();
		String shard = remoteData.get("shard").getAsString();
		String world = remoteData.get("world").getAsString();
		return new RemotePlayerPaper(uuid, name, isHidden, isOnline, shard, world, remoteData);
	}

	protected void broadcast() {
		JsonObject remotePlayerData = new JsonObject();
		remotePlayerData.addProperty("serverType", SERVER_TYPE);
		remotePlayerData.addProperty("playerUuid", mUuid.toString());
		remotePlayerData.addProperty("playerName", mName);
		remotePlayerData.addProperty("isHidden", mIsHidden);
		remotePlayerData.addProperty("isOnline", mIsOnline);
		remotePlayerData.addProperty("shard", mShard);
		remotePlayerData.addProperty("world", mWorld);
		remotePlayerData.add("plugins", serializePluginData());

		try {
			NetworkRelayAPI.sendExpiringBroadcastMessage(RemotePlayerManagerPaper.REMOTE_PLAYER_UPDATE_CHANNEL,
				remotePlayerData,
				RemotePlayerManagerPaper.REMOTE_PLAYER_MESSAGE_TTL);
		} catch (Exception e) {
			MMLog.severe("Failed to broadcast " + RemotePlayerManagerPaper.REMOTE_PLAYER_UPDATE_CHANNEL);
		}
	}

	@Override
	public String getServerType() {
		return SERVER_TYPE;
	}

	public String getShard() {
		return mShard;
	}

	public boolean isHidden() {
		return mIsHidden;
	}
}
