package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.apache.commons.collections4.map.LinkedMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerCommands {
    private final ArcaneBungee plugin;

    public ServerCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
    }

    private void sendPlayer(ProxiedPlayer p, ServerInfo server) {
        final boolean isEvent;
        final String name;
        if (server.getName().startsWith("Event-")) {
            name = server.getName().substring(6);
            isEvent = true;
        } else {
            name = server.getName();
            isEvent = false;
        }

        AtomicBoolean waiting = new AtomicBoolean(true);

        p.connect(server, (success, e) -> {
            waiting.set(false);
            if (success) {
                BaseComponent send = new TextComponent("Connected to the ");
                send.setColor(ArcaneColor.HEADING);

                BaseComponent n = new TextComponent(name);
                n.setColor(ArcaneColor.FOCUS);
                send.addExtra(n);

                send.addExtra(isEvent ? " event server" : " server");
                p.sendMessage(ChatMessageType.SYSTEM, send);
            }
        }, ServerConnectEvent.Reason.COMMAND);

        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (waiting.get()) {
                BaseComponent send = new TextComponent("Connecting to the ");
                send.setColor(ArcaneColor.CONTENT);

                BaseComponent n = new TextComponent(name);
                n.setColor(ArcaneColor.FOCUS);
                send.addExtra(n);

                send.addExtra((isEvent ? " event" : "") + " server...");
                p.sendMessage(ChatMessageType.SYSTEM, send);
            }
        }, 350, TimeUnit.MILLISECONDS);
    }

    public class Creative extends Command {
        public Creative() {
            super(BungeeCommandUsage.CREATIVE.getName(), BungeeCommandUsage.CREATIVE.getPermission(), BungeeCommandUsage.CREATIVE.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.CREATIVE.getCommand(), args);
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }
            sendPlayer((ProxiedPlayer) sender, plugin.getProxy().getServerInfo("Creative"));
        }
    }

    public class Survival extends Command {
        public Survival() {
            super(BungeeCommandUsage.SURVIVAL.getName(), BungeeCommandUsage.SURVIVAL.getPermission(), BungeeCommandUsage.SURVIVAL.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.SURVIVAL.getCommand(), args);
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }
            sendPlayer((ProxiedPlayer) sender, plugin.getProxy().getServerInfo("Survival"));
        }
    }

    public class Event extends Command implements TabExecutor {
        public Event() {
            super(BungeeCommandUsage.EVENT.getName(), BungeeCommandUsage.EVENT.getPermission(), BungeeCommandUsage.EVENT.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.EVENT.getCommand(), args);
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }

            ProxiedPlayer p = (ProxiedPlayer) sender;

            if (args.length == 0) {
                AtomicInteger n = new AtomicInteger(0);
                Map<String, Long> replied = new LinkedMap<>();
                AtomicBoolean waiting = new AtomicBoolean(true);

                final Runnable run = () -> {
                    // Make sure it goes through all event servers first
                    if (n.decrementAndGet() != 0)
                        return;

                    waiting.set(false);
                    int nReplied = replied.size();

                    if (nReplied == 0) {
                        BaseComponent send = new TextComponent("There are no active events at this time");
                        send.setColor(ArcaneColor.HEADING);
                        p.sendMessage(ChatMessageType.SYSTEM, send);
                        return;
                    }

                    BaseComponent send = new TextComponent("There " + (nReplied == 1 ? "is " : "are ") + nReplied + " active event" + (nReplied == 1 ? "" : "s") + ":");
                    send.setColor(ArcaneColor.HEADING);
                    p.sendMessage(ChatMessageType.SYSTEM, send);

                    for (Map.Entry<String, Long> e : replied.entrySet()) {
                        BaseComponent serv = new TextComponent(" - ");
                        serv.setColor(ArcaneColor.CONTENT);
                        BaseComponent name = new TextComponent(e.getKey());
                        name.setColor(ArcaneColor.FOCUS);
                        serv.addExtra(name);
                        serv.addExtra(" - ping: " + e.getValue() + "ms");
                        serv.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/event " + e.getKey()));

                        p.sendMessage(ChatMessageType.SYSTEM, serv);
                    }
                };

                // Query all event servers
                for (Map.Entry<String, ServerInfo> entry : plugin.getProxy().getServers().entrySet()) {
                    if (!entry.getKey().startsWith("Event-"))
                        continue;

                    n.getAndIncrement();
                    final String name = entry.getKey().substring(6);
                    final long pingStart = System.currentTimeMillis();

                    entry.getValue().ping((result, error) -> {
                        if (error == null)
                            replied.put(name, System.currentTimeMillis() - pingStart);
                        run.run();
                    });
                }

                plugin.getProxy().getScheduler().schedule(plugin, () -> {
                    if (waiting.get()) {
                        BaseComponent send = new TextComponent("Querying all the event servers...");
                        send.setColor(ArcaneColor.CONTENT);
                        p.sendMessage(ChatMessageType.SYSTEM, send);
                    }
                }, 350, TimeUnit.MILLISECONDS);

                return;
            }

            // If event server is specified
            String eventName = "event-" + args[0].toLowerCase();
            for (Map.Entry<String, ServerInfo> entry : plugin.getProxy().getServers().entrySet()) {
                if (entry.getKey().toLowerCase().equals(eventName)) {
                    sendPlayer(p, entry.getValue());
                    return;
                }
            }

            BaseComponent send = new TextComponent("Event server '" + args[0] + "' cannot be found");
            send.setColor(ChatColor.RED);

            p.sendMessage(ChatMessageType.SYSTEM, send);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                ArrayList<String> l = new ArrayList<>();
                for (Map.Entry<String, ServerInfo> entry : plugin.getProxy().getServers().entrySet()) {
                    if (!entry.getKey().startsWith("Event-"))
                        continue;
                    l.add(entry.getKey().substring(6));
                }
                return plugin.getTabCompletePreset().argStartsWith(args, l);
            }
            return Collections.emptyList();
        }
    }
}
