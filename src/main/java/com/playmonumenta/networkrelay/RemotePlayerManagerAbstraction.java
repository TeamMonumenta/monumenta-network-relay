package com.playmonumenta.networkrelay;

import com.playmonumenta.networkrelay.util.MMLog;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
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

	protected Set<RemotePlayerData> getOnlinePlayers() {
		return new HashSet<>(mRemotePlayersByUuid.values());
	}

	protected Set<RemotePlayerData> getVisiblePlayers() {
		return new HashSet<>(mVisiblePlayers);
	}

	protected Set<String> getOnlinePlayerNames() {
		return new TreeSet<>(mRemotePlayersByName.keySet());
	}

	protected Set<String> getVisiblePlayerNames() {
		Set<String> result = new TreeSet<>();
		for (RemotePlayerData remotePlayerData : mVisiblePlayers) {
			result.add(remotePlayerData.mName);
		}
		return result;
	}

	protected Set<UUID> getOnlinePlayerUuids() {
		return new HashSet<>(mRemotePlayersByUuid.keySet());
	}

	protected Set<UUID> getVisiblePlayerUuids() {
		Set<UUID> result = new HashSet<>();
		for (RemotePlayerData remotePlayerData : mVisiblePlayers) {
			result.add(remotePlayerData.mUuid);
		}
		return result;
	}

	protected boolean isPlayerOnline(String playerName) {
		return mRemotePlayersByName.containsKey(playerName);
	}

	protected boolean isPlayerOnline(UUID playerUuid) {
		return mRemotePlayersByUuid.containsKey(playerUuid);
	}

	protected boolean isPlayerVisible(String playerName) {
		RemotePlayerData player = mRemotePlayersByName.get(playerName);
		return player != null && !player.isHidden();
	}

	protected boolean isPlayerVisible(UUID playerUuid) {
		RemotePlayerData player = mRemotePlayersByUuid.get(playerUuid);
		return player != null && !player.isHidden();
	}

	protected @Nullable String getPlayerProxy(String playerName) {
		return getPlayerProxy(mRemotePlayersByName.get(playerName));
	}

	protected @Nullable String getPlayerProxy(UUID playerUuid) {
		return getPlayerProxy(mRemotePlayersByUuid.get(playerUuid));
	}

	protected @Nullable String getPlayerProxy(@Nullable RemotePlayerData remotePlayerData) {
		if (remotePlayerData == null) {
			return null;
		}
		RemotePlayerAbstraction proxyData = remotePlayerData.get("proxy");
		if (proxyData == null) {
			return null;
		}
		return proxyData.getServerId();
	}

	protected @Nullable String getPlayerShard(String playerName) {
		return getPlayerShard(mRemotePlayersByName.get(playerName));
	}

	protected @Nullable String getPlayerShard(UUID playerUuid) {
		return getPlayerShard(mRemotePlayersByUuid.get(playerUuid));
	}

	protected @Nullable String getPlayerShard(@Nullable RemotePlayerData remotePlayerData) {
		if (remotePlayerData == null) {
			return null;
		}
		RemotePlayerAbstraction minecraftData = remotePlayerData.get("minecraft");
		if (minecraftData == null) {
			return null;
		}
		return minecraftData.getServerId();
	}

	protected @Nullable RemotePlayerData getRemotePlayer(String playerName) {
		return mRemotePlayersByName.get(playerName);
	}

	protected @Nullable RemotePlayerData getRemotePlayer(UUID playerUuid) {
		return mRemotePlayersByUuid.get(playerUuid);
	}

	protected void registerPlayer(RemotePlayerAbstraction playerServerData) {
		if (playerServerData == null) {
			return;
		}
		MMLog.fine(() -> "Registering player: " + playerServerData);
		String serverType = playerServerData.getServerType();
		String serverId = playerServerData.getServerId();
		UUID playerId = playerServerData.mUuid;
		String playerName = playerServerData.mName;
		boolean isOnline = playerServerData.mIsOnline;
		if (!isOnline) {
			MMLog.fine(() -> "Player is offline instead; unregistering them");
		}

		// Handle UUID/name checks
		RemotePlayerData allPlayerData = mRemotePlayersByUuid.get(playerId);
		if (allPlayerData == null) {
			MMLog.fine(() -> "Player: " + playerName + " was previously offline network-wide");
			if (isOnline) {
				// TODO Add event for player logging in anywhere on the network, just the UUID/Name available so far
				allPlayerData = new RemotePlayerData(playerId, playerName);
				mRemotePlayersByUuid.put(playerId, allPlayerData);
				mRemotePlayersByName.put(playerName, allPlayerData);
			} else {
				MMLog.fine("Nothing to do!");
				return;
			}
		}

		RemotePlayerAbstraction oldPlayerServerData = allPlayerData.register(playerServerData);
		if (oldPlayerServerData != null) {
			String oldServerId = oldPlayerServerData.getServerId();
			MMLog.fine(() -> "Player: " + playerName + " was previously on server type " + serverType + ", ID " + oldServerId);
			Map<UUID, RemotePlayerData> allRemoteServerPlayerData = mRemotePlayersByServer.get(oldServerId);
			if (allRemoteServerPlayerData != null && (isOnline || oldServerId.equals(serverId))) {
				allRemoteServerPlayerData.remove(oldPlayerServerData.mUuid);
			}
		}
		if (allPlayerData.isHidden()) {
			mVisiblePlayers.remove(allPlayerData);
		} else {
			mVisiblePlayers.add(allPlayerData);
		}
		if (isOnline) {
			mRemotePlayersByServer.computeIfAbsent(serverId, k -> new ConcurrentSkipListMap<>())
				.put(playerId, allPlayerData);
		} else if (!allPlayerData.isOnline()) {
			// Last of that player's info is offline; unregister them completely
			mRemotePlayersByUuid.remove(playerId);
			mRemotePlayersByName.remove(playerName);
		}
	}

	protected boolean unregisterPlayer(UUID playerUuid) {
		RemotePlayerData allPlayerData = mRemotePlayersByUuid.remove(playerUuid);
		if (allPlayerData != null) {
			MMLog.fine(() -> "Unregistering player: " + allPlayerData);
			mRemotePlayersByName.remove(allPlayerData.mName);
			mVisiblePlayers.remove(allPlayerData);
			for (String serverType : allPlayerData.getServerTypes()) {
				RemotePlayerAbstraction remoteServerPlayerData = allPlayerData.get(serverType);
				if (remoteServerPlayerData == null) {
					continue;
				}
				String serverId = remoteServerPlayerData.getServerId();
				mRemotePlayersByServer.remove(serverId);
			}
			return true;
		}
		return false;
	}

	protected void updatePlayer(RemotePlayerAbstraction player) {
		// Update remote player caches with local data
		player.broadcast();
	}

	protected boolean registerServerId(String serverId) {
		if (mRemotePlayersByServer.containsKey(serverId)) {
			return false;
		}
		MMLog.fine("Registering shard " + serverId);
		mRemotePlayersByServer.put(serverId, new ConcurrentSkipListMap<>());
		return true;
	}

	protected boolean unregisterShard(String serverId) {
		ConcurrentMap<UUID, RemotePlayerData> remotePlayers = mRemotePlayersByServer.remove(serverId);
		if (remotePlayers == null) {
			return false;
		}

		MMLog.fine("Unregistering server ID " + serverId);
		String serverType = RabbitMQManager.getInstance().getOnlineDestinationType(serverId);
		if (serverType == null) {
			throw new RuntimeException("ERROR: Server type for server ID cleared before unregistering players from that server");
		}
		for (RemotePlayerData allPlayerData : remotePlayers.values()) {
			RemotePlayerAbstraction oldPlayerData = allPlayerData.unregister(serverType);
			if (oldPlayerData == null) {
				continue;
			}
			if (!allPlayerData.isOnline()) {
				// The player is now offline on all server types
				mRemotePlayersByUuid.remove(allPlayerData.mUuid);
				mRemotePlayersByName.remove(allPlayerData.mName);
			}
			if (allPlayerData.isHidden()) {
				mVisiblePlayers.remove(allPlayerData);
			} else {
				mVisiblePlayers.add(allPlayerData);
			}
		}
		return true;
	}
}
