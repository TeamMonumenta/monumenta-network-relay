package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Player;

public class WhereIsCommand {
	public static void register() {
		new CommandAPICommand("whereis")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.whereis"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				// TODO: REMOVE THIS COMMAND, THIS IS FOR DEBUGGING DEV/MOD only - usb
				RemotePlayerData data = RemotePlayerAPI.getRemotePlayer(player.getName());
				if (data == null) {
					throw CommandAPI.failWithString("No data found for: " + player.getName());
				}
				sender.sendPlainMessage(data.toString());
			})
			.register();
		new CommandAPICommand("whereis")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.whereis"))
			.executes((sender, args) -> {
				// TODO: REMOVE THIS COMMAND, THIS IS FOR DEBUGGING DEV/MOD only - usb
				RemotePlayerData data = RemotePlayerAPI.getRemotePlayer(sender.getName());
				if (data == null) {
					throw CommandAPI.failWithString("No data found for: " + sender.getName());
				}
				sender.sendPlainMessage(data.toString());
			})
			.register();
	}
}
