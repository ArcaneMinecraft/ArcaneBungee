package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class VanillaEvents implements Listener {
    private final ArcaneBungee plugin;

    VanillaEvents (ArcaneBungee plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLoginJoin(PostLoginEvent e) {
        String joined = e.getPlayer().getDisplayName();
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            if (p == e.getPlayer()) continue;
            // TODO: Name change "multiplayer.player.joined.renamed"
            // Ignore plugin stuff
            p.sendMessage(ChatMessageType.SYSTEM, new TranslatableComponent("multiplayer.player.joined", joined));
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent e) {
        String joined = e.getPlayer().getDisplayName();
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            if (p == e.getPlayer()) continue;
            // Ignore plugin stuff
            p.sendMessage(ChatMessageType.SYSTEM, new TranslatableComponent("multiplayer.player.left", joined));
        }
    }
}
