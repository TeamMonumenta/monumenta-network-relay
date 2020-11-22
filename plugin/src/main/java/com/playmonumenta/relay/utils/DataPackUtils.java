package com.playmonumenta.relay.utils;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.scoreboard.Team;
import org.bukkit.World;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class DataPackUtils {
	// Returns advancement json object for a datapack, or null.
	public static JsonObject getAdvancementJsonObject(File dataPackRoot, Advancement advancement) {
		if (dataPackRoot == null || advancement == null) {
			return null;
		}

		// The namespace (ie minecraft) and local path (ie nether/root.json) for an advancement.
		String advancementNamespace = advancement.getKey().getNamespace();
		String advancementLocalPath = advancement.getKey().getKey() + ".json";
		String pathWithinDatapack = "data/" + advancementNamespace + "/advancements/" + advancementLocalPath;

		String content = null;
		if (dataPackRoot.isDirectory()) {
			// Attempt to read as a datapack folder
			File advancementFile = new File(dataPackRoot, pathWithinDatapack.replace('/', File.separatorChar));
			if (advancementFile == null) {
				// Advancement not found, but it could be in another datapack.
				return null;
			}

			try {
				content = FileUtils.readFile(advancementFile.getPath());
			} catch (Exception e) {
				return null;
			}
		} else if (dataPackRoot.isFile() && dataPackRoot.getName().endsWith(".zip")) {
			// Attempt to read as a datapack zip
			try {
				content = FileUtils.readZipFile(dataPackRoot.getPath(), pathWithinDatapack);
			} catch (Exception e) {
				return null;
			}
		}

		try {
			if (content == null || content.isEmpty()) {
				return null;
			}

			Gson gson = new Gson();
			return gson.fromJson(content, JsonObject.class);
		} catch (Exception e) {
			return null;
		}
	}

	// Returns true if the advancement is visible within any available datapack, whether loaded or not.
	public static Collection<JsonObject> getAdvancementJsonObjects(Advancement advancement) {
		Collection<JsonObject> advancementObjects = new ArrayList<JsonObject>();

		for (File dataPackRoot : getDataPackRoots()) {
			JsonObject object = getAdvancementJsonObject(dataPackRoot, advancement);
			if (object != null) {
				advancementObjects.add(object);
			}
		}
		return advancementObjects;
	}

	// Returns a tellraw command string or null
	public static String getChatAnnouncement(Player player, JsonObject advancementJson) {
		if (player == null || advancementJson == null) {
			return null;
		}

		String frame = getChatAdvancementFrame(advancementJson);

		// Put the whole message together
		JsonArray translateWith = new JsonArray();
		translateWith.add(getChatPlayer(player));
		translateWith.add(getChatAdvancement(advancementJson));

		JsonObject advancementMessage = new JsonObject();
		advancementMessage.addProperty("translate", "chat.type.advancement." + frame);
		advancementMessage.add("with", translateWith);

		return "tellraw @a " + advancementMessage.toString();
	}

	public static JsonObject getChatAdvancement(JsonObject advancementJson) {
		if (advancementJson == null) {
			return null;
		}

		JsonObject displayObject = advancementJson.getAsJsonObject("display");
		if (displayObject == null) {
			return null;
		}

		// JsonElement.deepCopy() is not public; serializing/deserializing as a workaround.
		Gson gson = new Gson();

		JsonElement title = displayObject.get("title");
		if (title == null) {
			return null;
		}
		title = gson.fromJson(gson.toJson(title), JsonObject.class);
		JsonElement description = displayObject.get("description");
		if (description != null) {
			description = gson.fromJson(gson.toJson(description), JsonObject.class);
		}

		String frame = getChatAdvancementFrame(advancementJson);

		String color = "green";
		if (frame.equals("challenge")) {
			color = "light_purple";
		}

		JsonArray hoverExtra = new JsonArray();
		hoverExtra.add(title);
		if (description != null) {
			hoverExtra.add("\n");
			hoverExtra.add(description);
		}

		JsonObject hoverText = new JsonObject();
		hoverText.addProperty("color", color);
		hoverText.add("extra", hoverExtra);
		hoverText.addProperty("text", "");

		JsonObject hoverEvent = new JsonObject();
		hoverEvent.addProperty("action", "show_text");
		hoverEvent.add("value", hoverText);

		JsonArray advancementHoverableExtra = new JsonArray();
		advancementHoverableExtra.add("[");
		advancementHoverableExtra.add(title);
		advancementHoverableExtra.add("]");

		JsonObject advancementHoverableText = new JsonObject();
		advancementHoverableText.addProperty("color", color);
		advancementHoverableText.add("hoverEvent", hoverEvent);
		advancementHoverableText.add("extra", advancementHoverableExtra);
		advancementHoverableText.addProperty("text", "");

		return advancementHoverableText;
	}

	public static String getChatAdvancementFrame(JsonObject advancementJson) {
		if (advancementJson == null) {
			return "task";
		}

		JsonObject displayObject = advancementJson.getAsJsonObject("display");
		if (displayObject == null) {
			return "task";
		}

		String frame = null;
		JsonPrimitive framePrimitive = displayObject.getAsJsonPrimitive("frame");
		if (framePrimitive != null) {
			frame = framePrimitive.getAsString();
		}
		if (frame == null) {
			frame = "task";
		}

		return frame;
	}

	public static JsonObject getChatPlayer(Player player) {
		if (player == null) {
			return null;
		}

		String playerName = player.getName();
		String playerUuid = player.getUniqueId().toString();

		// Team part
		Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(playerName);
		String teamColor = "reset";
		String teamPrefix = "";
		String teamSuffix = "";
		if (team != null) {
			teamColor = team.getColor().toString().toLowerCase();
			teamPrefix = team.getPrefix();
			if (teamPrefix == null) {
				teamPrefix = "";
			}
			teamSuffix = team.getSuffix();
			if (teamSuffix == null) {
				teamSuffix = "";
			}
		}
		
		JsonObject playerTeamPrefixComponent = new JsonObject();
		playerTeamPrefixComponent.addProperty("text", teamPrefix);

		JsonObject playerTeamSuffixComponent = new JsonObject();
		playerTeamSuffixComponent.addProperty("text", teamSuffix);

		// Player part
		JsonObject playerClickEvent = new JsonObject();
		playerClickEvent.addProperty("action", "suggest_command");
		playerClickEvent.addProperty("value", "/tell " + playerName + " ");

		JsonObject playerHoverDetails = new JsonObject();
		playerHoverDetails.addProperty("type", "minecraft:player");
		playerHoverDetails.addProperty("id", playerUuid);
		playerHoverDetails.add("name", getChatSimpleText(playerName));

		JsonObject playerHoverEvent = new JsonObject();
		playerHoverEvent.addProperty("action", "show_entity");
		playerHoverEvent.add("contents", playerHoverDetails);

		JsonArray playerSelectorExtra = new JsonArray();
		playerSelectorExtra.add(playerTeamPrefixComponent);
		playerSelectorExtra.add(getChatSimpleText(playerName));
		playerSelectorExtra.add(playerTeamSuffixComponent);

		JsonObject playerSelectorResults = new JsonObject();
		playerSelectorResults.addProperty("color", teamColor);
		playerSelectorResults.addProperty("insertion", playerName);
		playerSelectorResults.add("clickEvent", playerClickEvent);
		playerSelectorResults.add("hoverEvent", playerHoverEvent);
		playerSelectorResults.add("extra", playerSelectorExtra);
		playerSelectorResults.addProperty("text", "");

		return playerSelectorResults;
	}

	public static JsonObject getChatSimpleText(String text) {
		if (text == null) {
			return null;
		}
		JsonObject textComponent = new JsonObject();
		textComponent.addProperty("text", text);
		return textComponent;
	}

	// Returns a list of datapack zip files and root folders.
	// Note that the built-in datapack is not returned. It can be unzipped from a client jar of the same version.
	// Also note that Bukkit does not include a way to directly list or access enabled datapacks.
	public static Collection<File> getDataPackRoots() {
		Collection<File> dataPackRoots = new ArrayList<File>();

		// The nether and end world folders don't include datapack folders, only the overworld.
		// There may be more than one overworld hosted by the same server, though.
		for (World world : Bukkit.getServer().getWorlds()) {
			File datapackFolder = new File(world.getWorldFolder(), "datapacks");
			if (datapackFolder == null || !datapackFolder.isDirectory()) {
				// This world may not have a datapacks folder. Ignore error.
				continue;
			}

			for (File possibleDatapack : datapackFolder.listFiles()) {
				// TODO check if possibleDatapack is valid.
				// Assuming all files/folders are valid datapacks for now.
				dataPackRoots.add(possibleDatapack);
			}
		}

		return dataPackRoots;
	}

	// Return the instant an advancement was awarded, or null if it was not.
	public static Instant getEarnedInstant(Player player, Advancement advancement) {
		if (player == null || advancement == null) {
			return null;
		}

		AdvancementProgress progress = player.getAdvancementProgress(advancement);
		if (progress == null) {
			return null;
		}

		Instant instant = null;
		for (String criteria : progress.getAwardedCriteria()) {
			if (criteria == null) {
				continue;
			}

			Instant criteriaInstant = progress.getDateAwarded(criteria).toInstant();
			if (criteriaInstant == null) {
				continue;
			}
			// The instant the advancement was awarded is the time of the last criteria.
			if (instant == null || criteriaInstant.compareTo(instant) > 0) {
				instant = criteriaInstant;
			}
		}

		return instant;
	}

	// Returns true if the advancement is visible within any available datapack, whether loaded or not.
	public static boolean isAnnouncedToChat(Advancement advancement) {
		for (JsonObject advancementObject : getAdvancementJsonObjects(advancement)) {
			if (isAnnouncedToChat(advancementObject)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAnnouncedToChat(JsonObject advancementJson) {
		if (advancementJson == null) {
			return false;
		}

		JsonObject displayObject = advancementJson.getAsJsonObject("display");
		if (displayObject == null || !displayObject.has("title")) {
			return false;
		}

		JsonPrimitive announceToChatPrimitive = displayObject.getAsJsonPrimitive("announce_to_chat");
		if (announceToChatPrimitive == null) {
			return true;
		}
		return announceToChatPrimitive.getAsBoolean();
	}
}
