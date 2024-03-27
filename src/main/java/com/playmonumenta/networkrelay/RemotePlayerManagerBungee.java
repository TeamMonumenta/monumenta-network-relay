package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class RemotePlayerManagerBungee extends RemotePlayerManagerAbstraction implements Listener {
	private static @MonotonicNonNull RemotePlayerManagerBungee INSTANCE = null;

	private RemotePlayerManagerBungee() {
		INSTANCE = this;
		try {
			for (String shard : NetworkRelayAPI.getOnlineShardNames()) {
				MMLog.fine("Registering shard " + shard);
				mRemotePlayersByServer.put(shard, new ConcurrentSkipListMap<>());
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

	protected static RemotePlayerManagerBungee getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RemotePlayerManagerBungee();
		}
		return INSTANCE;
	}

	protected static String getServerId() {
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

	protected static RemotePlayerBungee fromLocal(ProxiedPlayer player, boolean isOnline) {
		RemotePlayerBungee remotePlayer = new RemotePlayerBungee(
			getServerId(),
			player.getUniqueId(),
			player.getName(),
			isOnline,
			null,
			RemotePlayerManagerBungee.getServerId()
		);
		return remotePlayer;
	}

	protected void refreshLocalPlayers() {
		for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
			refreshLocalPlayer(player);
		}
	}

	// Run this on local players whenever their information is out of date
	protected void refreshLocalPlayer(ProxiedPlayer player) {
		MMLog.fine("Refreshing local player " + player.getName());
		RemotePlayerBungee localPlayer = fromLocal(player, true);

		// update local player with data
		registerPlayer(localPlayer);
		localPlayer.broadcast();

		RemotePlayerLoadedEventBungee remotePLE = new RemotePlayerLoadedEventBungee(localPlayer);
		ProxyServer.getInstance().getPluginManager().callEvent(remotePLE);
	}

	@Nullable
	protected RemotePlayerAbstraction remotePlayerChange(JsonObject data) {
		RemotePlayerAbstraction player = RemotePlayerAbstraction.from(data);

		if (player.mIsOnline) {
			RemotePlayerLoadedEventBungee remotePLE = new RemotePlayerLoadedEventBungee(player);
			ProxyServer.getInstance().getPluginManager().callEvent(remotePLE);
			return player;
		}

		@Nullable ProxiedPlayer localPlayer = ProxyServer.getInstance().getPlayer(player.mUuid);
		if (!player.mIsOnline && localPlayer != null && localPlayer.isConnected()) {
			// Player logged off on remote shard, but is locally online.
			// This can happen if the remote shard was not notified the player logged in here in time.
			MMLog.fine("Detected race condition, triggering refresh on " + player.mName);
			refreshLocalPlayer(localPlayer);
		}
		return player;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void destOnlineEvent(DestOnlineEventBungee event) {
		String remoteShardName = event.getDest();
		if (getServerId().equals(remoteShardName)) {
			return;
		}
		registerShard(remoteShardName);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void destOfflineEvent(DestOfflineEventBungee event) {
		String remoteShardName = event.getDest();
		unregisterShard(remoteShardName);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerJoinEvent(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();
		refreshLocalPlayer(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerQuitEvent(PlayerDisconnectEvent event) {
		ProxiedPlayer player = event.getPlayer();
		RemotePlayerBungee localPlayer = fromLocal(player, false);
		registerPlayer(localPlayer);
		localPlayer.broadcast();
	}

	// Technically this is already handled... by the shard
	// @EventHandler(priority = EventPriority.HIGHEST)
	// public void playerChangedServerEvent(ServerSwitchEvent event) {
	// 	ProxiedPlayer player = event.getPlayer();
	// 	refreshLocalPlayer(player);
	// }

	@EventHandler(priority = EventPriority.HIGHEST)
	public void networkRelayMessageEventBungee(NetworkRelayMessageEventBungee event) {
		switch (event.getChannel()) {
			case REMOTE_PLAYER_UPDATE_CHANNEL: {
				@Nullable JsonObject data = event.getData();
				if (!Objects.equals(event.getSource(), getServerId())) {
					if (data == null) {
						MMLog.severe("Got " + REMOTE_PLAYER_UPDATE_CHANNEL + " channel with null data");
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
