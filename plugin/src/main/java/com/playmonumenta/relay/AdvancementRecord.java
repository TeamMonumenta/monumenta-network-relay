package com.playmonumenta.relay;

import java.time.Instant;

import com.playmonumenta.relay.utils.DataPackUtils;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class AdvancementRecord {
	String mAdvancement = null;
	Instant mInstant = null;
	String mPlayer = null;
	String mTeam = null;

	public AdvancementRecord(Player player, Advancement advancement) {
		mAdvancement = advancement.getKey().toString();
		mInstant = DataPackUtils.getEarnedInstant(player, advancement);
		mPlayer = player.getName();

		if (DataPackUtils.getTeam(player) != null) {
			mTeam = DataPackUtils.getTeam(player).getName();
		} else {
			mTeam = "NoTeam";
		}
	}

	public AdvancementRecord(JsonObject record) throws Exception {
		if (record == null) {
			throw new Exception("record was null");
		}

		JsonPrimitive advancementPrimitive = record.getAsJsonPrimitive("advancement");
		JsonPrimitive instantPrimitive = record.getAsJsonPrimitive("instant");
		JsonPrimitive playerPrimitive = record.getAsJsonPrimitive("player");
		JsonPrimitive teamPrimitive = record.getAsJsonPrimitive("team");

		try {
			mAdvancement = advancementPrimitive.getAsString();

			Long instantMs = instantPrimitive.getAsLong();
			mInstant = Instant.ofEpochMilli(instantMs);

			mPlayer = playerPrimitive.getAsString();
			mTeam = teamPrimitive.getAsString();
		} catch (Exception e) {
			throw new Exception("json is not in record format");
		}

		if (mAdvancement == null ||
		    mInstant == null ||
		    mPlayer == null ||
		    mTeam == null) {
			throw new Exception("json is not in record format");
		}
	}

	public JsonObject toJson() {
		JsonObject record = new JsonObject();

		record.addProperty("advancement", mAdvancement);
		record.addProperty("instant", mInstant.toEpochMilli());
		record.addProperty("player", mPlayer);
		record.addProperty("team", mTeam);

		return record;
	}
}
