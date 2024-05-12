package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
import org.jetbrains.annotations.Nullable;

public final class RemotePlayerManagerPaper extends RemotePlayerManagerAbstraction implements Listener {
	private static @MonotonicNonNull RemotePlayerManagerPaper INSTANCE = null;

	private RemotePlayerManagerPaper() {
		INSTANCE = this;
		String lShard = getServerId();
		try {
			for (String shard : NetworkRelayAPI.getOnlineShardNames()) {
				if (shard.equals(lShard)) {
					continue;
				}
				MMLog.fine(() -> "Registering shard " + shard);
				registerServer(shard);
			}
		} catch (Exception ex) {
			MMLog.severe(() -> "Failed to get remote shard names");
			throw new RuntimeException("Failed to get remote shard names");
		}

		try {
			NetworkRelayAPI.sendExpiringBroadcastMessage(REMOTE_PLAYER_REFRESH_CHANNEL,
				new JsonObject(),
				REMOTE_PLAYER_MESSAGE_TTL);
		} catch (Exception ex) {
			MMLog.severe(() -> "Failed to broadcast to channel " + REMOTE_PLAYER_REFRESH_CHANNEL);
		}
	}

	static RemotePlayerManagerPaper getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RemotePlayerManagerPaper();
		}
		return INSTANCE;
	}

	static String getServerId() {
		@Nullable String shardName = null;
		try {
			shardName = NetworkRelayAPI.getShardName();
		} catch (Exception e) {
			MMLog.severe(() -> "Failed to get shard name");
		}
		if (shardName == null) {
			throw new RuntimeException("Got null shard name");
		}
		return shardName;
	}

	static RemotePlayerMinecraft fromLocal(Player player, boolean isOnline) {
		return new RemotePlayerMinecraft(
			getServerId(),
			player.getUniqueId(),
			player.getName(),
			RemotePlayerManagerPaper.internalPlayerHiddenTest(player),
			isOnline,
			player.getWorld().getName()
		);
	}

	static boolean internalPlayerHiddenTest(Player player) {
		for (MetadataValue meta : player.getMetadata("vanished")) {
			if (meta.asBoolean()) {
				return true;
			}
		}
		return false;
	}

	boolean isPlayerVisible(Player player) {
		boolean cachedResult = isPlayerVisible(player.getUniqueId());
		boolean currentResult = !internalPlayerHiddenTest(player);
		if (cachedResult ^ currentResult) {
			refreshLocalPlayer(player);
		}
		return currentResult;
	}

	void refreshLocalPlayers() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			refreshLocalPlayer(player);
		}
	}

	@Override
	boolean refreshLocalPlayer(UUID uuid) {
		@Nullable Player localPlayer = Bukkit.getPlayer(uuid);
		if (localPlayer != null && localPlayer.isOnline()) {
			refreshLocalPlayer(localPlayer);
			return true;
		}
		return false;
	}

	// Run this on local players whenever their information is out of date
	boolean refreshLocalPlayer(Player player) {
		MMLog.fine(() -> "Refreshing local player " + player.getName());
		RemotePlayerMinecraft localPlayer = fromLocal(player, true);

		// update local player with data'
		if (updateLocalPlayer(localPlayer, false)) {
			localPlayer.broadcast();
		}
		return true;
	}

	// We recieved data from another server, add more data
	void remotePlayerChange(JsonObject data) {
		if (data == null) {
			MMLog.severe(() -> "Null player data recieved from an unknown source!");
			return;
		}
		RemotePlayerAbstraction player = RemotePlayerAbstraction.from(data);
		if (player == null) {
			MMLog.severe(() -> "Invalid player data recieved from an unknown source!");
			return;
		}

		updateLocalPlayer(player, true);
	}

	@Override
	void callPlayerLoadEvent(RemotePlayerAbstraction player) {
		RemotePlayerLoadedEvent remotePE = new RemotePlayerLoadedEvent(player);
		Bukkit.getServer().getPluginManager().callEvent(remotePE);
	}

	@Override
	void callPlayerUnloadEvent(RemotePlayerAbstraction player) {
		RemotePlayerUnloadedEvent remotePE = new RemotePlayerUnloadedEvent(player);
		Bukkit.getServer().getPluginManager().callEvent(remotePE);
	}

	@Override
	void callPlayerUpdatedEvent(RemotePlayerAbstraction player) {
		RemotePlayerUpdatedEvent remotePE = new RemotePlayerUpdatedEvent(player);
		Bukkit.getServer().getPluginManager().callEvent(remotePE);
	}

	@Override
	Map<String, JsonObject> callGatherPluginDataEvent(RemotePlayerAbstraction player) {
		GatherRemotePlayerDataEvent remotePE = new GatherRemotePlayerDataEvent(player);
		Bukkit.getServer().getPluginManager().callEvent(remotePE);
		return remotePE.getPluginData();
	}

	@Override
	boolean checkAndRefreshIfLocalPlayer(RemotePlayerAbstraction player) {
		if (!player.getServerType().equals(RemotePlayerMinecraft.SERVER_TYPE)) {
			return false;
		}
		return refreshLocalPlayer(player.mUuid);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void destOnlineEvent(DestOnlineEvent event) {
		String remoteShardName = event.getDest();
		if (getServerId().equals(remoteShardName)) {
			return;
		}
		registerServer(remoteShardName);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void destOfflineEvent(DestOfflineEvent event) {
		String remoteShardName = event.getDest();
		unregisterServer(remoteShardName);
	}

	// Player ran a command
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
		String command = event.getMessage();

		if (command.startsWith("/pv ")
			|| "/pv".equals(command)
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
		// Run this with a 1 tick delay since a player can switch shards, since players can take a bit to switch
		Bukkit.getScheduler().runTaskLater(NetworkRelay.getInstance(), () -> {
			Player player = event.getPlayer();
			String playerShard = getPlayerShard(player.getUniqueId());
			if (playerShard != null && !playerShard.equals(getServerId())) {
				MMLog.warning(() -> "Refusing to unregister player " + player.getName() + ": they are on another shard");
				return;
			}
			RemotePlayerMinecraft localPlayer = fromLocal(player, false);
			if (updateLocalPlayer(localPlayer, false)) {
				localPlayer.broadcast();
			}
		}, 1L);
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
				if (!Objects.equals(event.getSource(), getServerId())) {
					if (data == null) {
						MMLog.severe(() -> "Got " + REMOTE_PLAYER_UPDATE_CHANNEL + " channel with null data");
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
