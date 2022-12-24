package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public interface RabbitMQManagerAbstractionInterface {
	void startHeartbeatRunnable(Runnable runnable, int delaySeconds, int periodSeconds);

	void scheduleProcessPacket(Runnable runnable);

	void stopHeartbeatRunnable();

	void stopServer();

	void sendMessageEvent(String channel, String source, JsonObject data);

	@Nullable JsonObject gatherHeartbeatData();

	void sendDestOnlineEvent(String dest);

	void sendDestOfflineEvent(String dest);
}
