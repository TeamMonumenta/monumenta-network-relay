package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.velocitypowered.api.event.ResultedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GatherRemotePlayerDataEventVelocity implements ResultedEvent<GatherRemotePlayerDataEventVelocity.GatherRemotePlayerDataResult> {
	private GatherRemotePlayerDataResult mResult = GatherRemotePlayerDataResult.defaultResult();
	public final RemotePlayerAbstraction mRemotePlayer;

	public GatherRemotePlayerDataEventVelocity(RemotePlayerAbstraction remotePlayer) {
		mRemotePlayer = remotePlayer;
	}

	@Override
	public GatherRemotePlayerDataResult getResult() {
		return mResult;
	}

	@Override
	public void setResult(GatherRemotePlayerDataResult result) {
		this.mResult = Objects.requireNonNull(result);
	}

	public static final class GatherRemotePlayerDataResult implements ResultedEvent.Result {
		private static final GatherRemotePlayerDataResult DEFAULT_RESULT = new GatherRemotePlayerDataResult();

		private final Map<String, JsonObject> mPluginData = new HashMap<>();

		@Override
		public boolean isAllowed() {
			return true;
		}

		/**
		 * Include data in the player's plugin data (that should be retrievable for this shard).
		 *
		 * @param pluginId A unique string key identifying this plugin.
		 * @param key      The key of this piece of data.
		 * @param data     The data to be included.
		 */
		public void addPluginData(String pluginId, String key, JsonObject data) {
			if (mPluginData.containsKey(pluginId)) {
				JsonObject root = mPluginData.get(pluginId);
				root.add(key, data);
				return;
			}
			JsonObject root = new JsonObject();
			root.add(key, data);
			mPluginData.put(pluginId, root);
		}

		/**
		 * Sets the plugin data that should be retrievable for this shard
		 *
		 * @param pluginId A unique string key identifying t			his plugin.
		 * @param data     The data to save.
		 */
		public void setPluginData(String pluginId, JsonObject data) {
			mPluginData.remove(pluginId);
			mPluginData.put(pluginId, data);
		}

		/**
		 * Gets the already registered plugin data
		 */
		public Map<String, JsonObject> getPluginData() {
			return mPluginData;
		}

		public static GatherRemotePlayerDataResult defaultResult() {
			return DEFAULT_RESULT;
		}
	}
}
