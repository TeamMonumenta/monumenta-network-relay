package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RemotePlayerGeneric extends RemotePlayerAbstraction {

	protected RemotePlayerGeneric(UUID uuid, String name) {
		super(uuid, name);

		MMLog.fine("Created RemotePlayerGeneric for " + mName);
	}

	protected RemotePlayerGeneric(UUID uuid, String name, JsonObject remoteData) {
		super(uuid, name, remoteData);

		MMLog.fine("Created RemotePlayerGeneric for " + mName);
	}

	public static RemotePlayerGeneric from(JsonObject remoteData) {
		UUID uuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
		String name = remoteData.get("playerName").getAsString();
		return new RemotePlayerGeneric(uuid, name, remoteData);
	}

	@Override
	protected void broadcast() {
		JsonObject remotePlayerData = new JsonObject();
		remotePlayerData.addProperty("serverType", getServerType());
		remotePlayerData.addProperty("playerUuid", mUuid.toString());
		remotePlayerData.addProperty("playerName", mName);
		// remotePlayerData.addProperty("isHidden", mIsHidden);
		remotePlayerData.addProperty("isOnline", mIsOnline);
		// remotePlayerData.addProperty("proxy", mProxy);
		// remotePlayerData.addProperty("shard", mShard);
		// remotePlayerData.addProperty("world", mWorld);
		remotePlayerData.add("plugins", serializePluginData());

		try {
			NetworkRelayAPI.sendExpiringBroadcastMessage(RemotePlayerManagerPaper.REMOTE_PLAYER_UPDATE_CHANNEL,
				remotePlayerData,
				RemotePlayerManagerPaper.REMOTE_PLAYER_MESSAGE_TTL);
		} catch (Exception e) {
			MMLog.severe("Failed to broadcast " + RemotePlayerManagerPaper.REMOTE_PLAYER_UPDATE_CHANNEL);
		}
	}

	protected void update(RemotePlayerAbstraction remote) {
		if (remote == null) {
			return;
		}
	}

	@Override
	public String getServerType() {
		return "generic";
	}
}
