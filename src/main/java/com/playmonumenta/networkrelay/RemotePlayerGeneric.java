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

	@Override
	protected Map<String, JsonObject> gatherPluginData() {
		return new HashMap<>();
	}

	public static RemotePlayerGeneric from(JsonObject remoteData) {
		UUID uuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
		String name = remoteData.get("playerName").getAsString();
		return new RemotePlayerGeneric(uuid, name, remoteData);
	}

	@Override
	public String getServerType() {
		return "generic";
	}
}
