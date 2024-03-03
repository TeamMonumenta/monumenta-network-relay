package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.logging.Level;

public class RemotePlayerCommand {
	public static void register() {
		new CommandAPICommand("remoteplayerinfo")
		.executes((sender, args) -> {
			sender.sendMessage(RemotePlayerAPI.getRemotePlayer(sender.getName()).toString());
		})
		.register();
	}
}
