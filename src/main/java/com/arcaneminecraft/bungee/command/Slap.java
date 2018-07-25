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

public class Slap extends Command implements TabExecutor {
    private final ArcaneBungee plugin;

    public Slap(ArcaneBungee plugin) {
        super(BungeeCommandUsage.SLAP.getName(), BungeeCommandUsage.SLAP.getPermission(), BungeeCommandUsage.SLAP.getAliases());
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.logCommand(sender, BungeeCommandUsage.SLAP.getCommand(), args);

        if (args.length == 0) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.SLAP.getUsage()));
            else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.SLAP.getUsage()));
            return;
        }

        ProxiedPlayer victim = plugin.getProxy().getPlayer(args[0]);

        if (victim == null) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(args[0]));
            else sender.sendMessage(ArcaneText.playerNotFound(args[0]));
            return;
        }

        BaseComponent send = new TextComponent(ArcaneText.playerComponentBungee(sender));
        send.addExtra(" slapped ");
        send.addExtra(ArcaneText.playerComponentBungee(victim));
        send.addExtra(" in the face!");

        send.setColor(ArcaneColor.META);

        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            p.sendMessage(ChatMessageType.SYSTEM, send);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return plugin.getTabCompletePreset().onlinePlayers(args);
        return Collections.emptyList();
    }
}
