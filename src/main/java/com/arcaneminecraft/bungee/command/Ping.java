package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;

public class Ping extends Command implements TabExecutor {
    private final ArcaneBungee plugin;

    public Ping(ArcaneBungee plugin) {
        super(BungeeCommandUsage.PING.getName(), BungeeCommandUsage.PING.getPermission(), BungeeCommandUsage.PING.getAliases());
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.logCommand(sender, BungeeCommandUsage.PING.getCommand(), args);

        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("Your ping will forever be <1ms."));
            return;
        }

        ProxiedPlayer p;

        if (args.length == 0) {
            p = (ProxiedPlayer) sender;
        } else {
            p = plugin.getProxy().getPlayer(args[0]);
            if (p == null) {
                sender.sendMessage(ArcaneText.playerNotFound(args[0]));
                return;
            }
        }

        BaseComponent m = new TextComponent("Pong! ");
        if (p == sender) {
            m.addExtra("Your");
        } else {
            m.addExtra(ArcaneText.playerComponentBungee(p));
            m.addExtra("'s");
        }
        m.addExtra(" ping: ");

        BaseComponent n = new TextComponent(p.getPing() + " ms");
        n.setColor(ArcaneColor.FOCUS);

        m.addExtra(n);

        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, m);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return plugin.getTabCompletePreset().onlinePlayers(args);
        return Collections.emptyList();
    }
}
