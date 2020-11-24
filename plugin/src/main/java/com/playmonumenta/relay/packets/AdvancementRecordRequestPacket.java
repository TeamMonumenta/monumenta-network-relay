package com.playmonumenta.relay.packets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.relay.ServerProperties;
import com.playmonumenta.relay.AdvancementManager;
import com.playmonumenta.relay.AdvancementRecord;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class AdvancementRecordRequestPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Broadcast.AdvancementRecordRequest";

	public AdvancementRecordRequestPacket() {
		/* TODO: Some kind of timeout */
		super("*", PacketOperation);
	}

	public static void handlePacket(Plugin plugin, JsonObject data) throws Exception {
		AdvancementManager.getInstance().broadcastAllAdvancementRecords();
	}
}
