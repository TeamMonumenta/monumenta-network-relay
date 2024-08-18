package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.util.MMLog;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;

public class RabbitMQManagerAbstractionVelocity implements RabbitMQManagerAbstractionInterface {
	private final RabbitMQExecutor mThread = new RabbitMQExecutor("RabbitMQ Thread");
	private final ProxyServer mServer;


	protected RabbitMQManagerAbstractionVelocity(NetworkRelayVelocity plugin) {
		// mPlugin = plugin;
		mServer = plugin.mServer;
	}

	@Override
	public void startHeartbeatRunnable(Runnable runnable, int delaySeconds, int periodSeconds) {
		mThread.scheduleRepeatingTask(runnable, delaySeconds, periodSeconds, TimeUnit.SECONDS);

	}

	@Override
	public void scheduleProcessPacket(Runnable runnable) {
		// mServer.getScheduler().buildTask(mPlugin, runnable).schedule();
		mThread.schedule(runnable);
	}

	@Override
	public void stopHeartbeatRunnable() {
		// ! Stopped in ProxyShutdownEvent
		mThread.stop();
	}

	@Override
	public void stopServer() {
		mThread.stop();
		mServer.shutdown(Component.text("Bungee lost connection to network relay / rabbitmq"));
	}

	@Override
	public void sendMessageEvent(String channel, String source, JsonObject data) {
		NetworkRelayMessageEventGeneric event = new NetworkRelayMessageEventGeneric(channel, source, data);
		mServer.getEventManager().fireAndForget(event);
	}

	@Override
	public synchronized JsonObject gatherHeartbeatData() {
		GatherHeartbeatDataEventVelocity event = new GatherHeartbeatDataEventVelocity();
		try {
			mServer.getEventManager().fire(event).get(1, TimeUnit.SECONDS);
		} catch (Exception ex) {
			MMLog.severe("Timeout for 1 seconds when gathering heartbeat data");
			ex.printStackTrace();
		}
		return event.getPluginData();
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

	@Override
	public void sendNetworkMessage(String destination, String channel, JsonObject data, com.rabbitmq.client.AMQP.BasicProperties properties) {
		mThread.schedule(() -> {
			try {
				RabbitMQManager.getInstance().sendNetworkMessage(destination, channel, data, properties);
			} catch (Exception ex) {
				MMLog.severe("Error sending RabbitMQ message from API");
				ex.printStackTrace();
			}
		});
	}
}
