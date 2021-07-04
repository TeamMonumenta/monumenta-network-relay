package com.playmonumenta.networkrelay;

import java.util.Map;

import com.google.gson.JsonObject;

public interface RabbitMQManagerAbstractionInterface {
	void startHeartbeatRunnable(Runnable runnable, int delaySeconds, int periodSeconds);

	void scheduleProcessPacket(Runnable runnable);

	void stopHeartbeatRunnable();

	void stopServer();

	void sendMessageEvent(String channel, String source, JsonObject data);

	Map<String, JsonObject> gatherHeartbeatData();

	void sendDestOnlineEvent(String dest);

	void sendDestOfflineEvent(String dest);
}
