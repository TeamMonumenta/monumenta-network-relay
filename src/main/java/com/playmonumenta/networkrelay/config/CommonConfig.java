package com.playmonumenta.networkrelay.config;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CommonConfig {
	public Level mLogLevel = Level.INFO;
	public String mShardName;
	public String mRabbitUri;
	public int mHeartbeatInterval;
	public int mDestinationTimeout;
	public long mDefaultTtl;

	protected void loadCommon(Logger logger, Map<String, Object> config) {
		Level logLevel = null;
		String logLevelStr = getString(config, "log-level", "INFO");
		try {
			logLevel = Level.parse(logLevelStr);
			logger.setLevel(logLevel);
			logger.info("log-level=" + logLevelStr);
		} catch (Exception unused) {
			logger.warning("log-level=" + logLevelStr + " is invalid - defaulting to INFO");
		}
		if (logLevel == null) {
			logLevel = Level.INFO;
		}
		mLogLevel = logLevel;

		/* Shard name defaults to environment variable NETWORK_RELAY_NAME if present */
		String shardName = System.getenv("NETWORK_RELAY_NAME");
		if (shardName == null || shardName.isEmpty()) {
			shardName = "default-shard";
		}
		shardName = getString(config, "shard-name", shardName);
		mShardName = shardName;
		if (mShardName.equals("default-shard")) {
			logger.warning("shard-name is default value 'default-shard' which should be changed!");
		} else {
			logger.info("shard-name=" + mShardName);
		}

		mRabbitUri = getString(config, "rabbitmq-uri", "amqp://guest:guest@127.0.0.1:5672");
		if (mRabbitUri.equals("amqp://guest:guest@127.0.0.1:5672")) {
			logger.info("rabbitmq-uri=<default>");
		} else {
			logger.info("rabbitmq-uri=<set>");
		}

		mHeartbeatInterval = getInt(config, "heartbeat-interval", 1);
		if (mHeartbeatInterval <= 0) {
			logger.warning("heartbeat-interval is <= 0 which is invalid! Using default of 1.");
			mHeartbeatInterval = 1;
		} else {
			logger.info("heartbeat-interval=" + mHeartbeatInterval);
		}

		mDestinationTimeout = getInt(config, "destination-timeout", 5);
		if (mDestinationTimeout <= 1) {
			logger.warning("destination-timeout is <= 1 which is invalid! Using default of 5.");
			mDestinationTimeout = 5;
		} else {
			logger.info("destination-timeout=" + mDestinationTimeout);
		}

		mDefaultTtl = getLong(config, "default-time-to-live", 604800);
		if (mDefaultTtl < 0) {
			logger.warning("default-time-to-live is < 0 which is invalid! Using default of 604800.");
			mDefaultTtl = 604800;
		} else {
			logger.info("default-time-to-live=" + mDefaultTtl);
		}
	}

	public String getString(Map<String, Object> config, String key, String fallback) {
		return getString(config, key, fallback, false);
	}

	public String getString(Map<String, Object> config, String key, String fallback, boolean allowEmpty) {
		Object o = config.get(key);
		if (!(o instanceof String)) {
			return fallback;
		}
		String result = (String) o;
		if (!allowEmpty && result.isEmpty()) {
			return fallback;
		}
		return result;
	}

	public boolean getBoolean(Map<String, Object> config, String key, boolean fallback) {
		Object o = config.get(key);
		if (!(o instanceof Boolean)) {
			return fallback;
		}
		return (boolean) o;
	}

	public int getInt(Map<String, Object> config, String key, int fallback) {
		Object o = config.get(key);
		if (!(o instanceof Integer)) {
			return fallback;
		}
		return (int) o;
	}

	public long getLong(Map<String, Object> config, String key, long fallback) {
		Object o = config.get(key);
		if (!(o instanceof Long)) {
			return fallback;
		}
		return (long) o;
	}
}
