package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ColorPalette;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;

public class Apply extends Command implements TabExecutor {
    private final ArcaneBungee plugin;
    private static final BaseComponent LINK = ArcaneText.url("https://arcaneminecraft.com/apply/");

    public Apply(ArcaneBungee plugin) {
        super(BungeeCommandUsage.APPLY.getName(), BungeeCommandUsage.APPLY.getPermission(), BungeeCommandUsage.APPLY.getAliases());
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.APPLY.getCommand(), args);

        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ArcaneText.noConsoleMsg());
            return;
        }

        ProxiedPlayer p = (ProxiedPlayer) sender;

        if (sender.hasPermission("arcane.build")) {
            BaseComponent send = new TextComponent("You are already greylisted");
            send.setColor(ColorPalette.CONTENT);
            p.sendMessage(ChatMessageType.SYSTEM, send);
            return;
        }

        BaseComponent send = new TextComponent("Apply at: ");
        send.addExtra(LINK);
        send.setColor(ColorPalette.CONTENT);
        p.sendMessage(ChatMessageType.SYSTEM, send);
        // TODO: Write application in-game?
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
