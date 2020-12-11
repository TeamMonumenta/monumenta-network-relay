package com.playmonumenta.relay.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
/*
ChatMessageType contains the following:
ACTION_BAR
CHAT
SYSTEM

https://ci.md-5.net/job/BungeeCord/ws/chat/target/apidocs/net/md_5/bungee/api/ChatMessageType.html
*/
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
// https://www.spigotmc.org/wiki/the-chat-component-api/
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class MessagingUtils {
	public static void sendStackTrace(CommandSender sender, Exception e) {
		TextComponent formattedMessage;
		String errorMessage = e.getLocalizedMessage();
		if (errorMessage != null) {
			formattedMessage = new TextComponent(TextComponent.fromLegacyText(errorMessage));
		} else {
			formattedMessage = new
			TextComponent("An error occured without a set message. Hover for stack trace.");
		}
		formattedMessage.setColor(ChatColor.RED);

		// Get the first 300 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString();
		sStackTrace = sStackTrace.substring(0, Math.min(sStackTrace.length(), 300));

		BaseComponent[] textStackTrace = new ComponentBuilder(sStackTrace.replace("\t",
		                                                      "  ")).color(ChatColor.RED).create();
		formattedMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, textStackTrace));
		sender.spigot().sendMessage(formattedMessage);

		e.printStackTrace();
	}
}
