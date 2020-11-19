package com.playmonumenta.relay;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.relay.utils.FileUtils;
import com.playmonumenta.relay.utils.MessagingUtils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ServerProperties {
	private static final String FILE_NAME = "Properties.json";

	/* Only the most recent instance of this is used */
	private static ServerProperties INSTANCE = null;

	private boolean mJoinMessagesEnabled = false;
	private boolean mBroadcastCommandEnabled = true;
	private int mHTTPStatusPort = 8000;

	private String mShardName = "default_settings";
	private String mRabbitHost = "rabbitmq";

	public ServerProperties() {
		INSTANCE = this;
	}

	/*
	 * Ensures that INSTANCE is non null
	 * If it is null, creates a default instance with default values
	 */
	private static void ensureInstance() {
		if (INSTANCE == null) {
			new ServerProperties();
		}
	}

	public static boolean getJoinMessagesEnabled() {
		ensureInstance();
		return INSTANCE.mJoinMessagesEnabled;
	}

	public static boolean getBroadcastCommandEnabled() {
		ensureInstance();
		return INSTANCE.mBroadcastCommandEnabled;
	}

	public static int getHTTPStatusPort() {
		ensureInstance();
		return INSTANCE.mHTTPStatusPort;
	}

	public static String getShardName() {
		ensureInstance();
		return INSTANCE.mShardName;
	}

	public static String getRabbitHost() {
		ensureInstance();
		return INSTANCE.mRabbitHost;
	}

	public static void load(Plugin plugin, CommandSender sender) {
		ensureInstance();
		INSTANCE.loadInternal(plugin, sender);
	}

	private void loadInternal(Plugin plugin, CommandSender sender) {
		final String fileLocation = plugin.getDataFolder() + File.separator + FILE_NAME;

		try {
			String content = FileUtils.readFile(fileLocation);
			if (content != null && content != "") {
				loadFromString(plugin, content, sender);
			}
		} catch (FileNotFoundException e) {
			plugin.getLogger().info("Properties.json file does not exist - using default values");
		} catch (Exception e) {
			plugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Properties.json file does not exist - using default values");
				MessagingUtils.sendStackTrace(sender, e);
			}
		}
	}

	private void loadFromString(Plugin plugin, String content, CommandSender sender) throws Exception {
		if (content != null && content != "") {
			try {
				Gson gson = new Gson();

				//  Load the file - if it exists, then let's start parsing it.
				JsonObject object = gson.fromJson(content, JsonObject.class);
				if (object != null) {
					mJoinMessagesEnabled         = getPropertyValueBool(plugin, object, "joinMessagesEnabled", mJoinMessagesEnabled);
					mBroadcastCommandEnabled     = getPropertyValueBool(plugin, object, "broadcastCommandEnabled", mBroadcastCommandEnabled);
					mHTTPStatusPort              = getPropertyValueInt(plugin, object, "httpStatusPort", mHTTPStatusPort);

					mShardName                   = getPropertyValueString(plugin, object, "shardName", mShardName);
					mRabbitHost                  = getPropertyValueString(plugin, object, "rabbitHost", mRabbitHost);

					if (sender != null) {
						sender.sendMessage(ChatColor.GOLD + "Successfully reloaded monumenta configuration");
					}
				}
			} catch (Exception e) {
				plugin.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Failed to load configuration!");
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}
	}

	private boolean getPropertyValueBool(Plugin plugin, JsonObject object, String properyName, boolean defaultVal) {
		boolean value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsBoolean();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private int getPropertyValueInt(Plugin plugin, JsonObject object, String properyName, int defaultVal) {
		int value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsInt();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}

	private String getPropertyValueString(Plugin plugin, JsonObject object, String properyName, String defaultVal) {
		String value = defaultVal;

		JsonElement element = object.get(properyName);
		if (element != null) {
			value = element.getAsString();
		}

		plugin.getLogger().info("Properties: " + properyName + " = " + value);

		return value;
	}
}
