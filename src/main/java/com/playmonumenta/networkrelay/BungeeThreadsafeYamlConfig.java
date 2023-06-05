/* This file is based on
 * https://github.com/SpigotMC/BungeeCord/blob/master/proxy/src/main/java/net/md_5/bungee/conf/YamlConfig.java
 *
 * It was copied with some modifications to make server list modification threadsafe.
 *
 * It retains its original license, as follows:

Copyright (c) 2012, md_5. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

The name of the author may not be used to endorse or promote products derived
from this software without specific prior written permission.

You may not use the software for commercial software hosting services without
written permission from the author.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package com.playmonumenta.networkrelay;

import com.google.common.base.Charsets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class BungeeThreadsafeYamlConfig implements ConfigurationAdapter {
	private static @Nullable BungeeThreadsafeYamlConfig INSTANCE = null;

	/**
	 * The default tab list options available for picking.
	 */
	private enum DefaultTabList {
		GLOBAL(), GLOBAL_PING(), SERVER();
	}

	private final Yaml yaml;
	private Map<String, Object> config = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
	private final File file = new File("config.yml");
	private final ConcurrentSkipListMap<String, ServerInfo> servers = new ConcurrentSkipListMap<>();

	public BungeeThreadsafeYamlConfig() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		yaml = new Yaml(options);
	}

	protected BungeeThreadsafeYamlConfig getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("Tried to get BungeeThreadsafeYamlConfig instance before it was registered");
		}
		return INSTANCE;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void load() {
		try {
			file.createNewFile();

			Map<String, Object> rawConfig;
			try (InputStream is = new FileInputStream(file)) {
				try {
					rawConfig = (Map) yaml.load(is);
				} catch (YAMLException ex) {
					throw new RuntimeException("Invalid configuration encountered - this is a configuration error and NOT a bug! Please attempt to fix the error or see https://www.spigotmc.org/ for help.", ex);
				}
			}

			config = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
			if (rawConfig != null) {
				config.putAll(rawConfig);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Could not load configuration!", ex);
		}

		Map<String, Object> permissions = get("permissions", null);
		if (permissions == null) {
			set("permissions.default", Arrays.asList(new String[] {
				"bungeecord.command.server", "bungeecord.command.list"
			}));
			set("permissions.admin", Arrays.asList(new String[] {
				"bungeecord.command.alert", "bungeecord.command.end", "bungeecord.command.ip", "bungeecord.command.reload", "bungeecord.command.kick"
			}));
		}

		Map<String, Object> groups = get("groups", null);
		if (groups == null) {
			set("groups.md_5", Collections.singletonList("admin"));
		}

		/*
		 * Cache servers after loading config into a separate structure that,
		 * if modified, **will not** be saved back to the config file
		 */
		Map<String, Map<String, Object>> base = get("servers", (Map) Collections.singletonMap("lobby", new ConcurrentSkipListMap<>()));
		if (base == null) {
			throw new RuntimeException("Config missing 'servers'");
		}
		for (Map.Entry<String, Map<String, Object>> entry : base.entrySet()) {
			Map<String, Object> val = entry.getValue();
			String name = entry.getKey();
			String addr = get("address", "localhost:25565", val);
			String motd = ChatColor.translateAlternateColorCodes('&', get("motd", "&1Just another BungeeCord - Forced Host", val));
			boolean restricted = get("restricted", false, val);
			SocketAddress address = Util.getAddr(addr);
			ServerInfo info = ProxyServer.getInstance().constructServerInfo(name, address, motd, restricted);
			servers.put(name, info);
		}
	}

	@Contract("_, !null -> !null")
	private <T> @Nullable T get(String path, @Nullable T def) {
		return get(path, def, config);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Contract("_, !null, _ -> !null")
	private <T> @Nullable T get(String path, @Nullable T def, Map submap) {
		int index = path.indexOf('.');
		if (index == -1) {
			Object val = submap.get(path);
			if (val == null && def != null) {
				val = def;
				submap.put(path, def);
				save();
			}
			return (T) val;
		} else {
			String first = path.substring(0, index);
			String second = path.substring(index + 1, path.length());
			Map sub = (Map) submap.get(first);
			if (sub == null) {
				sub = new ConcurrentSkipListMap<>();
				submap.put(first, sub);
			}
			return get(second, def, sub);
		}
	}

	private void set(String path, @Nullable Object val) {
		set(path, val, config);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void set(String path, @Nullable Object val, Map submap) {
		int index = path.indexOf('.');
		if (index == -1) {
			if (val == null) {
				submap.remove(path);
			} else {
				submap.put(path, val);
			}
			save();
		} else {
			String first = path.substring(0, index);
			String second = path.substring(index + 1, path.length());
			Map sub = (Map) submap.get(first);
			if (sub == null) {
				sub = new ConcurrentSkipListMap<>();
				submap.put(first, sub);
			}
			set(second, val, sub);
		}
	}

	private void save() {
		try {
			try (Writer wr = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
				yaml.dump(config, wr);
			}
		} catch (IOException ex) {
			ProxyServer.getInstance().getLogger().log(Level.WARNING, "Could not save config", ex);
		}
	}

	@Override
	public int getInt(String path, int def) {
		return get(path, def);
	}

	@Override
	public String getString(String path, String def) {
		return get(path, def);
	}

	@Override
	public boolean getBoolean(String path, boolean def) {
		return get(path, def);
	}

	@Override
	public Map<String, ServerInfo> getServers() {

		return servers;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Collection<ListenerInfo> getListeners() {
		Collection<Map<String, Object>> base = get("listeners", (Collection) Arrays.asList(new Map[] {
			new ConcurrentSkipListMap()
		}));
		Map<String, String> forcedDef = new ConcurrentSkipListMap<>();
		forcedDef.put("pvp.md-5.net", "pvp");

		Collection<ListenerInfo> ret = new HashSet<>();

		for (Map<String, Object> val : base) {
			String motd = get("motd", "&1Another Bungee server", val);
			motd = ChatColor.translateAlternateColorCodes('&', motd);

			int maxPlayers = get("max_players", 1, val);
			boolean forceDefault = get("force_default_server", false, val);
			String host = get("host", "0.0.0.0:25577", val);
			int tabListSize = get("tab_size", 60, val);
			SocketAddress address = Util.getAddr(host);
			Map<String, String> forced = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
			forced.putAll(get("forced_hosts", forcedDef, val));
			String tabListName = get("tab_list", "GLOBAL_PING", val);
			DefaultTabList value = DefaultTabList.valueOf(tabListName.toUpperCase(Locale.ROOT));
			if (value == null) {
				value = DefaultTabList.GLOBAL_PING;
			}
			boolean setLocalAddress = get("bind_local_address", true, val);
			boolean pingPassthrough = get("ping_passthrough", false, val);

			boolean query = get("query_enabled", false, val);
			int queryPort = get("query_port", 25577, val);

			boolean proxyProtocol = get("proxy_protocol", false, val);
			List<String> serverPriority = new ArrayList<>(get("priorities", Collections.EMPTY_LIST, val));

			// Default server list migration
			// TODO: Remove from submap
			String defaultServer = get("default_server", null, val);
			String fallbackServer = get("fallback_server", null, val);
			if (defaultServer != null) {
				serverPriority.add(defaultServer);
				set("default_server", null, val);
			}
			if (fallbackServer != null) {
				serverPriority.add(fallbackServer);
				set("fallback_server", null, val);
			}

			// Add defaults if required
			if (serverPriority.isEmpty()) {
				serverPriority.add("lobby");
			}
			set("priorities", serverPriority, val);

			ListenerInfo info = new ListenerInfo(address, motd, maxPlayers, tabListSize, serverPriority, forceDefault, forced, value.toString(), setLocalAddress, pingPassthrough, queryPort, query, proxyProtocol);
			ret.add(info);
		}

		return ret;
	}

	@Override
	public Collection<String> getGroups(String player) {
		// #1270: Do this to support player names with .
		Map<String, Collection<String>> raw = get("groups", Collections.emptyMap());
		Collection<String> groups = raw.get(player);

		Collection<String> ret = (groups == null) ? new HashSet<String>() : new HashSet<>(groups);
		ret.add("default");
		return ret;
	}

	@Override
	public Collection<?> getList(String path, Collection<?> def) {
		return get(path, def);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<String> getPermissions(String group) {
		Collection<String> permissions = get("permissions." + group, null);
		return (permissions == null) ? Collections.EMPTY_SET : permissions;
	}
}
