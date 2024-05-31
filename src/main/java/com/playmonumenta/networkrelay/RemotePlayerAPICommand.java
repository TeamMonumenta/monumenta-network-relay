package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.command.CommandSender;

public abstract class RemotePlayerAPICommand {
	public static final ArgumentSuggestions<CommandSender> VISIBLE_PLAYER_ONLINE_SUGGESTIONS = ArgumentSuggestions.strings((unused) -> NetworkRelayAPI.getVisiblePlayerNames().toArray(String[]::new));

	public static void register() {
		// we use all here for moderators
		Argument<String> playerArg = new StringArgument("player").replaceSuggestions(VISIBLE_PLAYER_ONLINE_SUGGESTIONS);

		// moderator command
		new CommandAPICommand("remoteplayerapi")
			.withPermission("monumenta.networkrelay.remoteplayerapi")
			.withSubcommand(new CommandAPICommand("get")
				.withArguments(playerArg)
				.executes((sender, args) -> {
					String playerName = args.getByArgument(playerArg);
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
