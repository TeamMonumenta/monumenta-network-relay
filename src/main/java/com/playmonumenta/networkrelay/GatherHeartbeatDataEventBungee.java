package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;

import net.md_5.bungee.api.plugin.Event;

public class GatherHeartbeatDataEventBungee extends Event {
	private JsonObject mPluginData = null;

	/**
	 * Sets the plugin data that should be retrievable for this shard
	 *
	 * @param pluginIdentifier  A unique string key identifying this plugin data
	 * @param pluginData        The data to save.
	 */
	public void setPluginData(String pluginIdentifier, JsonObject pluginData) {
		if (mPluginData == null) {
			mPluginData = new JsonObject();
		}
		mPluginData.add(pluginIdentifier, pluginData);
	}

	/**
	 * Gets the plugin data that has been set by other plugins
	 */
	public JsonObject getPluginData() {
		return mPluginData;
	}
}
