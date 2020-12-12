package com.playmonumenta.networkrelay;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class NetworkRelay extends JavaPlugin {
	private RabbitMQManager mRabbitMQManager = null;
	private BroadcastCommand mBroadcastCommand = null;

	@Override
	public void onLoad() {
		mBroadcastCommand = new BroadcastCommand(this);
	}

	@Override
	public void onEnable() {
		File configFile = new File(getDataFolder(), "config.yml");

		/* Create the config file & directories if it does not exist */
		if (!configFile.exists()) {
			try {
				// Create parent directories if they do not exist
				configFile.getParentFile().mkdirs();

				// Copy the default config file
				Files.copy(getClass().getResourceAsStream("/default_config.yml"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ex) {
				getLogger().log(Level.SEVERE, "Failed to create configuration file");
			}
		}

		/* Load the config file & parse it */
		boolean broadcastCommandSendingEnabled = true;
		boolean broadcastCommandReceivingEnabled = true;
		String shardName = "default_shard";
		String rabbitURI = "amqp://guest:guest@127.0.0.1:5672/";

		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

		if (!config.isBoolean("broadcast_command_sending_enabled")) {
			broadcastCommandSendingEnabled = config.getBoolean("broadcast_command_sending_enabled");
		}
		if (!config.isBoolean("broadcast_command_receiving_enabled")) {
			broadcastCommandReceivingEnabled = config.getBoolean("broadcast_command_receiving_enabled");
		}
		if (!config.isString("shard_name")) {
			shardName = config.getString("shard_name");
		}
		if (!config.isString("rabbitmq_uri")) {
			rabbitURI = config.getString("rabbitmq_uri");
		}

		/* Echo config */
		getLogger().info("broadcast_command_sending_enabled=" + broadcastCommandSendingEnabled);
		getLogger().info("broadcast_command_receiving_enabled=" + broadcastCommandReceivingEnabled);
		if (rabbitURI == "amqp://guest:guest@127.0.0.1:5672/") {
			getLogger().info("rabbitmq_uri=<set>");
		} else {
			getLogger().info("rabbitmq_uri=<default>");
		}
		if (shardName == "default_shard") {
			getLogger().info("shard_name=" + shardName);
		} else {
			getLogger().warning("shard_name is default value 'default_shard' which should be changed!");
		}

		/* Start relay components */
		BroadcastCommand.setEnabled(broadcastCommandSendingEnabled);
		if (broadcastCommandReceivingEnabled && mBroadcastCommand != null) {
			getServer().getPluginManager().registerEvents(mBroadcastCommand, this);
		}

		try {
			mRabbitMQManager = new RabbitMQManager(this, shardName, rabbitURI);
		} catch (Exception e) {
			getLogger().severe("RabbitMQ manager failed to initialize. This plugin will not function");
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		if (mRabbitMQManager != null) {
			mRabbitMQManager.stop();
		}
	}
}
