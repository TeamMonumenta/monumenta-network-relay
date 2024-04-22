package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class RemotePlayerBungee extends RemotePlayerAbstraction {
	protected static final String SERVER_TYPE = "proxy";

	/**
	 * The shard the proxy wishes the player to be on
	 * This will be blank if the target shard could not be determined
	 */
	protected final String mTargetShard;

	protected RemotePlayerBungee(String serverId, UUID uuid, String name, boolean isOnline, @Nullable Boolean isHidden, String targetShard) {
		super(serverId, uuid, name, isOnline, isHidden);
		mTargetShard = targetShard;

		MMLog.fine(() -> "Created RemotePlayerBungee for " + mName + " from " + mServerId + ": " + (mIsOnline ? "online" : "offline"));
	}

	protected RemotePlayerBungee(JsonObject remoteData) {
		super(remoteData);
		mTargetShard = remoteData.get("targetShard").getAsString();

		MMLog.fine(() -> "Received RemotePlayerBungee for " + mName + " from " + mServerId + ": " + (mIsOnline ? "online" : "offline"));
	}

	@Override
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

	@Override
	public boolean isSimilar(RemotePlayerAbstraction other) {
		if (!super.isSimilar(other)) {
			return false;
		}
		if (other instanceof RemotePlayerBungee otherB) {
			return this.mTargetShard.equals(otherB.mTargetShard);
		}
		return true;
	}
}
