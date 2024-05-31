package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.TreeSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ListShardsCommand {
	protected static void register() {
		CommandAPICommand innerCommand = new CommandAPICommand("listShards")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.listshards"))
			.executes((sender, args) -> {
				try {
					TreeSet<String> shardNames = new TreeSet<>(NetworkRelayAPI.getOnlineShardNames());
					sender.sendMessage(Component.text("Online shards: " + shardNames, NamedTextColor.GOLD));
				} catch (Exception e) {
					sender.sendMessage(Component.text("An error occurred, cannot check online shards.", NamedTextColor.RED));
				}
			});

		// Register first under the monumenta -> networkRelay namespace
		new CommandAPICommand("monumenta")
			.withSubcommand(new CommandAPICommand("networkRelay")
				.withSubcommand(innerCommand)
			).register();

		// Then directly, for convenience
		innerCommand.register();
	}
}
