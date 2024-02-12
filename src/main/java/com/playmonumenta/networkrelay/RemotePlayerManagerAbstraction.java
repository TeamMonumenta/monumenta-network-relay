package com.playmonumenta.networkrelay;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import org.jetbrains.annotations.Nullable;

public abstract class RemotePlayerManagerAbstraction {
	private static final Map<UUID, RemotePlayerAbstraction> mRemotePlayersByUuid = new ConcurrentSkipListMap<>();
	private static final Map<String, RemotePlayerAbstraction> mRemotePlayersByName = new ConcurrentSkipListMap<>();
	private static final Map<String, Map<UUID, RemotePlayerAbstraction>> mRemotePlayerShards = new ConcurrentSkipListMap<>();
	private static final Map<String, Map<UUID, RemotePlayerAbstraction>> mRemotePlayerProxies = new ConcurrentSkipListMap<>();

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

	protected void unregisterPlayer(UUID playerUuid) {
		RemotePlayerAbstraction player = mRemotePlayersByUuid.remove(playerUuid);
		if (player != null) {
			mRemotePlayersByName.remove(player.mName);
			if (player.mProxy != null) {
				Map<UUID, RemotePlayerAbstraction> proxyPlayers = mRemotePlayerProxies.get(player.mProxy);
				proxyPlayers.remove(playerUuid);
				mRemotePlayerProxies.put(player.mProxy, proxyPlayers);
			}
			if (player.mShard != null) {
				Map<UUID, RemotePlayerAbstraction> shardPlayers = mRemotePlayerShards.get(player.mShard);
				shardPlayers.remove(playerUuid);
				mRemotePlayerShards.put(player.mProxy, shardPlayers);
			}
		}
	}
}
