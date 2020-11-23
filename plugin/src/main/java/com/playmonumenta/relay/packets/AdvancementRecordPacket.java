package com.playmonumenta.relay.packets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.relay.ServerProperties;
import com.playmonumenta.relay.AdvancementManager;
import com.playmonumenta.relay.AdvancementRecord;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class AdvancementRecordPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Broadcast.AdvancementRecord";

	public AdvancementRecordPacket(AdvancementRecord record) {
		/* TODO: Some kind of timeout */
		super("*", PacketOperation);
		getData().add("record", record.toJson());
	}

	public static void handlePacket(Plugin plugin, JsonObject data) throws Exception {
		if (!data.has("record") ||
		    !data.get("record").isJsonObject()) {
			throw new Exception("AdvancementRecordPacket failed to parse required string field 'record'");
		}
		AdvancementRecord record = new AdvancementRecord(data.get("record").getAsJsonObject());

		AdvancementManager.getInstance().addRemoteRecord(record);
	}
}
