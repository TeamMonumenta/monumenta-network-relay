package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RemotePlayerGeneric extends RemotePlayerAbstraction {
	protected final String mServerType;

	protected RemotePlayerGeneric(UUID uuid, String name, String serverType) {
		super(uuid, name);
		mServerType = serverType;

		MMLog.fine("Created RemotePlayerGeneric for " + mName);
	}

	protected RemotePlayerGeneric(UUID uuid, String name, String serverType, JsonObject remoteData) {
		super(uuid, name, remoteData);
		mServerType = serverType;

		MMLog.fine("Created RemotePlayerGeneric for " + mName);
	}

	@Override
	protected Map<String, JsonObject> gatherPluginData() {
		return new HashMap<>();
	}

	protected static RemotePlayerGeneric genericFrom(String serverType, JsonObject remoteData) {
		UUID uuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
		String name = remoteData.get("playerName").getAsString();
		return new RemotePlayerGeneric(uuid, name, serverType, remoteData);
	}

	@Override
	public String getServerType() {
		return mServerType;
	}
}
