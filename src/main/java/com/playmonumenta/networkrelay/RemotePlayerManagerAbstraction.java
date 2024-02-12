package com.playmonumenta.networkrelay;

import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import org.jetbrains.annotations.Nullable;

public abstract class RemotePlayerManagerAbstraction {
	protected static final Map<UUID, RemotePlayerAbstraction> mRemotePlayersByUuid = new ConcurrentSkipListMap<>();
	protected static final Map<String, RemotePlayerAbstraction> mRemotePlayersByName = new ConcurrentSkipListMap<>();
	protected static final Map<String, Map<UUID, RemotePlayerAbstraction>> mRemotePlayerShards = new ConcurrentSkipListMap<>();
	protected static final Map<String, Map<UUID, RemotePlayerAbstraction>> mRemotePlayerProxies = new ConcurrentSkipListMap<>();

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
		MMLog.fine(() -> "Registering player <" + player.mName + ">,proxy:<" + player.mProxy + ">,shard:<" + player.mShard + ">,world:<" + player.mWorld + ">");
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
			MMLog.fine(() -> "Unregistering player <" + player.mName + ">,proxy:<" + player.mProxy + ">,shard:<" + player.mShard + ">,world:<" + player.mWorld + ">");
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
			player.update(localPlayer);
		}
	}

	protected boolean unregisterPlayerFromProxyList(UUID playerUuid) {
		boolean found = false;
		for (Entry<String, Map<UUID, RemotePlayerAbstraction>> proxy : mRemotePlayerProxies.entrySet()) {
			Map<UUID, RemotePlayerAbstraction> proxyPlayers = proxy.getValue();
			if (proxyPlayers.remove(playerUuid) != null) {
				mRemotePlayerProxies.put(proxy.getKey(), proxyPlayers);
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
				mRemotePlayerShards.put(shard.getKey(), shardPlayers);
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
}
