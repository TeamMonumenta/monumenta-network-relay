package com.playmonumenta.networkrelay;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RemotePlayerUnloadedEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	public final RemotePlayerManagerPaper.RemotePlayer mRemotePlayer;
	public final String mShard;

	public RemotePlayerUnloadedEvent(RemotePlayerManagerPaper.RemotePlayer remotePlayer) {
		mRemotePlayer = remotePlayer;
		mShard = remotePlayer.mShard;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
