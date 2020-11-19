package com.playmonumenta.relay;

import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class RelayReload {
	public static void register(MonumentaRelay plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		new CommandAPICommand("relayreload")
			.withPermission(CommandPermission.fromString("monumenta.command.relayreload"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(plugin, sender);
			})
			.register();
	}

	private static void run(MonumentaRelay plugin, CommandSender sender) throws WrapperCommandSyntaxException {
		plugin.reloadMonumentaConfig(sender);
	}
}

