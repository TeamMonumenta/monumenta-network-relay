package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.Map;
import java.util.UUID;

public class RemotePlayerPaper extends RemotePlayerAbstraction {
	protected static final String SERVER_TYPE = "minecraft";

	// The world the player is on for this Minecraft server
	protected final String mWorld;

	protected RemotePlayerPaper(String serverId, UUID uuid, String name, boolean isHidden, boolean isOnline, String world) {
		super(serverId, uuid, name, isOnline, isHidden);
		mWorld = world;

		MMLog.fine("Created RemotePlayerPaper for " + mName + " from " + mServerId + ": " + (mIsOnline ? "online" : "offline"));
	}

	protected RemotePlayerPaper(JsonObject remoteData) {
		super(remoteData);
		mWorld = remoteData.get("world").getAsString();

		MMLog.fine("Received RemotePlayerPaper for " + mName + " from " + mServerId + ": " + (mIsOnline ? "online" : "offline"));
	}

	public JsonObject toJson() {
		JsonObject playerData = super.toJson();
		playerData.addProperty("world", mWorld);
		return playerData;
	}

	@Override
	public String getServerType() {
		return SERVER_TYPE;
	}

	public String world() {
		return mWorld;
	}
}
