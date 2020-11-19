package com.playmonumenta.relay;

import java.util.LinkedHashMap;

import com.playmonumenta.relay.network.SocketManager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;

public class BroadcastCommand {
	static final String COMMAND = "broadcastcommand";

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.broadcastcommand");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("command", new GreedyStringArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(plugin, sender, (String)args[0]);
			})
			.register();
	}

	private static void run(Plugin plugin, CommandSender sender, String command) {
		/* Get the player's name, if any */
		String name = "";
		if (sender instanceof Player) {
			name = ((Player)sender).getName();
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender) sender).getCallee();
			if (callee instanceof Player) {
				name = ((Player)callee).getName();
			}
		}

		String commandStr = command;

		/* Replace all instances of @S with the player's name */
		commandStr = commandStr.replaceAll("@S", name);

		if (!(sender instanceof Player) || ((Player)sender).isOp()) {
			sender.sendMessage(ChatColor.GOLD + "Broadcasting command '" + commandStr + "' to all servers!");
		}

		try {
			SocketManager.broadcastCommand(plugin, commandStr);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Broadcasting command failed");
		}
	}
}
