package com.playmonumenta.relay.packets;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.relay.ServerProperties;
import com.playmonumenta.relay.AdvancementManager;
import com.playmonumenta.relay.AdvancementRecord;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class AdvancementRecordPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Broadcast.AdvancementRecord";

	public AdvancementRecordPacket(String advancementId, AdvancementRecord record) {
		/* TODO: Some kind of timeout */
		super("*", PacketOperation);
		getData().addProperty("advancementId", advancementId);
		getData().add("record", record.toJson());
	}

	public static void handlePacket(Plugin plugin, JsonObject data) throws Exception {
		JsonPrimitive advancementIdJson = data.getAsJsonPrimitive("advancementId");
		if (advancementIdJson == null ||
		    !advancementIdJson.isString()) {
			throw new Exception("AdvancementRecordPacket failed to parse required string field 'advancementId'");
		}
		String advancementId = advancementIdJson.getAsString();
		if (advancementId == null ||
		    advancementId.isEmpty()) {
			throw new Exception("AdvancementRecordPacket failed to parse required string field 'advancementId'");
		}

		JsonObject recordJson = data.getAsJsonObject("record");
		if (recordJson == null) {
			throw new Exception("AdvancementRecordPacket failed to parse required object field 'record'");
		}

		AdvancementRecord record = new AdvancementRecord(recordJson);
		AdvancementManager.getInstance().addRemoteRecord(advancementId, record);
	}
}
