package com.playmonumenta.networkrelay;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import net.md_5.bungee.api.plugin.Event;

public class GatherHeartbeatDataEventBungee extends Event {
	private Map<String, JsonObject> mPluginData = null;

	/**
	 * Sets the plugin data that should be retrievable for this shard
	 *
	 * @param pluginIdentifier  A unique string key identifying this plugin data
	 * @param pluginData        The data to save.
	 */
	public void setPluginData(String pluginIdentifier, JsonObject pluginData) {
		if (mPluginData == null) {
			mPluginData = new LinkedHashMap<>();
		}
		mPluginData.put(pluginIdentifier, pluginData);
	}

	/**
	 * Gets the plugin data that has been set by other plugins
	 */
	public Map<String, JsonObject> getPluginData() {
		return mPluginData;
	}
}
