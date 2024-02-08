package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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

public class RemotePlayerManagerPaper extends RemotePlayerManagerAbstraction implements Listener {
	public static class RemotePlayerPaper extends RemotePlayerAbstraction {
		private final boolean mIsHidden;

		public RemotePlayerPaper(Player player, boolean isOnline) {
			super(player.getUniqueId(), player.getName(), isOnline, getShardName());
			this.mIsHidden = !internalPlayerVisibleTest(player);

			MMLog.fine("Created RemotePlayerState for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
		}

		public RemotePlayerPaper(UUID mUuid, String mName, boolean mIsHidden, boolean mIsOnline, String mShard, JsonObject remoteData) {
			super(mUuid, mName, mIsOnline, mShard, remoteData);
			this.mIsHidden = mIsHidden;

			MMLog.fine("Received RemotePlayerState for " + mName + " from " + mShard + ": " + (mIsOnline ? "online" : "offline"));
		}

		@Override
		protected Map<String, JsonObject> gatherPluginData() {
			GatherRemotePlayerDataEvent event = new GatherRemotePlayerDataEvent();
			Bukkit.getPluginManager().callEvent(event);
			return event.getPluginData();
		}

		public static RemotePlayerPaper from(JsonObject remoteData) {
			UUID uuid = UUID.fromString(remoteData.get("playerUuid").getAsString());
			String name = remoteData.get("playerName").getAsString();
			boolean isHidden = remoteData.get("isHidden").getAsBoolean();
			boolean isOnline = remoteData.get("isOnline").getAsBoolean();
			String shard = remoteData.get("shard").getAsString();
			return new RemotePlayerPaper(uuid, name, isHidden, isOnline, shard, remoteData);
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

	private static @MonotonicNonNull RemotePlayerManagerPaper INSTANCE = null;
	private static final Map<String, Map<String, RemotePlayerPaper>> mRemotePlayerShardMapped = new ConcurrentSkipListMap<>();
	private static final Map<UUID, RemotePlayerPaper> mRemotePlayersByUuid = new ConcurrentSkipListMap<>();
	private static final Map<String, RemotePlayerPaper> mRemotePlayersByName = new ConcurrentSkipListMap<>();
	private static final Set<UUID> mVisiblePlayers = new ConcurrentSkipListSet<>();

	private RemotePlayerManagerPaper() {
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

	protected static RemotePlayerManagerPaper getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RemotePlayerManagerPaper();
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

	@Override
	protected Set<String> getAllOnlinePlayersName(boolean visibleOnly) {
		if (visibleOnly) {
			return getVisiblePlayerNames();
		}
		return new HashSet<>(mRemotePlayersByName.keySet());
	}

	@Override
	protected boolean isPlayerOnline(UUID playerUuid) {
		return mRemotePlayersByUuid.containsKey(playerUuid);
	}

	@Override
	protected boolean isPlayerOnline(String playerName) {
		return mRemotePlayersByName.containsKey(playerName);
	}

	protected Set<String> getVisiblePlayerNames() {
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

	protected boolean isPlayerVisible(Player player) {
		boolean cachedResult = isPlayerVisible(player.getUniqueId());
		boolean currentResult = internalPlayerVisibleTest(player);
		if (cachedResult ^ currentResult) {
			refreshLocalPlayer(player);
		}
		return currentResult;
	}

	@Override
	protected boolean isPlayerVisible(UUID playerUuid) {
		return mVisiblePlayers.contains(playerUuid);
	}

	@Override
	protected boolean isPlayerVisible(String playerName) {
		@Nullable UUID playerUuid = getPlayerUuid(playerName);
		if (playerUuid == null) {
			return false;
		}
		return isPlayerVisible(playerUuid);
	}

	protected @Nullable String getPlayerName(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return null;
		}

		@Nullable RemotePlayerManagerPaper.RemotePlayerPaper remotePlayer = mRemotePlayersByUuid.get(playerUuid);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mName;
	}

	protected @Nullable UUID getPlayerUuid(@Nullable String playerName) {
		if (playerName == null) {
			return null;
		}

		@Nullable RemotePlayerManagerPaper.RemotePlayerPaper remotePlayer = mRemotePlayersByName.get(playerName);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mUuid;
	}

	@Nullable
	@Override
	protected String getPlayerShard(@NotNull String username) {
		@Nullable RemotePlayerManagerPaper.RemotePlayerPaper remotePlayer = getRemotePlayer(username);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mShard;
	}

	@Nullable
	@Override
	protected String getPlayerShard(@NotNull UUID playerUuid) {
		@Nullable RemotePlayerManagerPaper.RemotePlayerPaper remotePlayer = getRemotePlayer(playerUuid);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mShard;
	}

	@Nullable
	@Override
	protected RemotePlayerManagerPaper.RemotePlayerPaper getRemotePlayer(@NotNull String username) {
		return mRemotePlayersByName.get(username);
	}

	@Nullable
	@Override
	protected RemotePlayerManagerPaper.RemotePlayerPaper getRemotePlayer(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return null;
		}

		@Nullable RemotePlayerManagerPaper.RemotePlayerPaper remotePlayer = mRemotePlayersByUuid.get(playerUuid);
		return remotePlayer;
	}

	protected void refreshLocalPlayers() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			refreshLocalPlayer(player);
		}
	}

