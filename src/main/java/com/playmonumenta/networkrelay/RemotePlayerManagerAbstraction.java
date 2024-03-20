package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.jetbrains.annotations.Nullable;

public abstract class RemotePlayerManagerAbstraction {
	public static final int REMOTE_PLAYER_MESSAGE_TTL = 5;
	public static final String REMOTE_PLAYER_CHANNEL_BASE = "monumenta.networkrelay.remote_player";
	public static final String REMOTE_PLAYER_REFRESH_CHANNEL = REMOTE_PLAYER_CHANNEL_BASE + ".refresh";
	public static final String REMOTE_PLAYER_UPDATE_CHANNEL = REMOTE_PLAYER_CHANNEL_BASE + ".update";

	// Fast lookup of player by UUID
	protected static final ConcurrentMap<UUID, RemotePlayerData> mRemotePlayersByUuid = new ConcurrentSkipListMap<>();
	// Fast lookup of player by name
	protected static final ConcurrentMap<String, RemotePlayerData> mRemotePlayersByName = new ConcurrentSkipListMap<>();
	// Required to handle timeout for a given server
	protected static final ConcurrentMap<String, ConcurrentMap<UUID, RemotePlayerData>> mRemotePlayersByServer
		= new ConcurrentSkipListMap<>();
	// Fast lookup of visible players
	protected static final ConcurrentSkipListSet<RemotePlayerData> mVisiblePlayers = new ConcurrentSkipListSet<>();

	protected Set<RemotePlayerData> getAllOnlinePlayers() {
		return new HashSet<>(mRemotePlayersByUuid.values());
	}

	protected Set<RemotePlayerData> getVisiblePlayers() {
		return new HashSet<>(mVisiblePlayers);
	}

	protected Set<String> getAllOnlinePlayersName(boolean visibleOnly) {
		Set<String> visible = new HashSet<>(mRemotePlayersByName.keySet());
		if (!visibleOnly) {
			return visible;
		}
		for (Entry<String, RemotePlayerAbstraction> data : mRemotePlayersByName.entrySet()) {
			RemotePlayerAbstraction player = data.getValue();
			if (player == null || player.mIsHidden) {
				visible.remove(data.getKey());
			}
		}
		return visible;
	}

	protected Set<UUID> getAllOnlinePlayersUuids(boolean visibleOnly) {
		Set<UUID> visible = new HashSet<>(mRemotePlayersByUuid.keySet());
		if (!visibleOnly) {
			return visible;
		}
		for (Entry<UUID, RemotePlayerAbstraction> data : mRemotePlayersByUuid.entrySet()) {
			RemotePlayerAbstraction player = data.getValue();
			if (player == null || player.mIsHidden) {
				visible.remove(data.getKey());
			}
		}
		return visible;
	}

	protected boolean isPlayerOnline(String playerName) {
		return mRemotePlayersByName.containsKey(playerName);
	}

	protected boolean isPlayerOnline(UUID playerUuid) {
		return mRemotePlayersByUuid.containsKey(playerUuid);
	}

	@Nullable
	protected String getPlayerProxy(String playerName) {
		RemotePlayerAbstraction player = mRemotePlayersByName.get(playerName);
		return player != null ? player.mProxy : null;
	}

	@Nullable
	protected String getPlayerProxy(UUID playerUuid) {
		RemotePlayerAbstraction player = mRemotePlayersByUuid.get(playerUuid);
		return player != null ? player.mProxy : null;
	}

	@Nullable
	protected String getPlayerShard(String playerName) {
		RemotePlayerAbstraction player = mRemotePlayersByName.get(playerName);
		return player != null ? player.mShard : null;
	}

	@Nullable
	protected String getPlayerShard(UUID playerUuid) {
		RemotePlayerAbstraction player = mRemotePlayersByUuid.get(playerUuid);
		return player != null ? player.mShard : null;
	}

	@Nullable
	protected RemotePlayerAbstraction getRemotePlayer(String playerName) {
		return mRemotePlayersByName.get(playerName);
	}

	@Nullable
	protected RemotePlayerAbstraction getRemotePlayer(UUID playerUuid) {
		return mRemotePlayersByUuid.get(playerUuid);
	}

	protected boolean isPlayerVisible(String playerName) {
		RemotePlayerAbstraction player = mRemotePlayersByName.get(playerName);
		return player != null ? player.mIsHidden : false;
	}

	protected boolean isPlayerVisible(UUID playerUuid) {
		RemotePlayerAbstraction player = mRemotePlayersByUuid.get(playerUuid);
		return player != null ? player.mIsHidden : false;
	}

