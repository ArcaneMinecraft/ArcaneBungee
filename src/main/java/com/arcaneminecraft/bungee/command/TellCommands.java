package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.DiscordUserModule;
import com.arcaneminecraft.bungee.module.MessengerModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.arcaneminecraft.bungee.module.SettingModule;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class TellCommands {
    private final ArcaneBungee plugin;
    private final HashMap<CommandSender, CommandSender> lastReceived = new HashMap<>();
    private MessengerModule module = ArcaneBungee.getInstance().getMessengerModule();
    private DiscordUserModule duModule = ArcaneBungee.getInstance().getDiscordUserModule();
    private MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();
    private SettingModule sModule = ArcaneBungee.getInstance().getSettingModule();

    public TellCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
     }

    public class Message extends Command implements TabExecutor {

        public Message() {
            super(BungeeCommandUsage.MSG.getName(), BungeeCommandUsage.MSG.getPermission(), BungeeCommandUsage.MSG.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.MSG.getUsage()));
                else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.MSG.getUsage()));
                return;
            }

            // Get recipient
            CommandSender p = plugin.getProxy().getPlayer(args[0]);
            if (p == null) {
                recipientNotOnline(sender, args[0]);
                return;
            }

            messenger(sender, p, args, 1);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1)
                return TabCompletePreset.onlinePlayers(args);
            return Collections.emptyList();
        }
    }

    public class Reply extends Command implements TabExecutor {

        public Reply() {
            super(BungeeCommandUsage.REPLY.getName(), BungeeCommandUsage.REPLY.getPermission(), BungeeCommandUsage.REPLY.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.REPLY.getUsage()));
                else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.REPLY.getUsage()));
                return;
            }

            CommandSender p = lastReceived.get(sender);

            if (p == null) {
                noReplyRecipient(sender);
                return;
            }

            if (p instanceof ProxiedPlayer && !((ProxiedPlayer) p).isConnected()) {
                recipientNotOnline(sender, ((ProxiedPlayer) p).getUniqueId());
                return;
            }

            messenger(sender, p, args, 0);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
    }

    private void noReplyRecipient(CommandSender sender) {
        BaseComponent send = ArcaneText.translatable(
                sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getLocale() : null,
                "commands.reply.nobody"
        );
        send.setColor(ChatColor.RED);

        if (sender instanceof ProxiedPlayer)
            ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, send);
        else
            sender.sendMessage(send);
    }

    private void recipientNotOnline(CommandSender sender, String recipient) {
        UUID uuid = mpModule.getUUID(recipient);
        if (uuid == null) {
            // Player DNE
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound());
            else
                sender.sendMessage(ArcaneText.playerNotFound());
            return;
        }
        recipientNotOnline(sender, uuid);
    }

    private void recipientNotOnline(CommandSender sender, UUID uuid) {
        sModule.get(SettingModule.Option.SET_DISCORD_PUBLIC, uuid).thenAcceptAsync(yes -> {
            Locale locale = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getLocale() : null;
            String name = mpModule.getName(uuid);
            long id = yes ? duModule.getDiscordId(uuid) : 0;
            BaseComponent send;

            if (id == 0) {
                // offline
                send = ArcaneText.translatable(locale, "commands.message.offline", name);
            } else {
                // offline but has Discord information set as public
                BaseComponent nick = new TextComponent(duModule.getNickname(id));
                nick.setColor(ArcaneColor.FOCUS);
                String tag = duModule.getUserTag(id);

                send = ArcaneText.translatable(locale, "commands.message.offline.discord", name, nick, tag);
            }

            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
            else
                sender.sendMessage(send);
        });
    }

    private void messenger(CommandSender from, CommandSender to, String[] args, int fromIndex) {
        BaseComponent msg = ArcaneText.url(args, fromIndex);

        module.sendP2pMessage(from, to, msg);

        // Update sender-receiver map
        lastReceived.put(to, from);
    }
}
