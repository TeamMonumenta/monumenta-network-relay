package com.playmonumenta.networkrelay;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class RabbitMQManagerAbstractionBungee implements RabbitMQManagerAbstractionInterface {
	private ScheduledTask mHeartbeatRunnable;
	private final Plugin mPlugin;

	protected RabbitMQManagerAbstractionBungee(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public void startHeartbeatRunnable(Runnable runnable, int delaySeconds, int periodSeconds) {
		mHeartbeatRunnable = mPlugin.getProxy().getScheduler().schedule(mPlugin, runnable, delaySeconds, periodSeconds, TimeUnit.SECONDS);
	}

	@Override
	public void scheduleProcessPacket(Runnable runnable) {
		mPlugin.getProxy().getScheduler().runAsync(mPlugin, runnable);
	}

	@Override
	public void stopHeartbeatRunnable() {
		mHeartbeatRunnable.cancel();
	}

	@Override
	public void stopServer() {
		mPlugin.getProxy().stop("Bungee lost connection to network relay / rabbitmq");
	}

	@Override
	public void sendMessageEvent(String channel, String source, JsonObject data) {
		NetworkRelayMessageEventBungee event = new NetworkRelayMessageEventBungee(channel, source, data);
		mPlugin.getProxy().getPluginManager().callEvent(event);
	}

	@Override
	public Map<String, JsonObject> gatherHeartbeatData() {
		GatherHeartbeatDataEventBungee event = new GatherHeartbeatDataEventBungee();
		mPlugin.getProxy().getPluginManager().callEvent(event);
		return event.getPluginData();
	}

	@Override
	public void sendDestOnlineEvent(String dest) {
		DestOnlineEventBungee event = new DestOnlineEventBungee(dest);
		mPlugin.getProxy().getPluginManager().callEvent(event);
	}

	@Override
	public void sendDestOfflineEvent(String dest) {
		DestOfflineEventBungee event = new DestOfflineEventBungee(dest);
		mPlugin.getProxy().getPluginManager().callEvent(event);
	}
}
