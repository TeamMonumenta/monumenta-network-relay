package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.UUID;

public class RemotePlayerBungee extends RemotePlayerAbstraction {
	protected static final String SERVER_TYPE = "proxy";

	// The shard the proxy wishes the player to be on
	protected final String mTargetShard;

	protected RemotePlayerBungee(String serverId, UUID uuid, String name, boolean isOnline, Boolean isHidden, String targetShard) {
		super(serverId, uuid, name, isOnline, isHidden);
		mTargetShard = targetShard;

		MMLog.fine("Created RemotePlayerBungee for " + mName + " from " + mServerId + ": " + (mIsOnline ? "online" : "offline"));
	}

	protected RemotePlayerBungee(JsonObject remoteData) {
		super(remoteData);
		mTargetShard = remoteData.get("targetShard").getAsString();

		MMLog.fine("Received RemotePlayerBungee for " + mName + " from " + mServerId + ": " + (mIsOnline ? "online" : "offline"));
	}

	public JsonObject toJson() {
		JsonObject playerData = super.toJson();
		playerData.addProperty("targetShard", mTargetShard);
		return playerData;
	}

	@Override
	public String getServerType() {
		return SERVER_TYPE;
	}

	public String targetShard() {
		return mTargetShard;
	}
}
