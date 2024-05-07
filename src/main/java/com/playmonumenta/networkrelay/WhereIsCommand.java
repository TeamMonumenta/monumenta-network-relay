package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;

public abstract class WhereIsCommand {
	public static void register() {
		new CommandAPICommand("whereis")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.whereis"))
			.withArguments(new StringArgument("player").replaceSuggestions(RemotePlayerAPICommand.VISIBLE_PLAYER_ONLINE_SUGGESTIONS))
			.executes((sender, args) -> {
				String playerName = (String) args[0];
				RemotePlayerData data = RemotePlayerAPI.getRemotePlayer(playerName);
				if (data == null || data.isHidden()) {
					throw CommandAPI.failWithString("No data found for: " + playerName);
				}
				sender.sendPlainMessage(data.toString());
			})
			.register();
		new CommandAPICommand("whereis")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.whereis"))
			.executes((sender, args) -> {
				RemotePlayerData data = RemotePlayerAPI.getRemotePlayer(sender.getName());
				if (data == null) {
					throw CommandAPI.failWithString("No data found for: " + sender.getName());
				}
				sender.sendPlainMessage(data.toString());
			})
			.register();
	}
}
