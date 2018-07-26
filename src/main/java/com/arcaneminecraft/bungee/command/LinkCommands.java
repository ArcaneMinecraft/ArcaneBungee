package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.channel.DiscordConnection;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO: Make the output of each individual commands prettier (cause it's a mess as of now)
public class LinkCommands {
    private final ArcaneBungee plugin;
    private final List<String> candidates = Arrays.asList("discord","forum","website");

    private static final BaseComponent DISCORD = ArcaneText.url("https://arcaneminecraft.com/discord/");
    private static final BaseComponent FORUM = ArcaneText.url("https://arcaneminecraft.com/forum/");
    private static final BaseComponent WEBSITE = ArcaneText.url("https://arcaneminecraft.com/");

    public LinkCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
        // Configuration for adding more custom links?
    }

    private BaseComponent singleLink(String what, BaseComponent link) {
        BaseComponent ret = new TextComponent("Link to ");
        ret.addExtra(what);
        ret.addExtra(": ");
        ret.addExtra(link);
        ret.setColor(ArcaneColor.CONTENT);
        return ret;
    }

    public class Links extends Command implements TabExecutor {

        public Links() {
            super(BungeeCommandUsage.LINKS.getName(), BungeeCommandUsage.LINKS.getPermission(), BungeeCommandUsage.LINKS.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.LINKS.getCommand(), args);

            if (sender instanceof ProxiedPlayer) {
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, WEBSITE);
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, DISCORD);
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, FORUM);
            } else {
                sender.sendMessage(WEBSITE);
                sender.sendMessage(DISCORD);
                sender.sendMessage(FORUM);
            }
        }
        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                List<String> ret = new ArrayList<>();
                for (String s : candidates)
                    if (s.startsWith(args[0]))
                        ret.add(s);
                return ret;
            }

            return ImmutableSet.of();
        }

    }
    public class Discord extends Command {
        private final BaseComponent offlineMsg;

        public Discord() {
            super(BungeeCommandUsage.DISCORD.getName(), BungeeCommandUsage.DISCORD.getPermission(), BungeeCommandUsage.DISCORD.getAliases());
            this.offlineMsg = new TextComponent("Discord Connection is not enabled or is offline");
            this.offlineMsg.setColor(ArcaneColor.CONTENT);
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.DISCORD.getCommand(), args);

            if (sender instanceof ProxiedPlayer && args.length != 0) {
                if (args[0].equalsIgnoreCase("link")) {
                    DiscordConnection d = plugin.getDiscordConnection();
                    if (d != null)
                        d.userLink((ProxiedPlayer) sender);
                    else {
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, offlineMsg);
                    }
                    return;
                }

                if (args[0].equalsIgnoreCase("unlink")) {
                    DiscordConnection d = plugin.getDiscordConnection();
                    if (d != null)
                        d.userUnlink((ProxiedPlayer) sender);
                    else
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, offlineMsg);
                    return;
                }
            }

            if (sender instanceof ProxiedPlayer) {
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, singleLink("Discord", DISCORD));
                BaseComponent send = new TextComponent(" Other usage: /discord [link|unlink]");
                send.setColor(ArcaneColor.CONTENT);
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
            } else {
                sender.sendMessage(singleLink("Discord", DISCORD));
            }
        }

    }
    public class Forum extends Command {
        public Forum() {
            super(BungeeCommandUsage.FORUM.getName(), BungeeCommandUsage.FORUM.getPermission(), BungeeCommandUsage.FORUM.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.FORUM.getCommand(), args);

            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, singleLink("forum", FORUM));
            else
                sender.sendMessage(singleLink("forum", FORUM));
        }

    }
    public class Website extends Command {
        public Website() {
            super(BungeeCommandUsage.WEBSITE.getName(), BungeeCommandUsage.WEBSITE.getPermission(), BungeeCommandUsage.WEBSITE.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.WEBSITE.getCommand(), args);

            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, singleLink("website", WEBSITE));
            else
                sender.sendMessage(singleLink("website", WEBSITE));
        }

    }
}
