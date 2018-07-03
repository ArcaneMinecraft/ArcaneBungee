package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneText;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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
        plugin.getSqlDatabase().checkName(e.getPlayer(), name -> {
            BaseComponent joined;
            if (name == null) {
                // Exceptioned out
                joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(e.getPlayer()));
            } else if (name.equals("")) {
                // New player
                BaseComponent newPlayer = new TextComponent(ArcaneText.playerComponentBungee(e.getPlayer()));
                newPlayer.addExtra(" joined for the first time!");
                newPlayer.setColor(ChatColor.YELLOW);

                for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                    p.sendMessage(ChatMessageType.SYSTEM, newPlayer);
                }
                joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(e.getPlayer()));
            } else if (name.equals(e.getPlayer().getName())) {
                // Player with same old name
                joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(e.getPlayer()));
            } else {
                // Player with new name
                joined = new TranslatableComponent("multiplayer.player.joined.renamed", ArcaneText.playerComponentBungee(e.getPlayer()), name);
            }

            joined.setColor(ChatColor.YELLOW);

            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                if (p.equals(e.getPlayer())) continue;
                p.sendMessage(ChatMessageType.SYSTEM, joined);
            }
        });
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent e) {
        plugin.getSqlDatabase().updateLastSeen(e.getPlayer().getUniqueId().toString());

        BaseComponent left = new TranslatableComponent("multiplayer.player.left", ArcaneText.playerComponentBungee(e.getPlayer()));
        left.setColor(ChatColor.YELLOW);
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            p.sendMessage(ChatMessageType.SYSTEM, left);
        }
    }
}
