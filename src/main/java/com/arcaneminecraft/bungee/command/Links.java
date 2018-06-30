package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ColorPalette;
import com.arcaneminecraft.bungee.ArcaneBungee;
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
public class Links extends Command implements TabExecutor {
    private final ArcaneBungee plugin;
    private final List<String> candidates = Arrays.asList("discord","forum","website");

    private static final BaseComponent DISCORD = ArcaneText.url("https://arcaneminecraft.com/discord/");
    private static final BaseComponent FORUM = ArcaneText.url("https://arcaneminecraft.com/forum/");
    private static final BaseComponent WEBSITE = ArcaneText.url("https://arcaneminecraft.com/");

    public Links(ArcaneBungee plugin) {
        super(BungeeCommandUsage.LINKS.getName(), BungeeCommandUsage.LINKS.getPermission(), BungeeCommandUsage.LINKS.getAliases());
        this.plugin = plugin;
        // Configuration for adding more custom links?
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.LINKS.getCommand(), args);

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

    public class Discord extends Command {
        public Discord() {
            super(BungeeCommandUsage.DISCORD.getName(), BungeeCommandUsage.DISCORD.getPermission(), BungeeCommandUsage.DISCORD.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.DISCORD.getCommand(), args);

            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, singleLink("website", DISCORD));
            else
                sender.sendMessage(singleLink("website", DISCORD));
        }
    }

    public class Forum extends Command {
        public Forum() {
            super(BungeeCommandUsage.FORUM.getName(), BungeeCommandUsage.FORUM.getPermission(), BungeeCommandUsage.FORUM.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.FORUM.getCommand(), args);

            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, singleLink("website", FORUM));
            else
                sender.sendMessage(singleLink("website", FORUM));
        }
    }

    public class Website extends Command {
        public Website() {
            super(BungeeCommandUsage.WEBSITE.getName(), BungeeCommandUsage.WEBSITE.getPermission(), BungeeCommandUsage.WEBSITE.getAliases());
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.WEBSITE.getCommand(), args);

            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, singleLink("website", WEBSITE));
            else
                sender.sendMessage(singleLink("website", WEBSITE));
        }
    }

    private BaseComponent singleLink(String what, BaseComponent link) {
        BaseComponent ret = new TextComponent("Link to ");
        ret.addExtra(what);
        ret.addExtra(": ");
        ret.addExtra(link);
        ret.setColor(ColorPalette.CONTENT);
        return ret;
    }
}
