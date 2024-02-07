package com.playmonumenta.networkrelay;

import java.util.Set;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class RemotePlayerAPI {
	@MonotonicNonNull
	private static RemotePlayerManagerAbstraction mManager = null;

	public static void init(RemotePlayerManagerAbstraction manager) {
		mManager = manager;
	}

	public static Set<String> getOnlinePlayerNames(boolean visibleOnly) {
		innerCheckManagerLoaded();
		return mManager.getAllOnlinePlayersName(visibleOnly);
	}

	public static boolean isPlayerOnline(String playerName) {
		innerCheckManagerLoaded();
		return mManager.isPlayerOnline(playerName);
	}

	public static boolean isPlayerOnline(UUID playerUuid) {
		innerCheckManagerLoaded();
		return mManager.isPlayerOnline(playerUuid);
	}

	@Nullable
	public static String getPlayerShard(String playerName) {
		innerCheckManagerLoaded();
		return mManager.getPlayerShard(playerName);
	}

	@Nullable
	public static String getPlayerShard(UUID playerUuid) {
		innerCheckManagerLoaded();
		return mManager.getPlayerShard(playerUuid);
	}

	@Nullable
	public static RemotePlayerManagerAbstraction.RemotePlayerAbstraction getRemotePlayer(String playerName) {
		innerCheckManagerLoaded();
		return mManager.getRemotePlayer(playerName);
	}

	@Nullable
	public static RemotePlayerManagerAbstraction.RemotePlayerAbstraction getRemotePlayer(UUID playerUuid) {
		innerCheckManagerLoaded();
		return mManager.getRemotePlayer(playerUuid);
	}

	public static boolean isPlayerVanished(String playerName) {
		innerCheckManagerLoaded();
		return mManager.isPlayerVisible(playerName);
	}
	public static boolean isPlayerVanished(UUID playerUuid) {
		innerCheckManagerLoaded();
		return mManager.isPlayerVisible(playerUuid);
	}

	public static boolean isManagerLoaded() {
		return !(mManager == null);
	}

	private static void innerCheckManagerLoaded() {
		if (mManager == null) {
			throw new IllegalStateException("RemotePlayerManager is not loaded");
		}
	}
}