	protected void registerPlayer(RemotePlayerAbstraction player) {
		if (player == null) {
			return;
		}
		MMLog.fine(() -> "Registering player: " + player.toString());
		mRemotePlayersByUuid.put(player.mUuid, player);
		mRemotePlayersByName.put(player.mName, player);
		if (player.mProxy != null) {
			Map<UUID, RemotePlayerAbstraction> proxyPlayers = mRemotePlayerProxies.get(player.mProxy);
			if (proxyPlayers == null) {
				proxyPlayers = new ConcurrentSkipListMap<>();
			}
			proxyPlayers.put(player.mUuid, player);
			mRemotePlayerProxies.put(player.mProxy, proxyPlayers);
		}

		if (player.mShard != null) {
			Map<UUID, RemotePlayerAbstraction> shardPlayers = mRemotePlayerShards.get(player.mShard);
			if (shardPlayers == null) {
				shardPlayers = new ConcurrentSkipListMap<>();
			}
			shardPlayers.put(player.mUuid, player);
			mRemotePlayerShards.put(player.mShard, shardPlayers);
		}
	}

	@Nullable
	protected boolean unregisterPlayer(UUID playerUuid) {
		RemotePlayerAbstraction player = mRemotePlayersByUuid.remove(playerUuid);
		if (player != null) {
			MMLog.fine(() -> "Unregistering player: " + player.toString());
			mRemotePlayersByName.remove(player.mName);
			unregisterPlayerFromProxyList(playerUuid);
			unregisterPlayerFromShardList(playerUuid);
			return true;
		}
		return false;
	}

	protected void updatePlayer(RemotePlayerAbstraction player) {
		// update remote copy player with "some" local data
		RemotePlayerAbstraction localPlayer = getRemotePlayer(player.mUuid);
		if (localPlayer != null) {
			localPlayer.broadcast();
		}
	}

	protected boolean unregisterPlayerFromProxyList(UUID playerUuid) {
		boolean found = false;
		for (Entry<String, Map<UUID, RemotePlayerAbstraction>> proxy : mRemotePlayerProxies.entrySet()) {
			Map<UUID, RemotePlayerAbstraction> proxyPlayers = proxy.getValue();
			if (proxyPlayers.remove(playerUuid) != null) {
				proxy.setValue(proxyPlayers);
				found = true;
			}
		}
		return found;
	}

	protected boolean unregisterPlayerFromShardList(UUID playerUuid) {
		boolean found = false;
		for (Entry<String, Map<UUID, RemotePlayerAbstraction>> shard : mRemotePlayerShards.entrySet()) {
			Map<UUID, RemotePlayerAbstraction> shardPlayers = shard.getValue();
			if (shardPlayers.remove(playerUuid) != null) {
				shard.setValue(shardPlayers);
				found = true;
			}
		}
		/*
		 * Other way to do this: possibly better?
		 * player: RemotePlayerAbstraction
		if (player.mShard != null) {
			Map<UUID, RemotePlayerAbstraction> shardPlayers = mRemotePlayerShards.get(player.mShard);
			shardPlayers.remove(playerUuid);
			mRemotePlayerShards.put(player.mShard, shardPlayers);
		}
		 */
		return found;
	}

	protected boolean registerShard(String shard) {
		if (mRemotePlayerShards.containsKey(shard)) {
			return false;
		}
		MMLog.fine("Registering shard " + shard);
		mRemotePlayerShards.put(shard, new ConcurrentSkipListMap<>());
		return true;
	}

	protected boolean unregisterShard(String shard) {
		@Nullable Map<UUID, RemotePlayerAbstraction> remotePlayers = mRemotePlayerShards.get(shard);
		if (remotePlayers == null) {
			return false;
		}

		MMLog.fine("Unregistering shard " + shard);
		Set<UUID> uuids = remotePlayers.keySet();
		for (UUID uuid: uuids) {
			unregisterPlayer(uuid);
		}
		mRemotePlayerShards.remove(shard);
		return true;
	}

	@Nullable
	protected RemotePlayerAbstraction remotePlayerChange(JsonObject data) {
		RemotePlayerAbstraction remotePlayer = null;
		try {
			remotePlayer = RemotePlayerAbstraction.from(data);
		} catch (Exception ex) {
			MMLog.warning("Received invalid RemotePlayer");
			MMLog.severe(data.toString());
			MMLog.severe(ex.toString());
			return remotePlayer;
		}

		updatePlayer(remotePlayer);
		unregisterPlayer(remotePlayer.mUuid);
		if (remotePlayer.mIsOnline) {
			MMLog.fine("Registering remote player " + remotePlayer.mName);
			registerPlayer(remotePlayer);
			return remotePlayer;
		}
		return remotePlayer;
	}
}
