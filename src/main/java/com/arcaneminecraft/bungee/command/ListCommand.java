package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Iterator;

public class ListCommand extends Command implements TabExecutor {

    public ListCommand() {
        super(BungeeCommandUsage.LIST.getName(), BungeeCommandUsage.LIST.getPermission(), BungeeCommandUsage.LIST.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean showUUID = args.length != 0 && args[0].equalsIgnoreCase("uuids");

        ProxyServer server = ProxyServer.getInstance();

        TranslatableComponent send = new TranslatableComponent("commands.list.players", // 1.13
                String.valueOf(server.getOnlineCount()),
                String.valueOf(server.getConfig().getPlayerLimit())
        );

        BaseComponent body = new TextComponent();
        Iterator<ProxiedPlayer> i = server.getPlayers().iterator();
        if (i.hasNext()) {
            ProxiedPlayer first = i.next();
            body.addExtra(ArcaneText.playerComponentBungee(first));
            if (showUUID)
                body.addExtra("(" + first.getUniqueId() + ")");

            i.forEachRemaining((ProxiedPlayer p) -> {
                body.addExtra(", ");
                body.addExtra(ArcaneText.playerComponentBungee(p));
                if (showUUID)
                    body.addExtra("(" + p.getUniqueId() + ")");
            });
        }

        send.addWith(body);

        if (sender instanceof ProxiedPlayer) {
            ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, send);
        } else {
            sender.sendMessage(send);
        }

    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return ImmutableSet.of("uuids");
    }
}
