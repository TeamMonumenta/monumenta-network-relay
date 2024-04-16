package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.logging.Level;

public class RemotePlayerCommand {
	public static void register() {
		new CommandAPICommand("remoteplayerinfo")
		.executes((sender, args) -> {
			// TODO: REMOVE THIS COMMAND, THIS IS FOR DEBUGGING DEV/MOD only - usb
			RemotePlayerData data = RemotePlayerAPI.getRemotePlayer(sender.getName());
			if (data == null) {
				sender.sendMessage("No data found :c");
				return;
			}
			sender.sendMessage(data.toString());
		})
		.register();
	}
}
