package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Collections;
import java.util.HashSet;

public class StaffChatCommands implements Listener {
    private final ArcaneBungee plugin;
    private final HashSet<ProxiedPlayer> toggled = new HashSet<>();

    public StaffChatCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
    }

    public class Chat extends Command implements TabExecutor {
        public Chat() {
            super(BungeeCommandUsage.STAFFCHAT.getName(), BungeeCommandUsage.STAFFCHAT.getPermission(), BungeeCommandUsage.STAFFCHAT.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            // Command logger inside this.broadcast()

            if (args.length == 0) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.STAFFCHAT.getUsage()));
                else
                    sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.STAFFCHAT.getUsage()));
                return;
            }
            broadcast(sender, String.join(" ", args));
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return TabCompletePreset.onlinePlayers(args);
        }
    }

    public class Toggle extends Command implements TabExecutor {

        public Toggle() {
            super(BungeeCommandUsage.STAFFCHATTOGGLE.getName(), BungeeCommandUsage.STAFFCHATTOGGLE.getPermission(), BungeeCommandUsage.STAFFCHATTOGGLE.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            // No real reason to log toggle command

            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }

            ProxiedPlayer p = (ProxiedPlayer) sender;

            BaseComponent send = new TextComponent("Staff chat toggle is ");
            send.setColor(ArcaneColor.CONTENT);

            if (toggled.add(p)){
                BaseComponent on = new TranslatableComponent("options.on");
                on.setColor(ArcaneColor.POSITIVE);
                send.addExtra(on);
            } else {
                toggled.remove(p);
                BaseComponent off = new TranslatableComponent("options.off");
                off.setColor(ArcaneColor.NEGATIVE);
                send.addExtra(off);
            }

            p.sendMessage(ChatMessageType.SYSTEM, send);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void checkToggle(ChatEvent e) {
        // if is a command and staff chat is not toggled
        //noinspection SuspiciousMethodCalls
        if (e.isCommand() || !toggled.contains(e.getSender()))
            return;

        broadcast((ProxiedPlayer) e.getSender(), e.getMessage());
        e.setCancelled(true);
    }

    private void broadcast(CommandSender sender, String msg) {
        plugin.logCommand(sender, BungeeCommandUsage.STAFFCHAT.getCommand() + " " + msg);

        BaseComponent send = new TextComponent("Staff // ");
        send.setColor(ArcaneColor.HEADING);

        BaseComponent name = ArcaneText.playerComponentBungee(sender);
        name.setColor(ArcaneColor.FOCUS);
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
