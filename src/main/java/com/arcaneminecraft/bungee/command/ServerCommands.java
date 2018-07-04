package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ColorPalette;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.*;

public class ServerCommands {
    private final ArcaneBungee plugin;
    private final Map<String, ServerInfo> eventServers;
    private final Set<ProxiedPlayer> eventConnecting;

    public ServerCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.eventServers = new HashMap<>();
        this.eventConnecting = new HashSet<>();

        for (Map.Entry<String, ServerInfo> entry : plugin.getProxy().getServers().entrySet()) {
            if (entry.getKey().startsWith("Event-")) {
                this.eventServers.put(entry.getKey().substring(6), entry.getValue());
            }
        }

    }

    private void sendPlayer(ProxiedPlayer p, String serverInfoName) {
        ServerInfo server = plugin.getProxy().getServerInfo(serverInfoName);
        server.ping((ServerPing res, Throwable e) -> {
            if (e == null) {
                p.connect(server);
            } else {
                BaseComponent send = new TextComponent("Can't connect to " + serverInfoName + "server");
                send.setColor(ColorPalette.NEGATIVE);
                p.sendMessage(ChatMessageType.SYSTEM, send);
            }
        });
    }

    public class Creative extends Command {
        public Creative() {
            super(BungeeCommandUsage.CREATIVE.getName(), BungeeCommandUsage.CREATIVE.getPermission(), BungeeCommandUsage.CREATIVE.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.CREATIVE.getCommand(), args);
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }
            sendPlayer((ProxiedPlayer) sender, "Creative");
        }
    }

    public class Survival extends Command {
        public Survival() {
            super(BungeeCommandUsage.SURVIVAL.getName(), BungeeCommandUsage.SURVIVAL.getPermission(), BungeeCommandUsage.SURVIVAL.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.SURVIVAL.getCommand(), args);
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }
            sendPlayer((ProxiedPlayer) sender, "Survival");
        }
    }

    public class Event extends Command implements TabExecutor {
        public Event() {
            super(BungeeCommandUsage.EVENT.getName(), BungeeCommandUsage.EVENT.getPermission(), BungeeCommandUsage.EVENT.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.EVENT.getCommand(), args);
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }

            ProxiedPlayer p = (ProxiedPlayer) sender;

            if (args.length == 0) {
                eventConnecting.add(p);
                // Connect to an event server that responds first
                for (Map.Entry<String, ServerInfo> entry : eventServers.entrySet())
                    entry.getValue().ping((ServerPing res, Throwable e) -> {
                        if (eventConnecting.remove(p) && e == null) {
                            p.connect(entry.getValue());

                            BaseComponent send = new TextComponent("Connected to event " + entry.getKey());
                            send.setColor(ColorPalette.CONTENT);

                            p.sendMessage(ChatMessageType.SYSTEM, send);
                        }
                    });
                return;
            }

            String name = args[0].toLowerCase();
            for (String candidate : eventServers.keySet()) {
                if (candidate.toLowerCase().equals(name)) {
                    sendPlayer(p, "Event-" + candidate);
                    return;
                }
            }

            BaseComponent send = new TextComponent("Event server '" + args[0] + "' cannot be found");
            send.setColor(ChatColor.RED);

            p.sendMessage(ChatMessageType.SYSTEM, send);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1)
                return plugin.getTabCompletePreset().argStartsWith(args, eventServers.keySet());
            return Collections.emptyList();
        }
    }
}
