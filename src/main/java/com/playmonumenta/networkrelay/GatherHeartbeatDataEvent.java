package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

public class GatherHeartbeatDataEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private @Nullable JsonObject mPluginData = null;

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
	public @Nullable JsonObject getPluginData() {
		return mPluginData;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
