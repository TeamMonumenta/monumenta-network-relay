package com.playmonumenta.relay;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class RelayReloadCommand {
	public static void register(MonumentaRelay plugin) {
		new CommandAPICommand("relayreload")
			.withPermission(CommandPermission.fromString("monumenta.command.relayreload"))
			.executes((sender, args) -> {
				run(plugin, sender);
			})
			.register();
	}

	private static void run(MonumentaRelay plugin, CommandSender sender) throws WrapperCommandSyntaxException {
		plugin.reloadMonumentaConfig(sender);
	}
}

