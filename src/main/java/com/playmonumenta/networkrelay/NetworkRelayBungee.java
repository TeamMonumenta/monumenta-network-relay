package com.playmonumenta.networkrelay;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

public class NetworkRelayBungee extends Plugin {
	class Config {
		boolean runReceivedCommands;
		boolean autoRegisterServersToBungee;
		boolean autoUnregisterInactiveServersFromBungee;
		String shardName;
		String rabbitURI;
		int heartbeatInterval;
		int destinationTimeout;
		long defaultTTL;

		private Config() {
			File configFile = new File(getDataFolder(), "config.yml");

			/* Create the config file & directories if it does not exist */
			if (!configFile.exists()) {
				try {
					// Create parent directories if they do not exist
					configFile.getParentFile().mkdirs();

					// Copy the default config file
					Files.copy(getClass().getResourceAsStream("/default_config_bungee.yml"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ex) {
					getLogger().log(Level.SEVERE, "Failed to create configuration file");
				}
			}

			/* Load the config file & parse it */
			Configuration config;
			try {
				config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
			} catch (IOException ex) {
				getLogger().warning("Failed to load config file, using defaults: " + ex.getMessage());
				config = new Configuration();
			}

			String logLevel = config.getString("log-level", "INFO");
			runReceivedCommands = config.getBoolean("run-received-commands", true);
			/* Shard name defaults to environment variable NETWORK_RELAY_NAME if present */
			shardName = System.getenv("NETWORK_RELAY_NAME");
			if (shardName == null || shardName.isEmpty()) {
				shardName = "default-shard";
			}
			shardName = config.getString("shard-name", shardName); // Config file overrides env var
			autoRegisterServersToBungee = config.getBoolean("auto-register-servers-to-bungee", false);
			autoUnregisterInactiveServersFromBungee = config.getBoolean("auto-unregister-inactive-servers-from-bungee", false);
			rabbitURI = config.getString("rabbitmq-uri", "amqp://guest:guest@127.0.0.1:5672");
			heartbeatInterval = config.getInt("heartbeat-interval", 1);
			destinationTimeout = config.getInt("destination-timeout", 5);
			defaultTTL = config.getLong("default-time-to-live", 604800);

			/* Echo config */
			try {
				getLogger().setLevel(Level.parse(logLevel));
				getLogger().info("log-level=" + logLevel);
			} catch (Exception ex) {
				getLogger().warning("log-level=" + logLevel + " is invalid - defaulting to INFO");
			}
			getLogger().info("run-received-commands=" + runReceivedCommands);
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
			getLogger().info("auto-register-servers-to-bungee=" + autoRegisterServersToBungee);
			if (autoUnregisterInactiveServersFromBungee && !autoRegisterServersToBungee) {
				getLogger().warning("Config mismatch - auto-unregister-inactive-servers-from-bungee auto-register-servers-to-bungee=false");
				getLogger().warning("Setting auto-unregister-inactive-servers-from-bungee");
				autoUnregisterInactiveServersFromBungee = false;
			}
			getLogger().info("auto-unregister-inactive-servers-from-bungee=" + autoUnregisterInactiveServersFromBungee);

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
		}
	}

	private @Nullable RabbitMQManager mRabbitMQManager = null;
	private @Nullable CustomLogger mLogger = null;
	private @Nullable Config mConfig = null;

	@Override
	public void onLoad() {
		mConfig = new Config();

		if (mConfig.autoRegisterServersToBungee) {
			/* Replace the default configuration adapter with a threadsafe variant with similar behavior */

			getLogger().info("Replacing bungee configuration loader with custom thread-safe version");
			getLogger().info("This may break other plugins that manipulate the bungee config");
			getProxy().setConfigurationAdapter(new BungeeThreadsafeYamlConfig());
		}
	}

	@Override
	public void onEnable() {
		if (mConfig == null) {
			mConfig = new Config();
		}

		getProxy().getPluginManager().registerListener(this, new BungeeNetworkMessageListener(getLogger(), mConfig.runReceivedCommands, mConfig.autoRegisterServersToBungee, mConfig.autoUnregisterInactiveServersFromBungee));

		try {
			mRabbitMQManager = new RabbitMQManager(new RabbitMQManagerAbstractionBungee(this), getLogger(), mConfig.shardName, mConfig.rabbitURI, mConfig.heartbeatInterval, mConfig.destinationTimeout, mConfig.defaultTTL);
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
