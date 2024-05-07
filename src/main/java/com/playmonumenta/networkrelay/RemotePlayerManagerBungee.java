package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public final class RemotePlayerManagerBungee extends RemotePlayerManagerAbstraction implements Listener {
	private static @MonotonicNonNull RemotePlayerManagerBungee INSTANCE = null;

	private RemotePlayerManagerBungee() {
		INSTANCE = this;
		try {
			for (String shard : NetworkRelayAPI.getOnlineShardNames()) {
				MMLog.fine(() -> "Registering shard " + shard);
				registerServer(shard);
			}
		} catch (Exception ex) {
			MMLog.severe(() -> "Failed to get remote shard names");
			throw new RuntimeException("Failed to get remote shard names", ex);
		}

		try {
			NetworkRelayAPI.sendExpiringBroadcastMessage(REMOTE_PLAYER_REFRESH_CHANNEL,
				new JsonObject(),
				REMOTE_PLAYER_MESSAGE_TTL);
		} catch (Exception ex) {
			MMLog.severe(() -> "Failed to broadcast to channel " + REMOTE_PLAYER_REFRESH_CHANNEL);
		}
	}

	static RemotePlayerManagerBungee getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RemotePlayerManagerBungee();
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

	static RemotePlayerBungee fromLocal(ProxiedPlayer player, boolean isOnline) {
		// player.getServer() has no information prior to the ServerSwitchEvent - we populate the player's information in the PostLoginEvent
		@Nullable String targetShard = player.getServer() != null ? player.getServer().getInfo().getName() : "";
		return fromLocal(player, isOnline, targetShard);
	}

	static RemotePlayerBungee fromLocal(ProxiedPlayer player, boolean isOnline, String targetShard) {
		return new RemotePlayerBungee(
			getServerId(),
			player.getUniqueId(),
			player.getName(),
			isOnline,
			null,
			targetShard
		);
	}

	void refreshLocalPlayers() {
		for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
			refreshLocalPlayer(player);
		}
	}

	boolean refreshLocalPlayer(UUID uuid) {
		@Nullable ProxiedPlayer localPlayer = ProxyServer.getInstance().getPlayer(uuid);
		if (localPlayer != null && localPlayer.isConnected()) {
			refreshLocalPlayer(localPlayer);
			return true;
		}
		return false;
	}

	// Run this on local players whenever their information is out of date
	boolean refreshLocalPlayer(ProxiedPlayer player) {
		MMLog.fine(() -> "Refreshing local player " + player.getName());
		RemotePlayerBungee localPlayer = fromLocal(player, true);

		// update local player with data
		if (updateLocalPlayer(localPlayer, false)) {
			localPlayer.broadcast();
		}
		return true;
	}

	void remotePlayerChange(JsonObject data) {
		if (data == null) {
			MMLog.severe(() -> "Null player data recieved from an unknown source!");
			return;
		}
		RemotePlayerAbstraction player = RemotePlayerAbstraction.from(data);
		if (player == null) {
			// something really bad happened
			MMLog.severe(() -> "Invalid player data recieved from an unknown source!");
			return;
		}

		updateLocalPlayer(player, true);
	}

	void callPlayerLoadEvent(RemotePlayerAbstraction player) {
		RemotePlayerLoadedEventBungee remotePE = new RemotePlayerLoadedEventBungee(player);
		ProxyServer.getInstance().getPluginManager().callEvent(remotePE);
	}

	void callPlayerUnloadEvent(RemotePlayerAbstraction player) {
		RemotePlayerUnloadedEventBungee remotePE = new RemotePlayerUnloadedEventBungee(player);
		ProxyServer.getInstance().getPluginManager().callEvent(remotePE);
	}

	void callPlayerUpdatedEvent(RemotePlayerAbstraction player) {
		RemotePlayerUpdatedEventBungee remotePE = new RemotePlayerUpdatedEventBungee(player);
		ProxyServer.getInstance().getPluginManager().callEvent(remotePE);
	}

	Map<String, JsonObject> callGatherPluginDataEvent(RemotePlayerAbstraction player) {
		GatherRemotePlayerDataEventBungee remotePE = new GatherRemotePlayerDataEventBungee(player);
		ProxyServer.getInstance().getPluginManager().callEvent(remotePE);
		return remotePE.getPluginData();
	}

	boolean checkAndRefreshIfLocalPlayer(RemotePlayerAbstraction player) {
		if (!player.getServerType().equals(RemotePlayerBungee.SERVER_TYPE)) {
			return false;
		}
		return refreshLocalPlayer(player.mUuid);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void destOnlineEvent(DestOnlineEventBungee event) {
		String remoteShardName = event.getDest();
		if (getServerId().equals(remoteShardName)) {
			return;
		}
		registerServer(remoteShardName);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void destOfflineEvent(DestOfflineEventBungee event) {
		String remoteShardName = event.getDest();
		unregisterServer(remoteShardName);
	}

	// This is when the player logins into the proxy
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerConnectEvent(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();
		refreshLocalPlayer(player);
	}

	// This is when the player connects or reconnects to a shard on the proxy
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerChangedServerEvent(ServerSwitchEvent event) {
		ProxiedPlayer player = event.getPlayer();
		refreshLocalPlayer(player);
	}

	// This is when the player disconnects from the proxy
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerQuitEvent(PlayerDisconnectEvent event) {
		ProxiedPlayer player = event.getPlayer();
		String playerProxy = getPlayerProxy(player.getUniqueId());
		if (playerProxy != null && !playerProxy.equals(getServerId())) {
			MMLog.warning(() -> "Refusing to unregister player " + player.getName() + ": they are on another proxy");
			return;
		}
		RemotePlayerBungee localPlayer = fromLocal(player, false);
		if (updateLocalPlayer(localPlayer, false)) {
			localPlayer.broadcast();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void networkRelayMessageEventBungee(NetworkRelayMessageEventBungee event) {
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
