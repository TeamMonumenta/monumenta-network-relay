package com.playmonumenta.networkrelay;

import net.md_5.bungee.api.plugin.Event;

public class RemotePlayerLoadedEventBungee extends Event {

	public final RemotePlayerAbstraction mRemotePlayer;
	public final String mShard;

	public RemotePlayerLoadedEventBungee(RemotePlayerAbstraction remotePlayer) {
		mRemotePlayer = remotePlayer;
		mShard = remotePlayer.mShard;
	}
}
