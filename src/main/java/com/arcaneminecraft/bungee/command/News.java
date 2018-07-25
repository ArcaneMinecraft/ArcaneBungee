package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Collections;

public class News extends Command implements TabExecutor {
    ArcaneBungee plugin;
    public News(ArcaneBungee plugin) {
        super(BungeeCommandUsage.NEWS.getName(), BungeeCommandUsage.NEWS.getPermission(), BungeeCommandUsage.NEWS.getAliases());
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.logCommand(sender, BungeeCommandUsage.NEWS.getCommand(), args);

        if (args.length == 0) {
            BaseComponent latest = new TextComponent("Latest news");
            latest.setColor(ArcaneColor.HEADING);
            BaseComponent send = new TextComponent(latest);
            send.addExtra(": ");
            send.setColor(ArcaneColor.FOCUS);

            plugin.getSqlDatabase().getLatestNews(news -> {
                send.addExtra(news);

                if (sender instanceof ProxiedPlayer) {
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                } else {
                    sender.sendMessage(send);
                }
            });
            return;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("arcane.command.news.set")) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.noPermissionMsg());
                else
                    sender.sendMessage(ArcaneText.noPermissionMsg());
                return;
            }

            if (args.length == 1) {
                BaseComponent send = ArcaneText.usage("/news set <content ...>");
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
                return;
            }

            StringBuilder n = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i++) {
                n.append(" ").append(args[i]);
            }

            String news = n.toString();

            plugin.getSqlDatabase().addNews(sender, news, isSet -> {
                BaseComponent send = new TextComponent(isSet
                        ? "News successfully set: " + news
                        : "News was not set"
                );
                send.setColor(isSet ? ArcaneColor.CONTENT : ArcaneColor.NEGATIVE);
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
            });
        }
        // TODO: Allow to see news history
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("arcane.command.news.set")) {
            ArrayList<String> iter = new ArrayList<>();
            iter.add("set");
            return plugin.getTabCompletePreset().argStartsWith(args, iter);
        }
        return Collections.emptyList();
    }
}
