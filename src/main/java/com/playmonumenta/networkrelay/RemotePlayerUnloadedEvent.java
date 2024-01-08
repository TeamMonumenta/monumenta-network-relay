package com.playmonumenta.networkrelay;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RemotePlayerUnloadedEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	public final RemotePlayerManager.RemotePlayer mRemotePlayer;
	public final String mShard;
	public RemotePlayerUnloadedEvent(@NotNull RemotePlayerManager.RemotePlayer remotePlayer) {
		mRemotePlayer = remotePlayer;
		mShard = remotePlayer.mShard;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
