package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collection;
import java.util.Iterator;

public class ListPlayers extends Command implements TabExecutor {
    private final ArcaneBungee plugin;

    public ListPlayers(ArcaneBungee plugin) {
        super(BungeeCommandUsage.LIST.getName(), BungeeCommandUsage.LIST.getPermission(), BungeeCommandUsage.LIST.getAliases());
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.LIST.getCommand(), args);

        Collection<ProxiedPlayer> cp = plugin.getProxy().getPlayers();
        BaseComponent head = new TranslatableComponent("There are %s/%s players online:", // TODO: Update Translatable node
                String.valueOf(plugin.getProxy().getOnlineCount()),
                String.valueOf(plugin.getProxy().getConfig().getPlayerLimit()));

        BaseComponent body = new TextComponent();
        Iterator<ProxiedPlayer> i = plugin.getProxy().getPlayers().iterator();
        if (i.hasNext()) {
            body.addExtra(ArcaneText.playerComponentBungee(i.next()));

            i.forEachRemaining((ProxiedPlayer p) -> {
                body.addExtra(", ");
                body.addExtra(ArcaneText.playerComponentBungee(p));
            });
        }

        if (sender instanceof ProxiedPlayer) {
            ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, head);
            ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, body);
        } else {
            sender.sendMessage(head);
            sender.sendMessage(body);
        }

    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return ImmutableSet.of("uuids");
    }
}
