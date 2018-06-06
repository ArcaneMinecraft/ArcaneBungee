package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.ColorPalette;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.HashSet;

public class StaffChat implements Listener {
    private final ArcaneBungee plugin;
    private static final String PERMISSION_NODE = Usage.STAFFCHAT.getPermission();
    private final HashSet<ProxiedPlayer> toggled = new HashSet<>();
    private final Command chat;
    private final Command toggle;

    StaffChat(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.chat = new Chat();
        this.toggle = new Toggle();
    }

    Command getChatListener() {
        return chat;
    }

    Command getToggleListener() {
        return toggle;
    }

    public class Chat extends Command {
        Chat() {
            super("a", Usage.STAFFCHAT.getPermission());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(Usage.STAFFCHAT.getUsage()));
                else sender.sendMessage(ArcaneText.usage(Usage.STAFFCHAT.getUsage()));
                return;
            }
            broadcast(sender, String.join(" ", args));
        }
    }

    public class Toggle extends Command {

        Toggle() {
            super("atoggle", Usage.STAFFCHAT.getPermission(), "at");
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
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void chat(ChatEvent e) {
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
        BaseComponent send = new TextComponent("Staff // ");
        send.setColor(ColorPalette.HEADING);

        BaseComponent name = ArcaneText.playerComponentBungee(sender);
        name.setColor(ColorPalette.FOCUS);
        name.addExtra(": ");

        send.addExtra(name);
        send.addExtra(ArcaneText.url(ChatColor.translateAlternateColorCodes('&', msg)));

        plugin.getProxy().getConsole().sendMessage(send);
        for (ProxiedPlayer recipient : plugin.getProxy().getPlayers()) {
            if (recipient.hasPermission(Usage.STAFFCHAT.getPermission())) {
                recipient.sendMessage(ChatMessageType.SYSTEM, send);
            }
        }
        if (sender instanceof ProxiedPlayer)
            plugin.logCommand((ProxiedPlayer) sender, "/a " + msg);
    }
}
