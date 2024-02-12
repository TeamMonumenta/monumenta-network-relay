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

	private RemotePlayerManagerPaper() {
		INSTANCE = this;
		String lShard = getShardName();
		try {
			for (String shard : NetworkRelayAPI.getOnlineShardNames()) {
				if (shard.equals(lShard)) {
					continue;
				}
				MMLog.fine("Registering shard " + shard);
				mRemotePlayerShards.put(shard, new ConcurrentSkipListMap<>());
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

	@Nullable
	@Override
	protected RemotePlayerPaper getRemotePlayer(@NotNull String username) {
		return (RemotePlayerPaper) super.getRemotePlayer(username);
	}

	@Nullable
	@Override
	protected RemotePlayerPaper getRemotePlayer(@NotNull UUID playerUuid) {
		return (RemotePlayerPaper) super.getRemotePlayer(playerUuid);
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

		// update local player with data
		updatePlayer(remotePlayer);
		unregisterPlayer(remotePlayer.mUuid);
		registerPlayer(remotePlayer);
		remotePlayer.broadcast();

		RemotePlayerLoadedEvent remotePLE = new RemotePlayerLoadedEvent(remotePlayer);
		Bukkit.getServer().getPluginManager().callEvent(remotePLE);
	}

	@Override
	protected boolean unregisterPlayer(UUID playerUuid) {
		RemotePlayerPaper player = getRemotePlayer(playerUuid);
		if (player != null) {
			super.unregisterPlayer(playerUuid);
			RemotePlayerUnloadedEvent event = new RemotePlayerUnloadedEvent((RemotePlayerPaper) player);
			Bukkit.getServer().getPluginManager().callEvent(event);
			return true;
		}
		return false;
	}

	private void remotePlayerChange(JsonObject data) {
		RemotePlayerAbstraction remotePlayer;
		try {
			remotePlayer = RemotePlayerAbstraction.from(data);
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
			MMLog.fine("Registering remote player " + remotePlayerPaper.mName);
			super.registerPlayer(remotePlayerPaper);
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
		registerShard(remoteShardName);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void destOfflineEvent(DestOfflineEvent event) {
		String remoteShardName = event.getDest();
		unregisterShard(remoteShardName);
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
		RemotePlayerPaper oldRemotePlayer = getRemotePlayer(player.getUniqueId());
		if (oldRemotePlayer != null && oldRemotePlayer.mShard != null && !oldRemotePlayer.mShard.equals(getShardName())) {
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
