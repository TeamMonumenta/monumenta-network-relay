package com.playmonumenta.networkrelay;

import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public abstract class RemotePlayerManagerAbstraction {
	public abstract static class RemotePlayerAbstraction {
		public final UUID mUuid;
		public final String mName;
		public final boolean mIsHidden;
		public final boolean mIsOnline;
		public final String mShard;

        protected RemotePlayerAbstraction(UUID mUuid, String mName, boolean mIsHidden, boolean mIsOnline, String mShard) {
            this.mUuid = mUuid;
            this.mName = mName;
            this.mIsHidden = mIsHidden;
            this.mIsOnline = mIsOnline;
            this.mShard = mShard;
        }
    }

	protected abstract Set<String> getAllOnlinePlayersName(boolean visibleOnly);

	protected abstract boolean isPlayerOnline(String playerName);
	protected abstract boolean isPlayerOnline(UUID playerUuid);

	protected abstract @Nullable String getPlayerShard(String playerName);
	protected abstract @Nullable String getPlayerShard(UUID playerUuid);

	protected abstract @Nullable RemotePlayerAbstraction getRemotePlayer(String playerName);
	protected abstract @Nullable RemotePlayerAbstraction getRemotePlayer(UUID playerUuid);

	protected abstract boolean isPlayerVisible(String playerName);
	protected abstract boolean isPlayerVisible(UUID playerUuid);
}
