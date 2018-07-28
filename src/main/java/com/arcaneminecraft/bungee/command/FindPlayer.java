package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FindPlayer extends Command implements TabExecutor {
    private final ArcaneBungee plugin;

    public FindPlayer(ArcaneBungee plugin) {
        super(BungeeCommandUsage.FINDPLAYER.getName(), BungeeCommandUsage.FINDPLAYER.getPermission(), BungeeCommandUsage.FINDPLAYER.getAliases());
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.logCommand(sender, BungeeCommandUsage.FINDPLAYER.getCommand(), args);

        if (args.length == 0) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.FINDPLAYER.getUsage()));
            else
                sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.FINDPLAYER.getUsage()));
            return;
        }

        AtomicBoolean done = new AtomicBoolean(false);

        String searchLower = args[0].toLowerCase();
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            List<String> pl = new ArrayList<>();

            // Match all players
            for (String name : plugin.getSqlDatabase().getAllPlayerName()) {
                int start;
                int end;
                int searchLength = args[0].length();
                String nameLower = name.toLowerCase();
                if ((start = nameLower.indexOf(searchLower)) != -1) {
                    // make it so end - beginning = searching string
                    end = start + searchLength;

                    StringBuilder toAdd = new StringBuilder(name.substring(0, start));

                    // Highlight matched portion
                    toAdd.append(ArcaneColor.FOCUS)
                            .append(name, start, end)
                            .append(ArcaneColor.CONTENT);

                    // Search again until it goes through and matches the entire string.
                    while ((start = name.toLowerCase().indexOf(searchLower, end)) != -1) {
                        toAdd.append(name, end, start)
                                .append(ArcaneColor.FOCUS);

                        end = start + searchLength;

                        toAdd.append(name, start, end)
                                .append(ArcaneColor.CONTENT);
                    }
                    toAdd.append(name, end, name.length());

                    // Add it to the list.
                    pl.add(toAdd.toString());
                }
            }

            if (pl.isEmpty()) {
                done.set(true);
                BaseComponent send = new TextComponent("Player matching '" + args[0] + "' cannot be found");
                send.setColor(ArcaneColor.NEGATIVE);
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
                return;
            }

            pl.sort(Comparator.comparing(ChatColor::stripColor));
            done.set(true);

            BaseComponent head = new TextComponent("--- Players matching '");
            BaseComponent part = new TextComponent(args[0]);
            part.setColor(ChatColor.GREEN);
            head.addExtra(part);
            head.addExtra("' ---");
            head.setColor(ChatColor.DARK_GREEN);

            BaseComponent[] content = TextComponent.fromLegacyText(ArcaneColor.CONTENT + String.join(", ", pl));

            if (sender instanceof ProxiedPlayer) {
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, head);
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, content);
            } else {
                sender.sendMessage(head);
                sender.sendMessage(content);
            }
        });

        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (done.get())
                return;

            BaseComponent send = new TextComponent("Searching for players matching '");
            BaseComponent part = new TextComponent(args[0]);
            part.setColor(ArcaneColor.FOCUS);
            send.addExtra(part);
            send.addExtra("' - please wait...");
            send.setColor(ArcaneColor.CONTENT);

            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
            else
                sender.sendMessage(send);

        }, 1000L, TimeUnit.MILLISECONDS);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        // TODO: Find how long this command takes to run.
        if (args.length == 1)
            return plugin.getTabCompletePreset().allPlayers(args);
        return Collections.emptyList();
    }
}
