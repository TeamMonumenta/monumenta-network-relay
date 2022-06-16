package com.playmonumenta.networkrelay;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class BroadcastCommand implements Listener {
	private static final List<NetworkRelayAPI.ServerType> ACCEPTED_SERVER_TYPES = Arrays.asList(
		NetworkRelayAPI.ServerType.ALL,
		NetworkRelayAPI.ServerType.MINECRAFT
	);

	private static boolean ENABLED = false;

	private final Logger mLogger;

	protected BroadcastCommand(Plugin plugin) {
		mLogger = plugin.getLogger();

		CommandAPICommand broadcastCommand = new CommandAPICommand("broadcastcommand")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.broadcastcommand"))
			.withArguments(new GreedyStringArgument("command"))
			.executes((sender, args) -> {
				run(plugin, sender, (String)args[0], NetworkRelayAPI.ServerType.ALL);
			});

		CommandAPICommand broadcastBungeeCommand = new CommandAPICommand("broadcastbungeecommand")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.broadcastbungeecommand"))
			.withArguments(new GreedyStringArgument("command"))
			.executes((sender, args) -> {
				run(plugin, sender, (String)args[0], NetworkRelayAPI.ServerType.BUNGEE);
			});

		CommandAPICommand broadcastMinecraftCommand = new CommandAPICommand("broadcastminecraftcommand")
			.withPermission(CommandPermission.fromString("monumenta.networkrelay.broadcastminecraftcommand"))
			.withArguments(new GreedyStringArgument("command"))
			.executes((sender, args) -> {
				run(plugin, sender, (String)args[0], NetworkRelayAPI.ServerType.MINECRAFT);
			});

		// Register first under the monumenta -> networkRelay namespace
		new CommandAPICommand("monumenta")
			.withSubcommand(new CommandAPICommand("networkRelay")
				.withSubcommand(broadcastCommand)
				.withSubcommand(broadcastBungeeCommand)
				.withSubcommand(broadcastMinecraftCommand)
			).register();

		// Then directly, for convenience
		broadcastCommand.register();
		broadcastBungeeCommand.register();
		broadcastMinecraftCommand.register();
	}

	private static void run(Plugin plugin, CommandSender sender, String command, NetworkRelayAPI.ServerType serverType) {
		if (!ENABLED) {
			sender.sendMessage("This command is not enabled");
		}

		/* Get the player's name, if any */
		String name = "";
		if (sender instanceof Player) {
			name = sender.getName();
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender) sender).getCallee();
			if (callee instanceof Player) {
				name = callee.getName();
			}
		}

		/* Replace all instances of @S with the player's name */
		command = command.replaceAll("@S", name);

		String typeStr;
		switch (serverType) {
			case BUNGEE:
				typeStr = "all bungee";
				break;
			case MINECRAFT:
				typeStr = "all minecraft";
				break;
			case ALL:
			default:
				typeStr = "all";
		}
		if (!(sender instanceof Player) || sender.isOp()) {
			sender.sendMessage(ChatColor.GOLD + "Broadcasting command '" + command + "' to " + typeStr + " servers");
		}
		plugin.getLogger().fine("Broadcasting command '" + command + "' to " + typeStr + "servers");

		try {
			NetworkRelayAPI.sendBroadcastCommand(command, serverType);
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

		JsonPrimitive serverTypeJson = data.getAsJsonPrimitive("server_type");
		if (serverTypeJson != null) {
			String serverTypeString = serverTypeJson.getAsString();
			if (serverTypeString != null) {
				NetworkRelayAPI.ServerType commandType;
				commandType = NetworkRelayAPI.ServerType.fromString(serverTypeString);
				if (!ACCEPTED_SERVER_TYPES.contains(commandType)) {
					return;
				}
			}
		}

		final String command = data.get("command").getAsString();
		mLogger.fine("Executing command'" + command + "' from source '" + event.getSource() + "'");

		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
	}
}
