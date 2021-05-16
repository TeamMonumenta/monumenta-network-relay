package com.playmonumenta.networkrelay;

import java.util.TreeSet;
import java.util.logging.Logger;

import com.google.gson.JsonObject;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class ListShardsCommand {
	protected ListShardsCommand() {
		new CommandAPICommand("listshards")
			.withPermission(CommandPermission.fromString("monumenta.command.listshards"))
			.executes((sender, args) -> {
				try {
					TreeSet<String> shardNames = new TreeSet<>(NetworkRelayAPI.getOnlineShardNames());
					sender.sendMessage(ChatColor.GOLD + "Online shards: " + shardNames.toString());
				} catch (Exception e) {
					sender.sendMessage(ChatColor.RED + "An error occured, cannot check online shards.");
				}
			})
			.register();
	}
}
