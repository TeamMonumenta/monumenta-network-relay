package com.playmonumenta.networkrelay;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

// TODO: rip this code out and replace with CommandAPI when migrating to Velocity
public class WhereIsCommandBungee extends Command {
	public WhereIsCommandBungee() {
		super("whereisbungee", "monumenta.networkrelay.whereis");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		RemotePlayerData data = null;
		if (sender instanceof ProxiedPlayer player) {
			boolean isSender = args.length == 0;
			if (isSender) {
				data = RemotePlayerAPI.getRemotePlayer(sender.getName());
			} else if (args.length == 1) {
				String name = args[0];
				data = RemotePlayerAPI.getRemotePlayer(name);
			}
			if (data != null && !(!isSender && data.isHidden())) {
				player.sendMessage(new TextComponent(data.toString()));
			} else {
				player.sendMessage(new TextComponent("No data found!"));
			}
		}
	}
}
