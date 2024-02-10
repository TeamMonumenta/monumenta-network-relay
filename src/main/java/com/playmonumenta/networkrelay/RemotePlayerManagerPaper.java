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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemotePlayerManagerPaper extends RemotePlayerManagerAbstraction implements Listener {

	public static final int REMOTE_PLAYER_MESSAGE_TTL = 5;
	public static final String REMOTE_PLAYER_CHANNEL_BASE = "monumenta.redissync.remote_player";
	public static final String REMOTE_PLAYER_REFRESH_CHANNEL = REMOTE_PLAYER_CHANNEL_BASE + ".refresh";
	public static final String REMOTE_PLAYER_UPDATE_CHANNEL = REMOTE_PLAYER_CHANNEL_BASE + ".update";

	private static @MonotonicNonNull RemotePlayerManagerPaper INSTANCE = null;
	private static final Map<String, Map<String, RemotePlayerPaper>> mRemotePlayerShardMapped = new ConcurrentSkipListMap<>();
	private static final Map<UUID, RemotePlayerPaper> mRemotePlayersByUuid = new ConcurrentSkipListMap<>();
	private static final Map<String, RemotePlayerPaper> mRemotePlayersByName = new ConcurrentSkipListMap<>();
	private static final Set<UUID> mHiddenPlayers = new ConcurrentSkipListSet<>();

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

	protected static RemotePlayerPaper fromLocal(Player player, boolean isOnline) {
		return new RemotePlayerPaper(
			player.getUniqueId(),
			player.getName(),
			RemotePlayerManagerPaper.internalPlayerHiddenTest(player),
			isOnline,
			RemotePlayerManagerPaper.getShardName(),
			player.getWorld().getName()
		);
	}

	@Override
	protected Set<String> getAllOnlinePlayersName(boolean visibleOnly) {
		Set<String> visible = new HashSet<>(mRemotePlayersByName.keySet());
		if (visibleOnly) {
			visible.removeAll(getHiddenPlayerNames());
		}
		return visible;
	}

	@Override
	protected boolean isPlayerOnline(UUID playerUuid) {
		return mRemotePlayersByUuid.containsKey(playerUuid);
	}

	@Override
	protected boolean isPlayerOnline(String playerName) {
		return mRemotePlayersByName.containsKey(playerName);
	}

	protected Set<String> getHiddenPlayerNames() {
		Set<String> results = new ConcurrentSkipListSet<>();
		for (UUID playerUuid : mHiddenPlayers) {
			results.add(getPlayerName(playerUuid));
		}
		return results;
	}

	protected static boolean internalPlayerHiddenTest(Player player) {
		for (MetadataValue meta : player.getMetadata("vanished")) {
			if (meta.asBoolean()) {
				return true;
			}
		}
		return false;
	}

	protected boolean isPlayerVisible(Player player) {
		boolean cachedResult = isPlayerVisible(player.getUniqueId());
		boolean currentResult = !internalPlayerHiddenTest(player);
		if (cachedResult ^ currentResult) {
			refreshLocalPlayer(player);
		}
		return currentResult;
	}

	@Override
	protected boolean isPlayerVisible(UUID playerUuid) {
		return !mHiddenPlayers.contains(playerUuid);
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

		@Nullable RemotePlayerPaper remotePlayer = mRemotePlayersByUuid.get(playerUuid);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mName;
	}

	protected @Nullable UUID getPlayerUuid(@Nullable String playerName) {
		if (playerName == null) {
			return null;
		}

		@Nullable RemotePlayerPaper remotePlayer = mRemotePlayersByName.get(playerName);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.mUuid;
	}

	@Nullable
	@Override
	protected String getPlayerShard(@NotNull String username) {
		@Nullable RemotePlayerPaper remotePlayer = getRemotePlayer(username);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.getShard();
	}

	@Nullable
	@Override
	protected String getPlayerShard(@NotNull UUID playerUuid) {
		@Nullable RemotePlayerPaper remotePlayer = getRemotePlayer(playerUuid);
		if (remotePlayer == null) {
			return null;
		}
		return remotePlayer.getShard();
	}

	@Nullable
	@Override
	protected RemotePlayerPaper getRemotePlayer(@NotNull String username) {
		return mRemotePlayersByName.get(username);
	}

	@Nullable
	@Override
	protected RemotePlayerPaper getRemotePlayer(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return null;
		}

		@Nullable RemotePlayerPaper remotePlayer = mRemotePlayersByUuid.get(playerUuid);
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
		RemotePlayerPaper remotePlayer = fromLocal(player, true);

		unregisterPlayer(remotePlayer.mUuid);
		MMLog.fine("Registering player " + remotePlayer.mName);
		mRemotePlayersByUuid.put(remotePlayer.mUuid, remotePlayer);
		mRemotePlayersByName.put(remotePlayer.mName, remotePlayer);
		if (remotePlayer.isHidden()) {
			mHiddenPlayers.add(remotePlayer.mUuid);
		} else {
			mHiddenPlayers.remove(remotePlayer.mUuid);
		}
		remotePlayer.broadcast();

		RemotePlayerLoadedEvent remotePLE = new RemotePlayerLoadedEvent(remotePlayer);
		Bukkit.getServer().getPluginManager().callEvent(remotePLE);
	}

	private void unregisterPlayer(UUID playerUuid) {
		@Nullable RemotePlayerPaper lastPlayerState = mRemotePlayersByUuid.get(playerUuid);
		if (lastPlayerState != null) {
			MMLog.fine("Unregistering player " + lastPlayerState.mName);
			String lastLoc = lastPlayerState.getShard();
			@Nullable Map<String, RemotePlayerPaper> lastShardRemotePlayers = mRemotePlayerShardMapped.get(lastLoc);
			if (lastShardRemotePlayers != null) {
				lastShardRemotePlayers.remove(lastPlayerState.mName);
			}
			mRemotePlayersByUuid.remove(playerUuid);
			mRemotePlayersByName.remove(lastPlayerState.mName);
			mHiddenPlayers.remove(playerUuid);

			RemotePlayerUnloadedEvent event = new RemotePlayerUnloadedEvent(lastPlayerState);
			Bukkit.getServer().getPluginManager().callEvent(event);
		}
	}

	private void remotePlayerChange(JsonObject data) {
		RemotePlayerAbstraction remotePlayer;
		try {
			remotePlayer = RemotePlayerPaper.from(data);
		} catch (Exception ex) {
			MMLog.warning("Received invalid RemotePlayer");
			MMLog.severe(data.toString());
			MMLog.severe(ex.toString());
			return;
		}

		// TODO All of the broadcasting/receiving/tracking of player data needs to be separated from the tracking and creation of local player data objects that need broadcasting
		if (!(remotePlayer instanceof RemotePlayerPaper)) {
			MMLog.severe("NEED TO IMPLEMENT SUPPORT FOR OTHER REMOTE PLAYER SERVER TYPES");
			return;
		}
		RemotePlayerPaper remotePlayerPaper = (RemotePlayerPaper) remotePlayer;

		unregisterPlayer(remotePlayerPaper.mUuid);
		if (remotePlayerPaper.mIsOnline) {
			@Nullable Map<String, RemotePlayerPaper> shardRemotePlayers = mRemotePlayerShardMapped.get(remotePlayerPaper.getShard());
			if (shardRemotePlayers != null) {
				shardRemotePlayers.put(remotePlayerPaper.mName, remotePlayerPaper);
			}
			MMLog.fine("Registering remote player " + remotePlayerPaper.mName);
			mRemotePlayersByUuid.put(remotePlayerPaper.mUuid, remotePlayerPaper);
			mRemotePlayersByName.put(remotePlayerPaper.mName, remotePlayerPaper);
			if (remotePlayerPaper.isHidden()) {
				mHiddenPlayers.add(remotePlayerPaper.mUuid);
			}
			RemotePlayerLoadedEvent remotePLE = new RemotePlayerLoadedEvent(remotePlayerPaper);
			Bukkit.getServer().getPluginManager().callEvent(remotePLE);
			return;
		}

		@Nullable Player localPlayer = Bukkit.getPlayer(remotePlayerPaper.mUuid);
		if (localPlayer != null && localPlayer.isOnline()) {
			// Player logged off on remote shard, but is locally online.
			// This can happen if the remote shard was not notified the player logged in here in time.
			MMLog.fine("Detected race condition, triggering refresh on " + remotePlayerPaper.mName);
			refreshLocalPlayer(localPlayer);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
		String command = event.getMessage();

		if (command.startsWith("/pv ")
			|| command.equals("/pv")
			|| command.contains("vanish")) {
			Bukkit.getScheduler().runTask(NetworkRelay.getInstance(), this::refreshLocalPlayers);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		refreshLocalPlayer(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		RemotePlayerPaper oldRemotePlayer = mRemotePlayersByUuid.get(player.getUniqueId());
		if (oldRemotePlayer != null && !oldRemotePlayer.getShard().equals(getShardName())) {
			MMLog.fine("Refusing to unregister player " + player.getName() + ": they are on another shard");
			return;
		}
		RemotePlayerPaper remotePlayer = fromLocal(player, false);
		unregisterPlayer(remotePlayer.mUuid);
		remotePlayer.broadcast();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerChangedWorldEvent(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		refreshLocalPlayer(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
