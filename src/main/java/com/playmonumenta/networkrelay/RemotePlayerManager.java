package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashSet;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemotePlayerManager implements Listener {
	public static class RemotePlayer {
		public final UUID mUuid;
		public final String mName;
		public final boolean mIsHidden;
		public final boolean mIsOnline;
		public final String mShard;
		private final ConcurrentMap<String, JsonObject> mPluginData;

		public RemotePlayer(Player player, boolean isOnline) {
			mUuid = player.getUniqueId();
			mName = player.getName();
			mIsHidden = !internalPlayerVisibleTest(player);
			mIsOnline = isOnline;
			mShard = getShardName();
			mPluginData = new ConcurrentHashMap<>();

			// Gather Plugin data
			GatherRemotePlayerDataEvent event = new GatherRemotePlayerDataEvent();
			Bukkit.getPluginManager().callEvent(event);
			mPluginData.putAll(event.getPluginData());

			MMLog.fine("Created RemotePlayerState for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
		}

		public RemotePlayer(JsonObject remoteData) {
			mUuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
			mName = remoteData.get("playerName").getAsString();
			mIsHidden = remoteData.get("isHidden").getAsBoolean();
			mIsOnline = remoteData.get("isOnline").getAsBoolean();
			mShard = remoteData.get("shard").getAsString();
			mPluginData = deserializePluginData(remoteData);

			MMLog.fine("Received RemotePlayerState for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
		}

		@Nullable
		public JsonObject getPluginData(String pluginId) {
			return mPluginData.get(pluginId);
		}

		private ConcurrentMap<String, JsonObject> deserializePluginData(JsonObject remoteData) {
			ConcurrentMap<String, JsonObject> pluginDataMap = new ConcurrentHashMap<>();
			// "plugins": {"plugin-name": {}}
			JsonObject pluginData = remoteData.getAsJsonObject("plugins");
			for (String key : pluginData.keySet()) {
				pluginDataMap.put(key, pluginData.getAsJsonObject(key));
			}
			return pluginDataMap;
		}

		private JsonObject serializePluginData() {
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
			remotePlayerData.add("plugins", serializePluginData());

			try {
				NetworkRelayAPI.sendExpiringBroadcastMessage(REMOTE_PLAYER_UPDATE_CHANNEL,
					remotePlayerData,
					REMOTE_PLAYER_MESSAGE_TTL);
			} catch (Exception e) {
				MMLog.severe("Failed to broadcast " + REMOTE_PLAYER_UPDATE_CHANNEL);
			}
		}
	}

	public static final int REMOTE_PLAYER_MESSAGE_TTL = 5;
	public static final String REMOTE_PLAYER_CHANNEL_BASE = "monumenta.redissync.remote_player";
	public static final String REMOTE_PLAYER_REFRESH_CHANNEL = REMOTE_PLAYER_CHANNEL_BASE + ".refresh";
	public static final String REMOTE_PLAYER_UPDATE_CHANNEL = REMOTE_PLAYER_CHANNEL_BASE + ".update";

	private static @MonotonicNonNull RemotePlayerManager INSTANCE = null;
	private static final Map<String, Map<String, RemotePlayer>> mRemotePlayerShardMapped = new ConcurrentSkipListMap<>();
	private static final Map<UUID, RemotePlayer> mRemotePlayersByUuid = new ConcurrentSkipListMap<>();
	private static final Map<String, RemotePlayer> mRemotePlayersByName = new ConcurrentSkipListMap<>();
	private static final Set<UUID> mVisiblePlayers = new ConcurrentSkipListSet<>();

	private RemotePlayerManager() {
		INSTANCE = this;
		String lShard = getShardName();
		try {
			for (String shard : NetworkRelayAPI.getOnlineShardNames()) {
				if (shard.equals(lShard)) {
					continue;
				}
				MMLog.fine("Registering shard " + shard);
				mRemotePlayerShardMapped.put(shard, new ConcurrentSkipListMap<>());
			}
		} catch (Exception ex) {
			MMLog.severe("Failed to get remote shard names");
			throw new RuntimeException("Failed to get remote shard names");
		}

		try {
			NetworkRelayAPI.sendExpiringBroadcastMessage(REMOTE_PLAYER_REFRESH_CHANNEL,
				new JsonObject(),
				REMOTE_PLAYER_MESSAGE_TTL);
		} catch (Exception ex) {
			MMLog.severe("Failed to broadcast to channel " + REMOTE_PLAYER_REFRESH_CHANNEL);
		}
	}

	public static RemotePlayerManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RemotePlayerManager();
		}
		return INSTANCE;
	}

	protected static String getShardName() {
		@Nullable String shardName = null;
		try {
			shardName = NetworkRelayAPI.getShardName();
		} catch (Exception e) {
			MMLog.severe("Failed to get shard name");
		}
		if (shardName == null) {
			throw new RuntimeException("Got null shard name");
		}
		return shardName;
	}

	public static Set<String> getAllOnlinePlayersName(boolean visibleOnly) {
		if (visibleOnly) {
			return getVisiblePlayerNames();
		}
		return new HashSet<>(mRemotePlayersByName.keySet());
	}

	public static boolean isPlayerOnline(UUID playerUuid) {
		return mRemotePlayersByUuid.containsKey(playerUuid);
	}

	public static boolean isPlayerOnline(String playerName) {
		return mRemotePlayersByName.containsKey(playerName);
	}

	public static Set<String> getVisiblePlayerNames() {
		Set<String> results = new ConcurrentSkipListSet<>();
		for (UUID playerUuid : mVisiblePlayers) {
			results.add(getPlayerName(playerUuid));
		}
		return results;
	}

	private static boolean internalPlayerVisibleTest(Player player) {
		for (MetadataValue meta : player.getMetadata("vanished")) {
			if (meta.asBoolean()) {
				return false;
			}
		}
		return true;
	}

	public static boolean isPlayerVisible(Player player) {
		boolean cachedResult = isPlayerVisible(player.getUniqueId());
		boolean currentResult = internalPlayerVisibleTest(player);
		if (cachedResult ^ currentResult) {
			refreshLocalPlayer(player);
		}
		return currentResult;
	}

	public static boolean isPlayerVisible(UUID playerUuid) {
		return mVisiblePlayers.contains(playerUuid);
	}

	public static boolean isPlayerVisible(String playerName) {
		@Nullable UUID playerUuid = getPlayerUuid(playerName);
		if (playerUuid == null) {
			return false;
		}
		return isPlayerVisible(playerUuid);
	}

	public static @Nullable String getPlayerName(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return null;
		}

		@Nullable RemotePlayer remotePlayer = mRemotePlayersByUuid.get(playerUuid);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mName;
	}

	public static @Nullable UUID getPlayerUuid(@Nullable String playerName) {
		if (playerName == null) {
			return null;
		}

		@Nullable RemotePlayer remotePlayer = mRemotePlayersByName.get(playerName);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mUuid;
	}

	@Nullable
	public static String getPlayerShard(@NotNull String username) {
		@Nullable RemotePlayer remotePlayer = getRemotePlayer(username);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mShard;
	}

	@Nullable
	public static String getPlayerShard(@NotNull UUID playerUuid) {
		@Nullable RemotePlayer remotePlayer = getRemotePlayer(playerUuid);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mShard;
	}

	@Nullable
	public static RemotePlayer getRemotePlayer(@NotNull String username) {
		return mRemotePlayersByName.get(username);
	}

	@Nullable
	public static RemotePlayer getRemotePlayer(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return null;
		}

		@Nullable RemotePlayer remotePlayer = mRemotePlayersByUuid.get(playerUuid);
		return remotePlayer;
	}

	public static void refreshLocalPlayers() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			refreshLocalPlayer(player);
		}
	}

	// Run this on local players whenever their information is out of date
	public static void refreshLocalPlayer(Player player) {
		MMLog.fine("Refreshing local player " + player.getName());
		RemotePlayer remotePlayer = new RemotePlayer(player, true);

		unregisterPlayer(remotePlayer.mUuid);
		MMLog.fine("Registering player " + remotePlayer.mName);
		mRemotePlayersByUuid.put(remotePlayer.mUuid, remotePlayer);
		mRemotePlayersByName.put(remotePlayer.mName, remotePlayer);
		if (remotePlayer.mIsHidden) {
			mVisiblePlayers.remove(remotePlayer.mUuid);
		} else {
			mVisiblePlayers.add(remotePlayer.mUuid);
		}
		remotePlayer.broadcast();

		RemotePlayerLoadedEvent remotePLE = new RemotePlayerLoadedEvent(remotePlayer);
		Bukkit.getServer().getPluginManager().callEvent(remotePLE);
	}

	private static void unregisterPlayer(UUID playerUuid) {
		@Nullable RemotePlayer lastPlayerState = mRemotePlayersByUuid.get(playerUuid);
		if (lastPlayerState != null) {
			MMLog.fine("Unregistering player " + lastPlayerState.mName);
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

	private static void remotePlayerChange(JsonObject data) {
		RemotePlayer remotePlayer;
		try {
			remotePlayer = new RemotePlayer(data);
		} catch (Exception ex) {
			MMLog.warning("Received invalid RemotePlayer");
			MMLog.severe(data.toString());
			MMLog.severe(ex.toString());
			return;
		}

		unregisterPlayer(remotePlayer.mUuid);
		if (remotePlayer.mIsOnline) {
			@Nullable Map<String, RemotePlayer> shardRemotePlayers = mRemotePlayerShardMapped.get(remotePlayer.mShard);
			if (shardRemotePlayers != null) {
				shardRemotePlayers.put(remotePlayer.mName, remotePlayer);
			}
			MMLog.fine("Registering remote player " + remotePlayer.mName);
			mRemotePlayersByUuid.put(remotePlayer.mUuid, remotePlayer);
			mRemotePlayersByName.put(remotePlayer.mName, remotePlayer);
			if (!remotePlayer.mIsHidden) {
				mVisiblePlayers.add(remotePlayer.mUuid);
			}
			RemotePlayerLoadedEvent remotePLE = new RemotePlayerLoadedEvent(remotePlayer);
			Bukkit.getServer().getPluginManager().callEvent(remotePLE);
			return;
		}

		@Nullable Player localPlayer = Bukkit.getPlayer(remotePlayer.mUuid);
		if (localPlayer != null && localPlayer.isOnline()) {
			// Player logged off on remote shard, but is locally online.
			// This can happen if the remote shard was not notified the player logged in here in time.
			MMLog.fine("Detected race condition, triggering refresh on " + remotePlayer.mName);
			refreshLocalPlayer(localPlayer);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void destOnlineEvent(DestOnlineEvent event) {
		String remoteShardName = event.getDest();
		if (getShardName().equals(remoteShardName)) {
			return;
		}
		MMLog.fine("Registering shard " + remoteShardName);
		mRemotePlayerShardMapped.put(remoteShardName, new ConcurrentSkipListMap<>());
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void destOfflineEvent(DestOfflineEvent event) {
		String remoteShardName = event.getDest();
		@Nullable Map<String, RemotePlayer> remotePlayers = mRemotePlayerShardMapped.get(remoteShardName);
		if (remotePlayers == null) {
			return;
		}

		MMLog.fine("Unregistering shard " + remoteShardName);
		Map<String, RemotePlayer> remotePlayersCopy = new ConcurrentSkipListMap<>(remotePlayers);
		for (Map.Entry<String, RemotePlayer> playerEntry : remotePlayersCopy.entrySet()) {
			RemotePlayer remotePlayer = playerEntry.getValue();
			unregisterPlayer(remotePlayer.mUuid);
		}
		mRemotePlayerShardMapped.remove(remoteShardName);
	}

	// Player ran a command
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
		String command = event.getMessage();

		if (command.startsWith("/pv ")
			|| command.equals("/pv")
			|| command.contains("vanish")) {
			Bukkit.getScheduler().runTask(NetworkRelay.getInstance(), RemotePlayerManager::refreshLocalPlayers);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		refreshLocalPlayer(player);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		RemotePlayer oldRemotePlayer = mRemotePlayersByUuid.get(player.getUniqueId());
		if (oldRemotePlayer != null && !oldRemotePlayer.mShard.equals(getShardName())) {
			MMLog.fine("Refusing to unregister player " + player.getName() + ": they are on another shard");
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
						CustomLogger.getInstance().ifPresent(logger -> logger.severe("Got " + REMOTE_PLAYER_UPDATE_CHANNEL + " channel with null data"));
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
}
