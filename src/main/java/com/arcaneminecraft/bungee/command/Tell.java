package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ColorPalette;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.HashMap;

public class Tell {
    private final ArcaneBungee plugin;
    private final HashMap<CommandSender, CommandSender> lastReceived = new HashMap<>();

    public Tell(ArcaneBungee plugin) {
        this.plugin = plugin;
     }

    public class Message extends Command implements TabExecutor {

        public Message() {
            super(BungeeCommandUsage.MSG.getName(), BungeeCommandUsage.MSG.getPermission(), BungeeCommandUsage.MSG.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.MSG.getCommand(), args);

            if (args.length < 2) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.MSG.getUsage()));
                else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.MSG.getUsage()));
                return;
            }

            // Get recipient
            CommandSender p = plugin.getProxy().getPlayer(args[0]);
            if (p == null) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(args[0]));
                else sender.sendMessage(ArcaneText.playerNotFound(args[0]));
                return;
            } else if (p == sender) {
                BaseComponent send = new TranslatableComponent("commands.message.sameTarget"); // TODO: Latest 1.13 snapshot allows self-messaging
                send.setColor(ChatColor.RED);
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, send);
                else sender.sendMessage(send);
                return;
            }

            messenger(sender, p, args, 1);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return plugin.getTabCompletePreset().onlinePlayers(args);
        }
    }

    public class Reply extends Command implements TabExecutor {

        public Reply() {
            super(BungeeCommandUsage.REPLY.getName(), BungeeCommandUsage.REPLY.getPermission(), BungeeCommandUsage.REPLY.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.REPLY.getCommand(), args);

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.REPLY.getUsage()));
                else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.REPLY.getUsage()));
                return;
            }

            CommandSender p = lastReceived.get(sender);
            if (p == null) {
                plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.REPLY.getCommand(), args);

                BaseComponent send = new TextComponent("There is nobody to reply to");
                send.setColor(ChatColor.RED);

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, send);
                else sender.sendMessage(send);
                return;
            }

            if (p instanceof ProxiedPlayer) {
                // Log with /msg instead for easier readability.
                plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.MSG.getCommand() + " " + p.getName() + " " + String.join(" ", args));

                if (!((ProxiedPlayer) p).isConnected()) {
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(p.getName()));
                    else
                        sender.sendMessage(ArcaneText.playerNotFound(p.getName()));
                    return;
                }
            } else {
                plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.REPLY.getCommand(), args);
            }

            messenger(sender, p, args, 0);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return plugin.getTabCompletePreset().onlinePlayers(args);
        }
    }

    private void messenger(CommandSender from, CommandSender to, String[] args, int fromIndex) {
        BaseComponent msg = ArcaneText.url(args, fromIndex);

        msg.setColor(ColorPalette.CONTENT);
        msg.setItalic(true);

        messageSender(from, to, msg, false); // send to "sender" as "To p: msg"
        messageSender(to, from, msg, true);

        // Update sender-receiver map
        lastReceived.put(to, from);
    }

    // TODO: Should we make messaging using vanilla translatable?
    private void messageSender(CommandSender player, CommandSender name, BaseComponent msg, boolean isReceiving) {
        TextComponent send = new TextComponent();

        // Beginning
        TextComponent header = new TextComponent();
        header.setColor(ColorPalette.HEADING);

        TextComponent in = new TextComponent("> ");
        in.setColor(ChatColor.DARK_GRAY);
        header.addExtra(in);

        header.addExtra((isReceiving ? "From" : "To") + " ");

        // Add a click action
        BaseComponent receiving = ArcaneText.playerComponentBungee(name);
        if (!(name instanceof ProxiedPlayer))
            receiving.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, BungeeCommandUsage.REPLY.getCommand() + " "));
        header.addExtra(receiving);

        TextComponent out = new TextComponent(": ");
        out.setColor(ChatColor.DARK_GRAY);
        header.addExtra(out);

        send.addExtra(header);

        // Message
        send.addExtra(msg);

        // Send Messages
        if (player instanceof ProxiedPlayer)
            ((ProxiedPlayer) player).sendMessage(ChatMessageType.SYSTEM, send);
        else
            player.sendMessage(send);
    }
}