	// Run this on local players whenever their information is out of date
	protected void refreshLocalPlayer(Player player) {
		MMLog.fine("Refreshing local player " + player.getName());
		RemotePlayerPaper remotePlayer = new RemotePlayerPaper(player, true);

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

	private void unregisterPlayer(UUID playerUuid) {
		@Nullable RemotePlayerManagerPaper.RemotePlayerPaper lastPlayerState = mRemotePlayersByUuid.get(playerUuid);
		if (lastPlayerState != null) {
			MMLog.fine("Unregistering player " + lastPlayerState.mName);
			String lastLoc = lastPlayerState.mShard;
			@Nullable Map<String, RemotePlayerPaper> lastShardRemotePlayers = mRemotePlayerShardMapped.get(lastLoc);
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
		RemotePlayerPaper remotePlayer;
		try {
			remotePlayer = RemotePlayerPaper.from(data);
		} catch (Exception ex) {
			MMLog.warning("Received invalid RemotePlayer");
			MMLog.severe(data.toString());
			MMLog.severe(ex.toString());
			return;
		}

		unregisterPlayer(remotePlayer.mUuid);
		if (remotePlayer.mIsOnline) {
			@Nullable Map<String, RemotePlayerPaper> shardRemotePlayers = mRemotePlayerShardMapped.get(remotePlayer.mShard);
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
		@Nullable Map<String, RemotePlayerPaper> remotePlayers = mRemotePlayerShardMapped.get(remoteShardName);
		if (remotePlayers == null) {
			return;
		}

		MMLog.fine("Unregistering shard " + remoteShardName);
		Map<String, RemotePlayerPaper> remotePlayersCopy = new ConcurrentSkipListMap<>(remotePlayers);
		for (Map.Entry<String, RemotePlayerPaper> playerEntry : remotePlayersCopy.entrySet()) {
			RemotePlayerPaper remotePlayer = playerEntry.getValue();
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
			Bukkit.getScheduler().runTask(NetworkRelay.getInstance(), this::refreshLocalPlayers);
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
		RemotePlayerPaper oldRemotePlayer = mRemotePlayersByUuid.get(player.getUniqueId());
		if (oldRemotePlayer != null && !oldRemotePlayer.mShard.equals(getShardName())) {
			MMLog.fine("Refusing to unregister player " + player.getName() + ": they are on another shard");
			return;
		}
		RemotePlayerPaper remotePlayer = new RemotePlayerPaper(player, false);
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
