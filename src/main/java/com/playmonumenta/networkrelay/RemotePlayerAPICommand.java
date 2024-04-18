package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemotePlayerAPICommand {
	public static void register() {
		// base command
		new CommandAPICommand("remoteplayerapi")
		.withPermission("monumenta.networkrelay.remoteplayerapi")
			.withSubcommand(new CommandAPICommand("get")
			.withArguments(
				new EntitySelectorArgument.OnePlayer("player")
			)
			.executes((sender, args) -> {
				CommandSender player;
				if (args[0] != null) {
					player = (CommandSender) args[0];
				} else {
					player = sender;
				}
				// TODO: REMOVE THIS COMMAND, THIS IS FOR DEBUGGING DEV/MOD only - usb
				RemotePlayerData data = RemotePlayerAPI.getRemotePlayer(player.getName());
				if (data == null) {
					sender.sendMessage("No data found: " + player.getName());
					return;
				}
				sender.sendMessage(data.toString());
			})
		)
		.register();

	}
}
