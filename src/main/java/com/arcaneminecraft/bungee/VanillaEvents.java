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
        plugin.getCommandLogger().getPlayerName(e.getPlayer().getUniqueId().toString(), (String name) -> {
            BaseComponent joined = null;

            if (name != null && !e.getPlayer().getName().equals(name)) {
                if (name.equals("")) { // Empty name = new player
                    BaseComponent newPlayer = new TextComponent(ArcaneText.playerComponentBungee(e.getPlayer()));
                    newPlayer.addExtra(" joined for the first time!");
                    newPlayer.setColor(ChatColor.YELLOW);

                    for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                        p.sendMessage(ChatMessageType.SYSTEM, newPlayer);
                    }
                } else {
                    joined = new TranslatableComponent("multiplayer.player.joined.renamed", ArcaneText.playerComponentBungee(e.getPlayer()), name);
                }
            }

            if (joined == null)
                joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(e.getPlayer()));

            joined.setColor(ChatColor.YELLOW);
            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                if (p.equals(e.getPlayer())) continue;
                p.sendMessage(ChatMessageType.SYSTEM, joined);
            }
        });
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent e) {
        BaseComponent left = new TranslatableComponent("multiplayer.player.left", ArcaneText.playerComponentBungee(e.getPlayer()));
        left.setColor(ChatColor.YELLOW);
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            p.sendMessage(ChatMessageType.SYSTEM, left);
        }
    }
}
