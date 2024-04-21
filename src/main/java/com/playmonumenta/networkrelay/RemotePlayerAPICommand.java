package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.command.CommandSender;

public class RemotePlayerAPICommand {
	public static void register() {
		// moderator command
		new CommandAPICommand("remoteplayerapi")
			.withPermission("monumenta.networkrelay.remoteplayerapi")
			.withSubcommand(new CommandAPICommand("get")
				.withArguments(
					// we use all here for moderators
					new StringArgument("player").replaceSuggestions(RemotePlayerAPI.ALL_PLAYER_ONLINE_SUGGESTIONS)
				)
				.executes((sender, args) -> {
					String playerName = (String) args[0];
					RemotePlayerData data = RemotePlayerAPI.getRemotePlayer(playerName);
					if (data == null) {
						sender.sendMessage("No data found: " + playerName);
						return;
					}
					sender.sendMessage(data.toString());
				})
			)
			.register();

	}
}
