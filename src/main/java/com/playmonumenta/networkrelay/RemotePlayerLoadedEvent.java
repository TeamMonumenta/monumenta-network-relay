package com.playmonumenta.networkrelay;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RemotePlayerLoadedEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	public final RemotePlayerAbstraction mRemotePlayer;
	public final String mShard;

	public RemotePlayerLoadedEvent(RemotePlayerAbstraction remotePlayer) {
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
