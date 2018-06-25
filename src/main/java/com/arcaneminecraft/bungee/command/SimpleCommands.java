package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ColorPalette;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Commands in this class:
 * apply, links, list, me, news, ping, say, slap
 */
public class SimpleCommands {
    private final ArcaneBungee plugin;

    public SimpleCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Apply());// TODO
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Links()); // TODO
        plugin.getProxy().getPluginManager().registerCommand(plugin, new ListPlayers());
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Me());
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Ping());
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Slap());
    }

    public class Apply extends Command implements TabExecutor {

        public Apply() {
            super(BungeeCommandUsage.APPLY.getName(), BungeeCommandUsage.APPLY.getPermission(), BungeeCommandUsage.APPLY.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.APPLY.getCommand(), args);

            // TODO
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return ImmutableSet.of();
        }
    }

    public class Links extends Command implements TabExecutor {
        private final List<String> candidates;
        // unfortunately gotta show all the links

        public Links() {
            super(BungeeCommandUsage.LINKS.getName(), BungeeCommandUsage.LINKS.getPermission(), BungeeCommandUsage.LINKS.getAliases());
            candidates = new ArrayList<>();
            candidates.add("website");
            candidates.add("forums");
            candidates.add("discord");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.LINKS.getCommand(), args);

            // TODO
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length > 1) {
                return ImmutableSet.of();
            } else if (args.length == 0) {
                return candidates;
            } else {
                List<String> ret = new ArrayList<>();
                for (String s : candidates)
                    if (s.startsWith(args[0]))
                        ret.add(s);
                return ret;
            }
        }
    }

    public class ListPlayers extends Command implements TabExecutor {

        ListPlayers() {
            super(BungeeCommandUsage.LIST.getName(), BungeeCommandUsage.LIST.getPermission(), BungeeCommandUsage.LIST.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.LIST.getCommand(), args);

            Collection<ProxiedPlayer> cp = plugin.getProxy().getPlayers();
            BaseComponent head = new TranslatableComponent("commands.players.list",
                    String.valueOf(plugin.getProxy().getOnlineCount()),
                    String.valueOf(plugin.getProxy().getConfig().getPlayerLimit()));

            BaseComponent body = new TextComponent();
            Iterator<ProxiedPlayer> i = plugin.getProxy().getPlayers().iterator();
            if (i.hasNext()) {
                body.addExtra(ArcaneText.playerComponentBungee(i.next()));

                i.forEachRemaining((ProxiedPlayer p) -> {
                    body.addExtra(", ");
                    body.addExtra(ArcaneText.playerComponentBungee(p));
                });
            }

            if (sender instanceof ProxiedPlayer) {
                ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, head);
                ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, body);
            } else {
                sender.sendMessage(head);
                sender.sendMessage(body);
            }

        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return null;
        }
    }

    public class Me extends Command {

        Me() {
            super(BungeeCommandUsage.ME.getName(), BungeeCommandUsage.ME.getPermission(), BungeeCommandUsage.ME.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.ME.getCommand(), args);

            if (args.length == 0) {
                ArcaneText.usage(BungeeCommandUsage.ME.getUsage());
                return;
            }

            BaseComponent ret = new TranslatableComponent("chat.type.emote", ArcaneText.playerComponentBungee(sender), String.join(" ", args));

            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                p.sendMessage(ChatMessageType.SYSTEM, ret);
            }
        }
    }

    public class Ping extends Command implements TabExecutor {

        Ping() {
            super("ping");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(new TextComponent("Your ping will forever be <1ms."));
                return;
            }

            ProxiedPlayer p;

            if (args.length == 0) {
                p = (ProxiedPlayer) sender;
            } else {
                p = plugin.getProxy().getPlayer(args[0]);
                if (p == null) {
                    sender.sendMessage(ArcaneText.playerNotFound(args[0]));
                    return;
                }
            }

            BaseComponent m = new TextComponent("Pong! ");
            if (p == sender) {
                m.addExtra("Your");
            } else {
                m.addExtra(ArcaneText.playerComponentBungee(p));
                m.addExtra("'s");
            }
            m.addExtra(" ping: ");

            BaseComponent n = new TextComponent(p.getPing() + " ms");
            n.setColor(ColorPalette.FOCUS);

            m.addExtra(n);

            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, m);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length > 1)
                return ImmutableSet.of();
            return TabCompletePreset.onlinePlayers(args);
        }
    }

    public class Slap extends Command implements TabExecutor {

        Slap() {
            super("slap", BungeeCommandUsage.SLAP.getPermission());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, "/slap", args);

            if (args.length == 0) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.SLAP.getUsage()));
                else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.SLAP.getUsage()));
                return;
            }

            ProxiedPlayer victim = plugin.getProxy().getPlayer(args[0]);

            if (victim == null) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(args[0]));
                else sender.sendMessage(ArcaneText.playerNotFound(args[0]));
                return;
            }

            BaseComponent send = new TextComponent(ArcaneText.playerComponentBungee(sender));
            send.addExtra(" slapped ");
            send.addExtra(ArcaneText.playerComponentBungee(victim));
            send.addExtra(" in the face!");

            send.setColor(ColorPalette.META);

            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                p.sendMessage(ChatMessageType.SYSTEM, send);
            }
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return TabCompletePreset.onlinePlayers(args);
        }
    }
}
