package com.playmonumenta.networkrelay;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.TreeSet;
import org.bukkit.ChatColor;

public class ListShardsCommand {
	protected static void register() {
		CommandAPICommand innerCommand = new CommandAPICommand("listShards")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.listshards"))
			.executes((sender, args) -> {
				try {
					TreeSet<String> shardNames = new TreeSet<>(NetworkRelayAPI.getOnlineShardNames());
					sender.sendMessage(ChatColor.GOLD + "Online shards: " + shardNames);
				} catch (Exception e) {
					sender.sendMessage(ChatColor.RED + "An error occurred, cannot check online shards.");
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
