package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

public class NetworkMessageListener implements Listener {
	private final @Nullable String mServerAddress;

	public NetworkMessageListener(@Nullable String serverAddress) {
		mServerAddress = serverAddress;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void gatherHeartbeatData(GatherHeartbeatDataEvent event) {
		JsonObject data = new JsonObject();
		if (mServerAddress != null && !mServerAddress.isEmpty()) {
			data.addProperty("server-address", mServerAddress);
		}
		data.addProperty("server-type", "minecraft");
		event.setPluginData(NetworkRelayAPI.NETWORK_RELAY_HEARTBEAT_IDENTIFIER, data);
	}
}
