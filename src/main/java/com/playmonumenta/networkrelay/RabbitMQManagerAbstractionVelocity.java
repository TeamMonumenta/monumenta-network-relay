package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class RabbitMQManagerAbstractionVelocity implements RabbitMQManagerAbstractionInterface {
	private @Nullable ScheduledTask mHeartbeatRunnable = null;
	private final NetworkRelayVelocity mPlugin;
	private final ProxyServer mServer;

	protected RabbitMQManagerAbstractionVelocity(NetworkRelayVelocity plugin) {
		mPlugin = plugin;
		mServer = plugin.mServer;
	}

	@Override
	public void startHeartbeatRunnable(Runnable runnable, int delaySeconds, int periodSeconds) {
		mHeartbeatRunnable = mServer.getScheduler().buildTask(mPlugin, runnable)
		.delay(delaySeconds, TimeUnit.SECONDS)
		.repeat(periodSeconds, TimeUnit.SECONDS)
		.schedule();
	}

	@Override
	public void scheduleProcessPacket(Runnable runnable) {
		mServer.getScheduler().buildTask(mPlugin, runnable).schedule();
	}

	@Override
	public void stopHeartbeatRunnable() {
		if (mHeartbeatRunnable != null) {
			mHeartbeatRunnable.cancel();
		}
	}

	@Override
	public void stopServer() {
		mServer.shutdown(Component.text("Bungee lost connection to network relay / rabbitmq"));
	}

	@Override
	public void sendMessageEvent(String channel, String source, JsonObject data) {
		NetworkRelayMessageEventGeneric event = new NetworkRelayMessageEventGeneric(channel, source, data);
		mServer.getEventManager().fireAndForget(event);
	}

	@Override
	public JsonObject gatherHeartbeatData() {
		GatherHeartbeatDataEventVelocity event = new GatherHeartbeatDataEventVelocity();
		try {
			mServer.getEventManager().fire(event).get(5, TimeUnit.SECONDS);
		} catch (Exception ex) {
			MMLog.severe("Timeout for 5 seconds when gathering heartbeat data");
			ex.printStackTrace();
		}
		return event.getResult().getPluginData();
	}

	@Override
	public void sendDestOnlineEvent(String dest) {
		DestOnlineEventGeneric event = new DestOnlineEventGeneric(dest);
		mServer.getEventManager().fireAndForget(event);
	}

	@Override
	public void sendDestOfflineEvent(String dest) {
		DestOfflineEventGeneric event = new DestOfflineEventGeneric(dest);
		mServer.getEventManager().fireAndForget(event);
	}
}
