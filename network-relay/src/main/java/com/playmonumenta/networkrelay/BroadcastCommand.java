package com.playmonumenta.networkrelay;

import java.util.logging.Logger;

import com.google.gson.JsonObject;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.GreedyStringArgument;

public class BroadcastCommand implements Listener {
	public static boolean ENABLED = false;

	private final Logger mLogger;

	protected BroadcastCommand(Plugin plugin) {
		mLogger = plugin.getLogger();

		new CommandAPICommand("broadcastcommand")
			.withPermission(CommandPermission.fromString("monumenta.command.broadcastcommand"))
			.withArguments(new GreedyStringArgument("command"))
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

		/* Replace all instances of @S with the player's name */
		command = command.replaceAll("@S", name);

		if (!(sender instanceof Player) || ((Player)sender).isOp()) {
			sender.sendMessage(ChatColor.GOLD + "Broadcasting command '" + command + "' to all servers");
		}
		plugin.getLogger().fine("Broadcasting command '" + command + "' to all servers");

		try {

			NetworkRelayAPI.sendBroadcastCommand(command);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Broadcasting command failed");
		}
	}

	protected static void setEnabled(boolean enabled) {
		ENABLED = enabled;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void networkRelayMessageEvent(NetworkRelayMessageEvent event) {
		if (!event.getChannel().equals(NetworkRelayAPI.COMMAND_CHANNEL)) {
			return;
		}

		JsonObject data = event.getData();
		if (!data.has("command") ||
		    !data.get("command").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("command").isString()) {
			mLogger.warning("Got invalid command message with no actual command");
			return;
		}

		final String command = data.get("command").getAsString();
		mLogger.fine("Executing command'" + command + "' from source '" + event.getSource() + "'");

		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
	}
}
