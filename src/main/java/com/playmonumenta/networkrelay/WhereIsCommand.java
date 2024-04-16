package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.logging.Level;
import org.bukkit.entity.Player;

public class WhereIsCommand {
	public static void register(NetworkRelay relayPlugin) {
		new CommandAPICommand("whereis")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.whereis"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				RemotePlayerData data = NetworkRelayAPI.getRemotePlayer(player.getUniqueId());

			})
		.register();
	}
}
