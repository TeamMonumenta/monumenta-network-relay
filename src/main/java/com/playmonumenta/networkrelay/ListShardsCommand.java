package com.playmonumenta.networkrelay;

import java.util.TreeSet;

import org.bukkit.ChatColor;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class ListShardsCommand {
	protected static void register() {
		CommandAPICommand innerCommand = new CommandAPICommand("listShards")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.listshards"))
			.executes((sender, args) -> {
				try {
					TreeSet<String> shardNames = new TreeSet<>(NetworkRelayAPI.getOnlineShardNames());
					sender.sendMessage(ChatColor.GOLD + "Online shards: " + shardNames.toString());
				} catch (Exception e) {
					sender.sendMessage(ChatColor.RED + "An error occured, cannot check online shards.");
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
