package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.ColorPalette;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.BungeeCommandUsage;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class SimpleCommands {
    private final ArcaneBungee plugin;

    public SimpleCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Slap());
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Ping());
    }

    public class Slap extends Command implements TabExecutor {

        public Slap() {
            super("slap", BungeeCommandUsage.SLAP.getPermission());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().log(sender, "/slap", args);

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


    public class Ping extends Command implements TabExecutor {

        public Ping() {
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
            // TODO: Use TextComponent and ArcaneText.playerComponentBungee()
            StringBuilder m;

            m = new StringBuilder("Pong! ");
            if (p == sender)
                m.append("Your");
            else
                m = new StringBuilder(p.getName()).append("'s");
            m.append(" ping: ");

            sender.sendMessage(new TextComponent(m.append(ColorPalette.FOCUS).append(
                    p.getPing()
            ).append("ms").toString()));
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length > 1)
                return ImmutableSet.of();
            return TabCompletePreset.onlinePlayers(args);
        }
    }
}
