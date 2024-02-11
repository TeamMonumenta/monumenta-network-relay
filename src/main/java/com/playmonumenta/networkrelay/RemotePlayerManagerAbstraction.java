package com.playmonumenta.networkrelay;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public abstract class RemotePlayerManagerAbstraction {

	private static @MonotonicNonNull RemotePlayerManagerPaper INSTANCE = null;
	private static final Map<UUID, RemotePlayerAbstraction> mRemotePlayersByUuid = new ConcurrentSkipListMap<>();
	private static final Map<String, RemotePlayerAbstraction> mRemotePlayersByName = new ConcurrentSkipListMap<>();

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

	protected abstract boolean isPlayerOnline(String playerName);

	protected abstract boolean isPlayerOnline(UUID playerUuid);

	@Nullable
	protected abstract String getPlayerProxy(String playerName);

	@Nullable
	protected abstract String getPlayerProxy(UUID playerUuid);

	@Nullable
	protected abstract String getPlayerShard(String playerName);

	@Nullable
	protected abstract String getPlayerShard(UUID playerUuid);

	@Nullable
	protected abstract RemotePlayerAbstraction getRemotePlayer(String playerName);

	@Nullable
	protected abstract RemotePlayerAbstraction getRemotePlayer(UUID playerUuid);

	protected abstract boolean isPlayerVisible(String playerName);

	protected abstract boolean isPlayerVisible(UUID playerUuid);
}
