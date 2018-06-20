package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.ColorPalette;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.BungeeCommandUsage;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.HashSet;

public class StaffChat implements Listener {
    private final ArcaneBungee plugin;
    private final HashSet<ProxiedPlayer> toggled = new HashSet<>();

    public StaffChat(ArcaneBungee plugin) {
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Chat());
        plugin.getProxy().getPluginManager().registerCommand(plugin, new Toggle());
    }

    public class Chat extends Command implements TabExecutor{
        Chat() {
            super("a", BungeeCommandUsage.STAFFCHAT.getPermission());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.STAFFCHAT.getUsage()));
                else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.STAFFCHAT.getUsage()));
                return;
            }
            broadcast(sender, String.join(" ", args));
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            // attempt to stop "you do not have permission" msg when tab-completing "/a"
            return TabCompletePreset.onlinePlayers(args);
        }
    }

    public class Toggle extends Command implements TabExecutor {

        Toggle() {
            super("atoggle", BungeeCommandUsage.STAFFCHATTOGGLE.getPermission(), "at");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }

            ProxiedPlayer p = (ProxiedPlayer) sender;

            BaseComponent send = new TextComponent("Staff chat has been toggled ");
            send.setColor(ColorPalette.CONTENT);

            if (toggled.add(p)) {
                BaseComponent on = new TextComponent("on");
                on.setColor(ColorPalette.POSITIVE);
                send.addExtra(on);
            } else {
                toggled.remove(p);
                BaseComponent off = new TextComponent("off");
                off.setColor(ColorPalette.NEGATIVE);
                send.addExtra(off);
            }

            send.addExtra(".");

            p.sendMessage(ChatMessageType.SYSTEM, send);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return ImmutableSet.of();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void checkToggle(ChatEvent e) {
        //noinspection SuspiciousMethodCalls
        if (!(e.getSender() instanceof ProxiedPlayer) // Must be sent by player
                || !e.isCommand() // Don't hog onto commands
                || e.getMessage().startsWith("/g ")
                || e.getMessage().startsWith("/global ") // escapes capture
                || !toggled.contains(e.getSender()) // has toggle on
                ) return;
        broadcast((ProxiedPlayer) e.getSender(), e.getMessage());
        e.setCancelled(true);
    }

    private void broadcast(CommandSender sender, String msg) {
        plugin.getCommandLogger().log(sender, "/a" + msg);

        BaseComponent send = new TextComponent("Staff // ");
        send.setColor(ColorPalette.HEADING);

        BaseComponent name = ArcaneText.playerComponentBungee(sender);
        name.setColor(ColorPalette.FOCUS);
        name.addExtra(": ");

        send.addExtra(name);
        send.addExtra(ArcaneText.url(ChatColor.translateAlternateColorCodes('&', msg)));

        plugin.getProxy().getConsole().sendMessage(send);
        for (ProxiedPlayer recipient : plugin.getProxy().getPlayers()) {
            if (recipient.hasPermission(BungeeCommandUsage.STAFFCHAT.getPermission())) {
                recipient.sendMessage(ChatMessageType.SYSTEM, send);
            }
        }
    }
}
