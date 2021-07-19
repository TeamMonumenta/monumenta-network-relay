package com.playmonumenta.networkrelay;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {
	NetworkRelay mPlugin;

	public PlaceholderAPIIntegration(NetworkRelay plugin) {
		super();
		plugin.getLogger().info("Enabling PlaceholderAPI integration");
		mPlugin = plugin;
	}

	@Override
	public String getIdentifier() {
		return "network-relay";
	}

	@Override
	public String getPlugin() {
		return null;
	}

	@Override
	public String getAuthor() {
		return "Team Monumenta";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier) {
		// %network-relay_shard%
		if (identifier.equalsIgnoreCase("shard")) {
			RabbitMQManager instance = RabbitMQManager.getInstance();
			if (instance == null) {
				return "";
			}
			return instance.getShardName();
		}

		if (player == null) {
			return "";
		}

		// %network-relay_world%
		if (identifier.equalsIgnoreCase("world")) {
			return player.getWorld().getName();
		}

		return null;
	}
}
