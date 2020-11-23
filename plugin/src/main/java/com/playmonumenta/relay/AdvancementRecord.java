package com.playmonumenta.relay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.playmonumenta.relay.utils.DataPackUtils;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class AdvancementRecord {
	private String mAdvancement = null;
	private Instant mInstant = null;
	private Map<String, String> mFirstPlayerTeams = new HashMap<String, String>();
	private Map<String, String> mLaterPlayerTeams = new HashMap<String, String>();

	public AdvancementRecord(AdvancementRecord toClone) {
		mAdvancement = toClone.getAdvancement();
		mInstant = toClone.getInstant();
		mFirstPlayerTeams = toClone.getFirstPlayerTeams();
		mLaterPlayerTeams = toClone.getLaterPlayerTeams();
	}

	public AdvancementRecord(Player player, Advancement advancement) {
		mAdvancement = advancement.getKey().toString();
		mInstant = DataPackUtils.getEarnedInstant(player, advancement);

		String playerName = player.getName();
		String playerTeam = "NoTeam";
		if (DataPackUtils.getTeam(player) != null) {
			playerTeam = DataPackUtils.getTeam(player).getName();
		}
		mFirstPlayerTeams.put(playerName, playerTeam);
	}

	public AdvancementRecord(JsonObject record) throws Exception {
		if (record == null) {
			throw new Exception("record was null");
		}

		JsonPrimitive advancementPrimitive = record.getAsJsonPrimitive("advancement");
		JsonPrimitive instantPrimitive = record.getAsJsonPrimitive("instant");
		JsonObject firstPlayerTeamsObject = record.getAsJsonObject("first_player_teams");
		JsonObject laterPlayerTeamsObject = record.getAsJsonObject("later_player_teams");

		try {
			mAdvancement = advancementPrimitive.getAsString();

			Long instantMs = instantPrimitive.getAsLong();
			mInstant = Instant.ofEpochMilli(instantMs);

			String playerName;
			JsonElement teamElement;
			String team;

			for (Map.Entry<String, JsonElement> entry : firstPlayerTeamsObject.entrySet()) {
				playerName = entry.getKey();
				teamElement = entry.getValue();
				team = teamElement.getAsString();
				mFirstPlayerTeams.put(playerName, team);
			}

			for (Map.Entry<String, JsonElement> entry : laterPlayerTeamsObject.entrySet()) {
				playerName = entry.getKey();
				teamElement = entry.getValue();
				team = teamElement.getAsString();
				mLaterPlayerTeams.put(playerName, team);
			}
		} catch (Exception e) {
			throw new Exception("json is not in record format");
		}

		if (mAdvancement == null ||
		    mInstant == null ||
		    mFirstPlayerTeams.isEmpty()) {
			throw new Exception("json is not in record format");
		}
	}

	public String getAdvancement() {
		return mAdvancement;
	}

	public Instant getInstant() {
		return Instant.ofEpochMilli(mInstant.toEpochMilli());
	}

	public Map<String, String> getFirstPlayerTeams() {
		return new HashMap<String, String>(mFirstPlayerTeams);
	}

	public Map<String, String> getLaterPlayerTeams() {
		return new HashMap<String, String>(mLaterPlayerTeams);
	}

	public Set<Map.Entry<String, String>> getNewlyFirstPlayerTeams(AdvancementRecord newRecord) {
		if (newRecord == null) {
			return new HashMap<String, String>().entrySet();
		}
		Set<Map.Entry<String, String>> newlyFirstPlayers;

		int oldComparedToNewRecord = mInstant.compareTo(newRecord.getInstant());
		if (oldComparedToNewRecord < 0) {
			// Old record was faster, no newly faster records
			return new HashMap<String, String>().entrySet();
		}

		// New record was at least as fast, so all new first records are at least as fast...
		newlyFirstPlayers = newRecord.getFirstPlayerTeams().entrySet();

		// ...and existing ones aren't "newly faster"
		newlyFirstPlayers.removeAll(getFirstPlayerTeams().entrySet());

		return newlyFirstPlayers;
	}

	public Set<Map.Entry<String, String>> getNewlyLaterPlayerTeams(AdvancementRecord newRecord) {
		if (newRecord == null) {
			return new HashMap<String, String>().entrySet();
		}
		Set<Map.Entry<String, String>> newlyLaterPlayers;

		int oldComparedToNewRecord = mInstant.compareTo(newRecord.getInstant());
		if (oldComparedToNewRecord < 0) {
			// Current was faster, so all the entries from the new record are later
			newlyLaterPlayers = newRecord.getFirstPlayerTeams().entrySet();
			newlyLaterPlayers.addAll(newRecord.getLaterPlayerTeams().entrySet());

			// But, anything current is already accounted for, not "newly later"
			newlyLaterPlayers.removeAll(getFirstPlayerTeams().entrySet());
			newlyLaterPlayers.removeAll(getLaterPlayerTeams().entrySet());
		} else if (oldComparedToNewRecord == 0) {
			// Same time! Any new "later" entries are newly later...
			newlyLaterPlayers = newRecord.getLaterPlayerTeams().entrySet();

			// ...unless they're accounted for currently.
			newlyLaterPlayers.removeAll(getLaterPlayerTeams().entrySet());
		} else {
			// New record was faster. Current first entries fall under "corrected later", not here.
			// Instead, count any later entries from the new record...
			newlyLaterPlayers = newRecord.getLaterPlayerTeams().entrySet();
			
			// ...and ignore the ones accounted for currently.
			newlyLaterPlayers.removeAll(getLaterPlayerTeams().entrySet());

			// This is identical to the last case, but duplicated with new comments for clarity.
		}

		return newlyLaterPlayers;
	}

	public Set<Map.Entry<String, String>> getCorrectedLaterPlayerTeams(AdvancementRecord newRecord) {
		if (newRecord == null) {
			return new HashMap<String, String>().entrySet();
		}
		Set<Map.Entry<String, String>> correctedLaterPlayers;

		int oldComparedToNewRecord = mInstant.compareTo(newRecord.getInstant());
		if (oldComparedToNewRecord < 0) {
			// Current was faster, so nothing to do.
			return new HashMap<String, String>().entrySet();
		} else if (oldComparedToNewRecord == 0) {
			// Same time! Return new entries from the new record that is also a later entry of the current record.
			Set<Map.Entry<String, String>> currentLaterEntries = getLaterPlayerTeams().entrySet();
			correctedLaterPlayers = new HashMap<String, String>().entrySet();

			for (Map.Entry<String, String> entry : newRecord.getFirstPlayerTeams().entrySet()) {
				if (currentLaterEntries.contains(entry)) {
					correctedLaterPlayers.add(entry);
				}
			}
		} else {
			// New record was faster. Current first entries are "corrected later"...
			correctedLaterPlayers = getFirstPlayerTeams().entrySet();
			
			// ...assuming they're not also fastest on the new record.
			correctedLaterPlayers.removeAll(newRecord.getFirstPlayerTeams().entrySet());
		}

		return correctedLaterPlayers;
	}

	public AdvancementRecord cloneAndUpdate(AdvancementRecord newRecord) {
		AdvancementRecord updatedClone = new AdvancementRecord(this);

		if (newRecord == null) {
			return updatedClone;
		}

		int oldComparedToNewRecord = mInstant.compareTo(newRecord.getInstant());
		if (oldComparedToNewRecord < 0) {
			// Current was faster.
			// Update current "later" to include new "first" and "later"
			updatedClone.mLaterPlayerTeams.putAll(newRecord.getFirstPlayerTeams());
		} else if (oldComparedToNewRecord == 0) {
			// Same time! Add all "first" entries from new record...
			updatedClone.mFirstPlayerTeams.putAll(newRecord.getFirstPlayerTeams());

			// ...add all "later" entries from new record...
			updatedClone.mLaterPlayerTeams.putAll(newRecord.getLaterPlayerTeams());
			// ...and remove any "later" entries that are "first" in the new record.
			for (String player : newRecord.getFirstPlayerTeams().keySet()) {
				updatedClone.mLaterPlayerTeams.remove(player);
			}
		} else {
			// New record was faster. Update the instant...
			mInstant = newRecord.getInstant();
			// ...copy current "first" and new "later" to "later"...
			mLaterPlayerTeams.putAll(mFirstPlayerTeams);
			mLaterPlayerTeams.putAll(newRecord.getLaterPlayerTeams());
			// ...and replace updated "first" with new "first"
			mFirstPlayerTeams = newRecord.getFirstPlayerTeams();
		}

		return updatedClone;
	}

	public JsonObject toJson() {
		JsonObject firstPlayerTeams = new JsonObject();
		for (Map.Entry<String, String> entry : mFirstPlayerTeams.entrySet()) {
			firstPlayerTeams.addProperty(entry.getKey(), entry.getValue());
		}

		JsonObject laterPlayerTeams = new JsonObject();
		for (Map.Entry<String, String> entry : mLaterPlayerTeams.entrySet()) {
			laterPlayerTeams.addProperty(entry.getKey(), entry.getValue());
		}

		JsonObject record = new JsonObject();

		record.addProperty("advancement", mAdvancement);
		record.addProperty("instant", mInstant.toEpochMilli());
		record.add("first_player_teams", firstPlayerTeams);
		record.add("later_player_teams", laterPlayerTeams);

		return record;
	}
}
