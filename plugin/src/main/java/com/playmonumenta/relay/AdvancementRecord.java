package com.playmonumenta.relay;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
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

	public Set<Map.Entry<String, String>> getNewlyFirstPlayers(AdvancementRecord oldRecord) {
		if (oldRecord == null) {
			return getFirstPlayerTeams().entrySet();
		}
		Set<Map.Entry<String, String>> newlyFirstPlayers;

		int oldComparedToCurrentRecord = oldRecord.mInstant.compareTo(mInstant);
		if (oldComparedToCurrentRecord < 0) {
			// Old record was faster, no newly faster records
			return new HashMap<String, String>().entrySet();
		}

		// Current record was at least as fast, so all current first records are at least as fast...
		newlyFirstPlayers = getFirstPlayerTeams().entrySet();

		// ...and old ones aren't "newly faster"
		newlyFirstPlayers.removeAll(oldRecord.getFirstPlayerTeams().entrySet());

		return newlyFirstPlayers;
	}

	public Set<Map.Entry<String, String>> getNewlyLaterPlayers(AdvancementRecord oldRecord) {
		if (oldRecord == null) {
			return getLaterPlayerTeams().entrySet();
		}
		Set<Map.Entry<String, String>> newlyLaterPlayers;

		int oldComparedToCurrentRecord = oldRecord.mInstant.compareTo(mInstant);
		if (oldComparedToCurrentRecord < 0) {
			// Old was faster, so all the entries from the current record are later
			newlyLaterPlayers = getFirstPlayerTeams().entrySet();
			newlyLaterPlayers.addAll(getLaterPlayerTeams().entrySet());

			// But, anything old is already accounted for, not "newly later"
			newlyLaterPlayers.removeAll(oldRecord.getFirstPlayerTeams().entrySet());
			newlyLaterPlayers.removeAll(oldRecord.getLaterPlayerTeams().entrySet());

			return newlyLaterPlayers;
		}

		// If the current record is as fast or faster than the old record, the logic is the same.

		// Any current later entries are newly later...
		newlyLaterPlayers = getLaterPlayerTeams().entrySet();

		// ...unless they're accounted for in the old record...
		newlyLaterPlayers.removeAll(oldRecord.getLaterPlayerTeams().entrySet());

		// ...or corrected to later times. Handle those separately.
		newlyLaterPlayers.removeAll(oldRecord.getFirstPlayerTeams().entrySet());

		return newlyLaterPlayers;
	}

	public Set<Map.Entry<String, String>> getCorrectedPlayers(AdvancementRecord oldRecord) {
		if (oldRecord == null) {
			return new HashMap<String, String>().entrySet();
		}
		Set<Map.Entry<String, String>> correctedLaterPlayers;

		int oldComparedToCurrentRecord = oldRecord.mInstant.compareTo(mInstant);
		if (oldComparedToCurrentRecord < 0) {
			// Old was faster, so nothing to do.
			return new HashMap<String, String>().entrySet();
		} else if (oldComparedToCurrentRecord == 0) {
			// Same time! Return first entries from the old record that are also a later entry of the current record.
			Set<Map.Entry<String, String>> currentLaterEntries = getLaterPlayerTeams().entrySet();
			correctedLaterPlayers = new HashMap<String, String>().entrySet();

			for (Map.Entry<String, String> entry : oldRecord.getFirstPlayerTeams().entrySet()) {
				if (currentLaterEntries.contains(entry)) {
					correctedLaterPlayers.add(entry);
				}
			}
		} else {
			// Current record was faster. Old first entries are "corrected later"...
			correctedLaterPlayers = oldRecord.getFirstPlayerTeams().entrySet();

			// ...assuming they're not also fastest on the current record.
			correctedLaterPlayers.removeAll(getFirstPlayerTeams().entrySet());
		}

		return correctedLaterPlayers;
	}

	public Set<String> getNewlyFirstTeams(AdvancementRecord oldRecord) {
		if (oldRecord == null) {
			return new HashSet<String>(getFirstPlayerTeams().values());
		}
		Set<String> newlyFirstTeams;

		int oldComparedToCurrentRecord = oldRecord.mInstant.compareTo(mInstant);
		if (oldComparedToCurrentRecord < 0) {
			// Old record was faster, no newly faster records
			return new HashSet<String>();
		}

		// Current record was at least as fast, so all current first records are at least as fast...
		newlyFirstTeams = new HashSet<String>(getFirstPlayerTeams().values());

		// ...and old ones aren't "newly faster"
		newlyFirstTeams.removeAll(oldRecord.getFirstPlayerTeams().values());

		return newlyFirstTeams;
	}

	public Set<String> getNewlyLaterTeams(AdvancementRecord oldRecord) {
		Set<String> newlyLaterTeams;
		if (oldRecord == null) {
			newlyLaterTeams = new HashSet<String>(getLaterPlayerTeams().values());
			newlyLaterTeams.removeAll(getFirstPlayerTeams().values());
			return newlyLaterTeams;
		}

		int oldComparedToCurrentRecord = oldRecord.mInstant.compareTo(mInstant);
		if (oldComparedToCurrentRecord < 0) {
			// Old was faster, so all the entries from the current record are later
			newlyLaterTeams = new HashSet<String>(getFirstPlayerTeams().values());
			newlyLaterTeams.addAll(getLaterPlayerTeams().values());

			// But, anything old is already accounted for, not "newly later"
			newlyLaterTeams.removeAll(oldRecord.getFirstPlayerTeams().values());
			newlyLaterTeams.removeAll(oldRecord.getLaterPlayerTeams().values());

			return newlyLaterTeams;
		}

		// If the current record is as fast or faster than the old record, the logic is the same.

		// Any current later entries are newly later...
		newlyLaterTeams = new HashSet<String>(getLaterPlayerTeams().values());

		// ...unless the same team is still in first...
		newlyLaterTeams.removeAll(getFirstPlayerTeams().values());

		// ...they're accounted for in the old record...
		newlyLaterTeams.removeAll(oldRecord.getLaterPlayerTeams().values());

		// ...or corrected to later times. Handle those separately.
		newlyLaterTeams.removeAll(oldRecord.getFirstPlayerTeams().values());

		return newlyLaterTeams;
	}

	public Set<String> getCorrectedTeams(AdvancementRecord oldRecord) {
		if (oldRecord == null) {
			return new HashSet<String>();
		}
		Set<String> correctedLaterTeams;

		int oldComparedToCurrentRecord = oldRecord.mInstant.compareTo(mInstant);
		if (oldComparedToCurrentRecord < 0) {
			// Old was faster, so nothing to do.
			return new HashSet<String>();
		} else if (oldComparedToCurrentRecord == 0) {
			// Same time! Return first teams from the current record that are also a later teams of the old record...
			Set<String> currentLaterEntries = new HashSet<String>(oldRecord.getLaterPlayerTeams().values());
			correctedLaterTeams = new HashSet<String>();

			for (String team : getFirstPlayerTeams().values()) {
				if (currentLaterEntries.contains(team)) {
					correctedLaterTeams.add(team);
				}
			}

			// ...but also remove teams that were first already.
			correctedLaterTeams.removeAll(oldRecord.getFirstPlayerTeams().values());
		} else {
			// Current record was faster. Old first entries are "corrected later"...
			correctedLaterTeams = new HashSet<String>(oldRecord.getFirstPlayerTeams().values());

			// ...assuming they're not also fastest on the current record.
			correctedLaterTeams.removeAll(getFirstPlayerTeams().values());
		}

		return correctedLaterTeams;
	}

	public AdvancementRecord cloneAndUpdate(AdvancementRecord newRecord) {
		AdvancementRecord updatedClone = new AdvancementRecord(this);

		if (newRecord == null) {
			return updatedClone;
		}

		int currentComparedToNewRecord = mInstant.compareTo(newRecord.getInstant());
		if (currentComparedToNewRecord < 0) {
			// Current was faster.
			// Update current later to include new first and later
			updatedClone.mLaterPlayerTeams.putAll(newRecord.getFirstPlayerTeams());
		} else if (currentComparedToNewRecord == 0) {
			// Same time! Add all first entries from new record...
			updatedClone.mFirstPlayerTeams.putAll(newRecord.getFirstPlayerTeams());

			// ...add all later entries from new record...
			updatedClone.mLaterPlayerTeams.putAll(newRecord.getLaterPlayerTeams());
			// ...and remove any later entries that are first in the new record.
			for (String player : newRecord.getFirstPlayerTeams().keySet()) {
				updatedClone.mLaterPlayerTeams.remove(player);
			}
		} else {
			// New record was faster. Update the instant...
			updatedClone.mInstant = newRecord.getInstant();
			// ...copy current first and new later to later...
			updatedClone.mLaterPlayerTeams.putAll(mFirstPlayerTeams);
			updatedClone.mLaterPlayerTeams.putAll(newRecord.getLaterPlayerTeams());
			// ...and replace updated first with new first
			updatedClone.mFirstPlayerTeams = newRecord.getFirstPlayerTeams();
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
