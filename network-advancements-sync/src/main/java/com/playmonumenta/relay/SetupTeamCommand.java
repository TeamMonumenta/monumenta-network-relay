package com.playmonumenta.relay;

import java.util.HashMap;
import java.util.Map;

import com.playmonumenta.relay.AdvancementManager;
import com.playmonumenta.relay.utils.DataPackUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.TeamArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;

public class SetupTeamCommand {
	static final String COMMAND = "setupteam";

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.setupteam");

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(new TeamArgument("team").safeOverrideSuggestions(s ->
                Bukkit.getScoreboardManager().getMainScoreboard().getTeams().toArray(new Team[0])))
			.withArguments(new GreedyStringArgument("new display name, raw json text"))
			.executes((sender, args) -> {
				run(plugin, sender, (String)args[0], (String)args[1]);
			})
			.register();
	}

	private static void run(Plugin plugin, CommandSender sender, String teamId, String teamName) {
		Team team = DataPackUtils.getTeam(teamId);
		String teamColor = null;
		String teamDisplayName = null;
		String teamPrefix = null;
		String teamSuffix = null;
		if (team != null) {
			if (team.getColor() != null) {
				teamColor = team.getColor().name().toLowerCase();
			}
			teamDisplayName = team.getDisplayName();
			teamPrefix = team.getPrefix();
			teamSuffix = team.getSuffix();
		}
		if (teamColor == null) {
			teamColor = "reset";
		}
		if (teamDisplayName == null) {
			teamDisplayName = teamId;
		}
		if (teamPrefix == null) {
			teamPrefix = "";
		}
		if (teamSuffix == null) {
			teamSuffix = "";
		}

		Map<String, String> commandReplacements = new HashMap<String, String>();
		commandReplacements.put("__team_id__", teamId);
		commandReplacements.put("__team_color__", teamColor);
		commandReplacements.put("__team_display_name__", teamDisplayName);
		commandReplacements.put("__team_prefix__", teamPrefix);
		commandReplacements.put("__team_suffix__", teamSuffix);
		commandReplacements.put("__new_display_name__", teamName);

		DataPackUtils.runFunctionWithReplacements("rivals",
		                                          "setup_team",
		                                          true,
		                                          commandReplacements);

		AdvancementManager.getInstance().watchTeamId(teamId, teamName);
	}
}
