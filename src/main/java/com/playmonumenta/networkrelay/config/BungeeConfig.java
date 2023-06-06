package com.playmonumenta.networkrelay.config;

import com.playmonumenta.networkrelay.util.YamlConfig;
import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

public class BungeeConfig extends CommonConfig {
	public boolean mRunReceivedCommands;
	public boolean mAutoRegisterServersToBungee;
	public boolean mAutoUnregisterInactiveServersFromBungee;

	public BungeeConfig(Logger logger, File configFile, Class<?> networkRelayClass, String resourcePath) {
		Map<String, Object> config = YamlConfig.loadWithFallback(logger, configFile, networkRelayClass, resourcePath);
		loadCommon(logger, config);

		mRunReceivedCommands = getBoolean(config, "auto-register-servers-to-bungee", false);
		logger.info("run-received-commands=" + mRunReceivedCommands);

		mAutoRegisterServersToBungee = getBoolean(config, "auto-register-servers-to-bungee", false);
		logger.info("auto-register-servers-to-bungee=" + mAutoRegisterServersToBungee);

		mAutoUnregisterInactiveServersFromBungee =
			getBoolean(config, "auto-unregister-inactive-servers-from-bungee", false);
		if (mAutoUnregisterInactiveServersFromBungee && !mAutoRegisterServersToBungee) {
			logger.warning("Config mismatch - auto-unregister-inactive-servers-from-bungee auto-register-servers-to-bungee=false");
			logger.warning("Setting auto-unregister-inactive-servers-from-bungee");
			mAutoUnregisterInactiveServersFromBungee = false;
		}
		logger.info("auto-unregister-inactive-servers-from-bungee=" + mAutoUnregisterInactiveServersFromBungee);
	}
}
