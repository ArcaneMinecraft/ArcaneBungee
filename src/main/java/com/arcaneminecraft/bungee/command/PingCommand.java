package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.TabCompletePreset;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;
import java.util.Locale;

public class PingCommand extends Command implements TabExecutor {
    public PingCommand() {
        super(BungeeCommandUsage.PING.getName(), BungeeCommandUsage.PING.getPermission(), BungeeCommandUsage.PING.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0 && !(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("Pong!"));
            return;
        }

        ProxiedPlayer target;

        if (args.length == 0) {
            target = (ProxiedPlayer) sender;
        } else {
            target = ProxyServer.getInstance().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ArcaneText.playerNotFound());
                return;
            }
        }

        Locale locale = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getLocale() : null;
        BaseComponent send;
        BaseComponent time = new TextComponent(target.getPing() + " ms");
        time.setColor(ArcaneColor.FOCUS);

        if (target == sender) {
            send = ArcaneText.translatable(locale, "commands.ping.self", time);
        } else {
            send = ArcaneText.translatable(locale, "commands.ping.other", ArcaneText.playerComponentBungee(target), time);
        }
        send.setColor(ArcaneColor.CONTENT);

        if (sender instanceof ProxiedPlayer)
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
        else
            sender.sendMessage(send);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return TabCompletePreset.onlinePlayers(args);
        return Collections.emptyList();
    }
}
