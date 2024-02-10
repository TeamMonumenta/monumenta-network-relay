package com.playmonumenta.networkrelay;

import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public abstract class RemotePlayerManagerAbstraction {

	protected abstract Set<String> getAllOnlinePlayersName(boolean visibleOnly);

	protected abstract boolean isPlayerOnline(String playerName);

	protected abstract boolean isPlayerOnline(UUID playerUuid);

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
