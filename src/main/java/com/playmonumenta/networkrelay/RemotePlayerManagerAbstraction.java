package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.Nullable;

public abstract class RemotePlayerManagerAbstraction {
	public static abstract class RemotePlayerAbstraction {
		private final ConcurrentMap<String, JsonObject> mPluginData;
		public final UUID mUuid;
		public final String mName;
		public final boolean mIsOnline;
		public final String mShard;

		protected RemotePlayerAbstraction(UUID mUuid, String mName, boolean mIsOnline, String mShard) {
			this.mUuid = mUuid;
			this.mName = mName;
			this.mIsOnline = mIsOnline;
			this.mShard = mShard;
			this.mPluginData = new ConcurrentHashMap<>();
			mPluginData.putAll(gatherPluginData());
		}

		protected RemotePlayerAbstraction(UUID mUuid, String mName, boolean mIsOnline, String mShard, JsonObject remoteData) {
			this.mUuid = mUuid;
			this.mName = mName;
			this.mIsOnline = mIsOnline;
			this.mShard = mShard;
			this.mPluginData = deserializePluginData(remoteData);
		}

		protected abstract Map<String, JsonObject> gatherPluginData();

		@Nullable
		public JsonObject getPluginData(String pluginId) {
			return mPluginData.get(pluginId);
		}

		protected ConcurrentMap<String, JsonObject> deserializePluginData(JsonObject remoteData) {
			ConcurrentMap<String, JsonObject> pluginDataMap = new ConcurrentHashMap<>();
			// "plugins": {"plugin-name": {}}
			JsonObject pluginData = remoteData.getAsJsonObject("plugins");
			for (String key : pluginData.keySet()) {
				pluginDataMap.put(key, pluginData.getAsJsonObject(key));
			}
			return pluginDataMap;
		}

		protected JsonObject serializePluginData() {
			JsonObject pluginData = new JsonObject();
			for (Map.Entry<String, JsonObject> entry : mPluginData.entrySet()) {
				pluginData.add(entry.getKey(), entry.getValue());
			}
			return pluginData;
		}
	}

	public static class RemotePlayerBungee extends RemotePlayerAbstraction {
		public final String mProxy;

		protected RemotePlayerBungee(UUID uuid, String name, boolean isOnline, String shard, String proxy) {
			super(uuid, name, isOnline, shard);
			this.mProxy = proxy;
		}

		@Override
		protected Map<String, JsonObject> gatherPluginData() {
			return new HashMap<>();
		}
	}

	protected abstract Set<String> getAllOnlinePlayersName(boolean visibleOnly);

	protected abstract boolean isPlayerOnline(String playerName);

	protected abstract boolean isPlayerOnline(UUID playerUuid);

	@Nullable
	protected abstract String getPlayerShard(String playerName);

	@Nullable
	protected abstract String getPlayerShard(UUID playerUuid);

	@Nullable
	protected abstract RemotePlayerAbstraction getRemotePlayer(String playerName);

	@Nullable
	protected abstract RemotePlayerAbstraction getRemotePlayer(UUID playerUuid);

	protected abstract boolean isPlayerVisible(String playerName);

	protected abstract boolean isPlayerVisible(UUID playerUuid);
}
