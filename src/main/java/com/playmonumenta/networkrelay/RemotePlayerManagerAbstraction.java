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

	@Nullable
	protected abstract  String getPlayerShard(String playerName);
	@Nullable
	protected abstract String getPlayerShard(UUID playerUuid);

	@Nullable
	protected abstract RemotePlayerAbstraction getRemotePlayer(String playerName);
	@Nullable
	protected abstract RemotePlayerAbstraction getRemotePlayer(UUID playerUuid);

	protected abstract boolean isPlayerVisible(String playerName);
	protected abstract boolean isPlayerVisible(UUID playerUuid);
}
