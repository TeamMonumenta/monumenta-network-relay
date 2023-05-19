package com.playmonumenta.networkrelay;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class NetworkRelay extends JavaPlugin {
	private @Nullable RabbitMQManager mRabbitMQManager = null;
	private @Nullable BroadcastCommand mBroadcastCommand = null;
	private @Nullable CustomLogger mLogger = null;

	@Override
	public void onLoad() {
		mBroadcastCommand = new BroadcastCommand(this);
		ChangeLogLevelCommand.register(this);
		ListShardsCommand.register();
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
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

		String logLevel = config.getString("log-level", "INFO");
		boolean broadcastCommandSendingEnabled = config.getBoolean("broadcast-command-sending-enabled", true);
		boolean broadcastCommandReceivingEnabled = config.getBoolean("broadcast-command-receiving-enabled", true);
		/* Shard name defaults to environment variable NETWORK_RELAY_NAME if present */
		String shardName = System.getenv("NETWORK_RELAY_NAME");
		if (shardName == null || shardName.isEmpty()) {
			shardName = "default-shard";
		}
		shardName = config.getString("shard-name", shardName); // Config file overrides env var
		/* Server address defaults to environment variable NETWORK_RELAY_SERVER_ADDRESS if present */
		String serverAddress = System.getenv("NETWORK_RELAY_SERVER_ADDRESS");
		serverAddress = config.getString("server-address", serverAddress); // Config file overrides env var
		if (serverAddress == null || serverAddress.isEmpty()) {
			serverAddress = null;
		}
		String rabbitURI = config.getString("rabbitmq-uri", "amqp://guest:guest@127.0.0.1:5672");
		int heartbeatInterval = config.getInt("heartbeat-interval", 1);
		int destinationTimeout = config.getInt("destination-timeout", 5);
		long defaultTTL = config.getLong("default-time-to-live", 604800);

		/* Echo config */
		try {
			getLogger().setLevel(Level.parse(logLevel));
			getLogger().info("log-level=" + logLevel);
		} catch (Exception ex) {
			getLogger().warning("log-level=" + logLevel + " is invalid - defaulting to INFO");
		}
		getLogger().info("broadcast-command-sending-enabled=" + broadcastCommandSendingEnabled);
		getLogger().info("broadcast-command-receiving-enabled=" + broadcastCommandReceivingEnabled);
		if (rabbitURI.equals("amqp://guest:guest@127.0.0.1:5672")) {
			getLogger().info("rabbitmq-uri=<default>");
		} else {
			getLogger().info("rabbitmq-uri=<set>");
		}
		if (shardName.equals("default-shard")) {
			getLogger().warning("shard-name is default value 'default-shard' which should be changed!");
		} else {
			getLogger().info("shard-name=" + shardName);
		}
		if (serverAddress == null) {
			getLogger().info("server-address=<unset>");
		} else {
			getLogger().info("server-address=" + serverAddress);
		}
		if (heartbeatInterval <= 0) {
			getLogger().warning("heartbeat-interval is <= 0 which is invalid! Using default of 1.");
			heartbeatInterval = 1;
		} else {
			getLogger().info("heartbeat-interval=" + heartbeatInterval);
		}
		if (destinationTimeout <= 1) {
			getLogger().warning("destination-timeout is <= 1 which is invalid! Using default of 5.");
			destinationTimeout = 5;
		} else {
			getLogger().info("destination-timeout=" + destinationTimeout);
		}
		if (defaultTTL < 0) {
			getLogger().warning("default-time-to-live is < 0 which is invalid! Using default of 604800.");
			defaultTTL = 604800;
		} else {
			getLogger().info("default-time-to-live=" + defaultTTL);
		}

		Bukkit.getServer().getPluginManager().registerEvents(new NetworkMessageListener(serverAddress), this);

		/* Start relay components */
		BroadcastCommand.setEnabled(broadcastCommandSendingEnabled);
		if (broadcastCommandReceivingEnabled && mBroadcastCommand != null) {
			getServer().getPluginManager().registerEvents(mBroadcastCommand, this);
		}

		try {
			mRabbitMQManager = new RabbitMQManager(new RabbitMQManagerAbstractionBukkit(this), getLogger(), shardName, rabbitURI, heartbeatInterval, destinationTimeout, defaultTTL);
		} catch (Exception e) {
			getLogger().severe("RabbitMQ manager failed to initialize. This plugin will not function");
			e.printStackTrace();
		}

		// Provide placeholder API replacements if it is present
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new PlaceholderAPIIntegration(this).register();
		}
	}

	@Override
	public void onDisable() {
		if (mRabbitMQManager != null) {
			mRabbitMQManager.stop();
		}
	}

	@Override
	public Logger getLogger() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}
		return mLogger;
	}

	public void setLogLevel(Level level) {
		super.getLogger().info("Changing log level to: " + level.toString());
		getLogger().setLevel(level);
	}
}
