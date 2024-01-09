package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemotePlayerManager implements Listener {
	public static final String REMOTE_PLAYER_CHANNEL_BASE = "monumenta.redissync.remote_player";
	public static final String REMOTE_PLAYER_UPDATE_CHANNEL = REMOTE_PLAYER_CHANNEL_BASE + ".update";
	public static final String REMOTE_PLAYER_REFRESH_CHANNEL = REMOTE_PLAYER_CHANNEL_BASE + ".refresh";

	@MonotonicNonNull
	private static RemotePlayerManager INSTANCE = null;

	private static final Map<String, Map<String, RemotePlayer>> mRemotePlayerShardMapped = new ConcurrentSkipListMap<>();
	private static final Map<UUID, RemotePlayer> mRemotePlayersByUuid = new ConcurrentSkipListMap<>();
	private static final Map<String, RemotePlayer> mRemotePlayersByName = new ConcurrentSkipListMap<>();
	private static final Set<UUID> mVisiblePlayers = new ConcurrentSkipListSet<>();

	private final NetworkRelay mPlugin;

	protected RemotePlayerManager(NetworkRelay plugin) {
		mPlugin = plugin;
		String lShard = getShardName();
		try {
			for (String shard : NetworkRelayAPI.getOnlineShardNames()) {
				if (shard.equals(lShard)) {
					continue;
				}
				mRemotePlayerShardMapped.put(shard, new ConcurrentSkipListMap<>());
			}
		} catch (Exception ex) {
			mPlugin.getLogger().warning("Failed to get load shards");
		}

		try {
			NetworkRelayAPI.sendBroadcastMessage(REMOTE_PLAYER_REFRESH_CHANNEL, new JsonObject());
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to broadcast to channel " + REMOTE_PLAYER_REFRESH_CHANNEL);
		}

		INSTANCE = this;
	}

	@Nullable
	protected static RemotePlayerManager getInstance() {
		if (INSTANCE == null) {
			return null;
		}
		return INSTANCE;
	}

	protected Set<String> getAllOnlinePlayersName(boolean visibleOnly) {
		if (visibleOnly) {
			return getVisiblePlayerNames();
		}
		return mRemotePlayersByName.keySet();
	}

	protected Set<String> getVisiblePlayerNames() {
		Set<String> results = new ConcurrentSkipListSet<>();
		for (UUID playerUuid: mVisiblePlayers) {
			results.add(getPlayerName(playerUuid));
		}
		return results;
	}

	protected boolean isLocalPlayerVisible(Player player) {
		for (MetadataValue meta: player.getMetadata("vanished")) {
			if (meta.asBoolean()) {
				return false;
			}
		}
		return true;
	}

	protected boolean isPlayerVisible(Player player) {
		boolean cachedResult = isPlayerVisible(player.getUniqueId());
		boolean currentResult = isLocalPlayerVisible(player);
		if (cachedResult ^ currentResult) {
			refreshLocalPlayer(player);
		}
		return currentResult;
	}

	protected boolean isPlayerVisible(UUID playerUuid) {
		return mVisiblePlayers.contains(playerUuid);
	}

	protected boolean isPlayerVisible(String playerName) {
		@Nullable UUID playerUuid = getPlayerUuid(playerName);
		if (playerUuid == null) {
			return false;
		}
		return isPlayerVisible(playerUuid);
	}

	@Nullable
	protected String getPlayerShard(@NotNull String username) {
		@Nullable RemotePlayer remotePlayer = getRemotePlayer(username);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mShard;
	}

	@Nullable
	protected String getPlayerShard(@NotNull UUID playerUuid) {
		@Nullable RemotePlayer remotePlayer = getRemotePlayer(playerUuid);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mShard;
	}

	@Nullable
	protected RemotePlayer getRemotePlayer(@NotNull String username) {
		return getRemotePlayer(getPlayerUuid(username));
	}

	@Nullable
	protected RemotePlayer getRemotePlayer(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return null;
		}

		@Nullable RemotePlayer remotePlayer = mRemotePlayersByUuid.get(playerUuid);
		return remotePlayer;
	}

	@Nullable
	protected String getShardName() {
		@Nullable String shardName = null;
		try {
			shardName = NetworkRelayAPI.getShardName();
		} catch (Exception e) {
			mPlugin.getLogger().severe("Failed to retrieve shard name");
		}
		return shardName;
	}

	protected @Nullable String getPlayerName(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return null;
		}

		@Nullable RemotePlayer remotePlayer = mRemotePlayersByUuid.get(playerUuid);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mName;
	}

	protected @Nullable UUID getPlayerUuid(@Nullable String playerName) {
		if (playerName == null) {
			return null;
		}

		@Nullable RemotePlayer remotePlayer = mRemotePlayersByName.get(playerName);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mUuid;
	}

	protected void refreshLocalPlayers() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			refreshLocalPlayer(player);
		}
	}

	protected void refreshLocalPlayer(Player player) {
		mPlugin.getLogger().fine("Refreshing local player");
		RemotePlayer remotePlayer = new RemotePlayer(player, true);

		unregisterPlayer(remotePlayer.mUuid);
		mPlugin.getLogger().fine("Registering local player");
		mRemotePlayersByUuid.put(remotePlayer.mUuid, remotePlayer);
		mRemotePlayersByName.put(remotePlayer.mName, remotePlayer);
		if (remotePlayer.mIsHidden) {
			mVisiblePlayers.remove(remotePlayer.mUuid);
		} else {
			mVisiblePlayers.add(remotePlayer.mUuid);
		}
		remotePlayer.broadcast();
	}

	private void unregisterPlayer(UUID playerUuid) {
		@Nullable RemotePlayer lastPlayerState = mRemotePlayersByUuid.get(playerUuid);
		if (lastPlayerState != null) {
			mPlugin.getLogger().fine("Unregistering local player");
			String lastLoc = lastPlayerState.mShard;
			@Nullable Map<String, RemotePlayer> lastShardRemotePlayers = mRemotePlayerShardMapped.get(lastLoc);
			if (lastShardRemotePlayers != null) {
				lastShardRemotePlayers.remove(lastPlayerState.mName);
			}
			mRemotePlayersByUuid.remove(playerUuid);
			mRemotePlayersByName.remove(lastPlayerState.mName);
			mVisiblePlayers.remove(playerUuid);

			RemotePlayerUnloadedEvent event = new RemotePlayerUnloadedEvent(lastPlayerState);
			Bukkit.getServer().getPluginManager().callEvent(event);
		}
	}

	private void remotePlayerChange(JsonObject data) {
		RemotePlayer remotePlayer;
		try {
			remotePlayer = new RemotePlayer(data);
		} catch (Exception ex) {
			mPlugin.getLogger().warning("Received invalid RemotePlayer");
			return;
		}

		unregisterPlayer(remotePlayer.mUuid);
		if (remotePlayer.mIsOnline) {
			@Nullable Map<String, RemotePlayer> shardRemotePlayers = mRemotePlayerShardMapped.get(remotePlayer.mShard);
			if (shardRemotePlayers != null) {
				shardRemotePlayers.put(remotePlayer.mName, remotePlayer);
			}
			mPlugin.getLogger().fine("Registering local player from remote data");
			mRemotePlayersByUuid.put(remotePlayer.mUuid, remotePlayer);
			mRemotePlayersByName.put(remotePlayer.mName, remotePlayer);
			RemotePlayerLoadedEvent remotePLE = new RemotePlayerLoadedEvent(remotePlayer);
			Bukkit.getServer().getPluginManager().callEvent(remotePLE);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void destOnlineEvent(DestOnlineEvent event) {
		String remoteShardName = event.getDest();
		if (Objects.equals(getShardName(), remoteShardName)) {
			return;
		}
		mPlugin.getLogger().fine("Registering remote shard");
		mRemotePlayerShardMapped.put(remoteShardName, new ConcurrentSkipListMap<>());
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void destOfflineEvent(DestOfflineEvent event) {
		String remoteShardName = event.getDest();
		@Nullable Map<String, RemotePlayer> remotePlayers = mRemotePlayerShardMapped.get(remoteShardName);
		if (remotePlayers == null) {
			return;
		}

		mPlugin.getLogger().fine("Unregistering remote shard");
		Map<String, RemotePlayer> remotePlayerCopy = new ConcurrentSkipListMap<>(remotePlayers);
		for (Map.Entry<String, RemotePlayer> playerEntry : remotePlayerCopy.entrySet()) {
			RemotePlayer remotePlayer = playerEntry.getValue();
			unregisterPlayer(remotePlayer.mUuid);
		}
		mRemotePlayerShardMapped.remove(remoteShardName);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		refreshLocalPlayer(player);

		RemotePlayer remotePlayer = getRemotePlayer(player.getUniqueId());
		if (remotePlayer != null) {
			RemotePlayerLoadedEvent remotePLE = new RemotePlayerLoadedEvent(remotePlayer);
			Bukkit.getServer().getPluginManager().callEvent(remotePLE);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		RemotePlayer oldRemotePlayer = mRemotePlayersByUuid.get(player.getUniqueId());
		if (oldRemotePlayer != null && !oldRemotePlayer.mShard.equals(getShardName())) {
			mPlugin.getLogger().finer("Probable race condition: Stopped removal of player registered on another shard.");
			return;
		}
		RemotePlayer remotePlayer = new RemotePlayer(player, false);
		unregisterPlayer(remotePlayer.mUuid);
		remotePlayer.broadcast();
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void networkRelayMessageEvent(NetworkRelayMessageEvent event) {
		switch (event.getChannel()) {
			case REMOTE_PLAYER_UPDATE_CHANNEL: {
				@Nullable JsonObject data = event.getData();
				if (!Objects.equals(event.getSource(), getShardName())) {
					if (data == null) {
						mPlugin.getLogger().severe("Got " + REMOTE_PLAYER_UPDATE_CHANNEL + " channel with null data");
						return;
					}
					remotePlayerChange(data);
				}
				break;
			}
			case REMOTE_PLAYER_REFRESH_CHANNEL: {
				refreshLocalPlayers();
				break;
			}
			default: {
				break;
			}
		}
	}

	public static class RemotePlayer {
		public final UUID mUuid;
		public final String mName;
		public final String mShard;
		public final boolean mIsHidden;
		public final boolean mIsOnline;
		private final ConcurrentMap<String, JsonObject> mPluginData;

		public RemotePlayer(Player player, boolean isOnline) {
			mUuid = player.getUniqueId();
			mName = player.getName();
			RemotePlayerManager manager = RemotePlayerManager.getInstance();
			mShard = (manager != null && manager.getShardName() != null) ? manager.getShardName() : NetworkRelayAPI.getShardName();
			mIsHidden = (manager != null && !manager.isLocalPlayerVisible(player));
			mIsOnline = isOnline;
			mPluginData = new ConcurrentHashMap<>();
		}

		public RemotePlayer(JsonObject remoteData) {
			mUuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
			mName = remoteData.get("playerName").getAsString();
			mIsHidden = remoteData.get("isHidden").getAsBoolean();
			mIsOnline = remoteData.get("isOnline").getAsBoolean();
			mShard = remoteData.get("shard").getAsString();
			mPluginData = serializePluginData(remoteData);
		}

		@Nullable
		public JsonObject getPluginData(String pluginId) {
			return mPluginData.get(pluginId);
		}

		private ConcurrentMap<String, JsonObject> serializePluginData(JsonObject remoteData) {
			ConcurrentMap<String, JsonObject> pluginDataMap = new ConcurrentHashMap<>();
			//"plugins": {"plugin-name": {}}
			JsonObject pluginData = remoteData.getAsJsonObject("plugins");
			for (String key : pluginData.keySet()) {
				pluginDataMap.put(key, pluginData.getAsJsonObject(key));
			}
			return pluginDataMap;
		}

		private JsonObject deserializePluginData() {
			JsonObject pluginData = new JsonObject();
			for (Map.Entry<String, JsonObject> entry : mPluginData.entrySet()) {
				pluginData.add(entry.getKey(), entry.getValue());
			}
			return pluginData;
		}

		public void broadcast() {
			JsonObject remotePlayerData = new JsonObject();
			remotePlayerData.addProperty("playerUuid", mUuid.toString());
			remotePlayerData.addProperty("playerName", mName);
			remotePlayerData.addProperty("isHidden", mIsHidden);
			remotePlayerData.addProperty("isOnline", mIsOnline);
			remotePlayerData.addProperty("shard", mShard);

			//Gather Plugin data
			GatherRemotePlayerDataEvent event = new GatherRemotePlayerDataEvent();
			Bukkit.getPluginManager().callEvent(event);
			mPluginData.clear();
			mPluginData.putAll(event.getPluginData());

			remotePlayerData.add("plugins", deserializePluginData());

			try {
				NetworkRelayAPI.sendBroadcastMessage(RemotePlayerManager.REMOTE_PLAYER_UPDATE_CHANNEL,
					remotePlayerData);
			} catch (Exception e) {
				RemotePlayerManager manager = RemotePlayerManager.getInstance();
				if (manager != null) {
					manager.mPlugin.getLogger().warning("Failed to broadcast to channel " + REMOTE_PLAYER_UPDATE_CHANNEL + " reason: " + e.getMessage());
				}
			}
		}
	}
}
