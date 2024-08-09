package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.velocitypowered.api.event.ResultedEvent;
import java.util.Objects;

public class GatherHeartbeatDataEventVelocity implements ResultedEvent<GatherHeartbeatDataEventVelocity.GatherHeartbeatDataResult> {
	private GatherHeartbeatDataResult mResult = GatherHeartbeatDataResult.defaultResult();

	@Override
	public GatherHeartbeatDataResult getResult() {
		return mResult;
	}

	@Override
	public void setResult(GatherHeartbeatDataResult result) {
		this.mResult = Objects.requireNonNull(result);
	}

	public static final class GatherHeartbeatDataResult implements ResultedEvent.Result {
		private static final GatherHeartbeatDataResult DEFAULT_RESULT = new GatherHeartbeatDataResult();

		private final JsonObject mPluginData = new JsonObject();

		@Override
		public boolean isAllowed() {
			return true;
		}

		/**
		 * Sets the plugin data that should be retrievable for this shard
		 *
		 * @param pluginIdentifier  A unique string key identifying this plugin data
		 * @param pluginData        The data to save.
		 */
		public void setPluginData(String pluginIdentifier, JsonObject pluginData) {
			mPluginData.add(pluginIdentifier, pluginData);
		}

		/**
		 * Gets the plugin data that has been set by other plugins
		 */
		public JsonObject getPluginData() {
			return mPluginData;
		}

		public static GatherHeartbeatDataResult defaultResult() {
			return DEFAULT_RESULT;
		}
	}
}
