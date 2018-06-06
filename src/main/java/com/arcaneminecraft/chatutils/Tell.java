package com.arcaneminecraft.chatutils;

import java.util.HashMap;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.ColorPalette;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Tell {
    private static final String TAG = "PM";
    private final ArcaneChatUtils plugin;
    private final HashMap<CommandSender, CommandSender> lastReceived = new HashMap<>();
    private final Command message;
    private final Command reply;

    Tell(ArcaneChatUtils plugin) {
        this.plugin = plugin;
        this.message = new Message();
        this.reply = new Reply();
    }

    Command getMessage() {
        return message;
    }

    Command getReply() {
        return reply;
    }

    public class Message extends Command {

        Message() {
            super("tell", null, "w", "msg", "m", "t");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usageTranslatable("commands.message.usage"));
                else sender.sendMessage(ArcaneText.usageTranslatable("commands.message.usage"));
                return;
            }

            // Get recipient
            CommandSender p = plugin.getProxy().getPlayer(args[0]);
            if (p == null) {
                BaseComponent send = new TranslatableComponent("commands.generic.player.notFound", args[0]);
                send.setColor(ChatColor.RED);
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, send);
                else sender.sendMessage(send);
                return;
            } else if (p == sender) {
                BaseComponent send = new TranslatableComponent("commands.message.sameTarget");
                send.setColor(ChatColor.RED);
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, send);
                else sender.sendMessage(send);
                return;
            }

            messenger(sender, p, args, 1);
        }
    }

    public class Reply extends Command {

        Reply() {
            super("reply", null, "r");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(Usage.REPLY.getUsage()));
                else sender.sendMessage(ArcaneText.usage(Usage.REPLY.getUsage()));
                return;
            }

            CommandSender p = lastReceived.get(sender);
            if (p == null || (p instanceof ProxiedPlayer && !((ProxiedPlayer) p).isConnected())) {
                BaseComponent send = new TranslatableComponent("commands.generic.player.notFound", p == null ? "" : p.getName());
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, send);
                else sender.sendMessage(send);
                return;
            }

            messenger(sender, p, args, 0);
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

        if (from instanceof ProxiedPlayer)
            plugin.logCommand((ProxiedPlayer) from, "/msg " + to.getName() + " " + String.join(" ",args));
    }

    // TODO: Should we vanilla messaging?
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
            receiving.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/reply "));
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
