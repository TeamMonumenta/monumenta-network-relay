package com.playmonumenta.relay;

import java.io.IOException;

import com.playmonumenta.relay.network.HttpManager;
import com.playmonumenta.relay.network.SocketManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MonumentaRelay extends JavaPlugin {
	public SocketManager mSocketManager = null;
	private HttpManager mHttpManager = null;

	private static Plugin INSTANCE = null;

	public static Plugin getInstance() {
		return INSTANCE;
	}

	@Override
	public void onLoad() {
		BroadcastCommand.register(this);

		try {
			mHttpManager = new HttpManager(this);
		} catch (IOException err) {
			getLogger().warning("HTTP manager failed to start");
			err.printStackTrace();
		}
	}

	@Override
	public void onEnable() {
		INSTANCE = this;
		PluginManager manager = getServer().getPluginManager();

		mHttpManager.start();

		try {
			mSocketManager = new SocketManager(this);
		} catch (Exception ex) {
			getLogger().warning("Failed to instantiate socket manager: " + ex.getMessage());
			if (ex instanceof java.net.UnknownHostException) {
				getLogger().warning("This is expected if running a standalone server");
			} else {
				ex.printStackTrace();
			}
		}

		// Load info.
		reloadMonumentaConfig(null);

		manager.registerEvents(new RelayListener(this), this);
	}

	@Override
	public void onDisable() {
		INSTANCE = null;
		getServer().getScheduler().cancelTasks(this);

		mHttpManager.stop();
		if (mSocketManager != null) {
			mSocketManager.stop();
		}
	}

	/* Sender will be sent debugging info if non-null */
	public void reloadMonumentaConfig(CommandSender sender) {
		ServerProperties.load(this, sender);
	}
}
