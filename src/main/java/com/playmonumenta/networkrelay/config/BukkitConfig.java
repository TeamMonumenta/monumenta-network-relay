package com.playmonumenta.networkrelay.config;

import com.playmonumenta.networkrelay.util.YamlConfig;
import java.io.File;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class BukkitConfig extends CommonConfig {
	public boolean mBroadcastCommandSendingEnabled;
	public boolean mBroadcastCommandReceivingEnabled;
	public @Nullable String mServerAddress;

	public BukkitConfig(Logger logger, File configFile, Class<?> networkRelayClass, String resourcePath) {
		Map<String, Object> config = YamlConfig.loadWithFallback(logger, configFile, networkRelayClass, resourcePath);
		loadCommon(logger, config);

		mBroadcastCommandSendingEnabled = getBoolean(config, "broadcast-command-sending-enabled", true);
		logger.info("broadcast-command-sending-enabled=" + mBroadcastCommandSendingEnabled);

		mBroadcastCommandReceivingEnabled = getBoolean(config, "broadcast-command-receiving-enabled", true);
		logger.info("broadcast-command-receiving-enabled=" + mBroadcastCommandReceivingEnabled);

		/* Server address defaults to environment variable NETWORK_RELAY_SERVER_ADDRESS if present */
		// TODO Verify this modified behavior is correct - originally the config had priority over the env var
		String serverAddress = getString(config, "server-address", "");
		if (serverAddress.isEmpty()) {
			serverAddress = System.getenv("NETWORK_RELAY_SERVER_ADDRESS");
			if (serverAddress != null && serverAddress.isEmpty()) {
				serverAddress = null;
			}
		}
		mServerAddress = serverAddress;
		if (mServerAddress == null) {
			logger.info("server-address=<unset>");
		} else {
			logger.info("server-address=" + mServerAddress);
		}
	}
}
