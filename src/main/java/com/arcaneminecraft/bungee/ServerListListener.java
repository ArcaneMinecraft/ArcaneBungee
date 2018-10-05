package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

// Status listener
public final class ServerListListener implements Listener {
    private final ArcaneBungee plugin;

    public ServerListListener(ArcaneBungee plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProxyPing(ProxyPingEvent e) {
        if (e.getResponse() == null) return; // Check if response is not empty

        ServerPing ping = e.getResponse();

        if (ping.getPlayers() != null) {
            ServerPing.Players pingPlayers = ping.getPlayers();

            int size = plugin.getProxy().getOnlineCount();
            ServerPing.PlayerInfo[] list = new ServerPing.PlayerInfo[size];
            Collection<ProxiedPlayer> ps = plugin.getProxy().getPlayers();

            int i = 0;
            for (ProxiedPlayer p : ps) {
                list[i] = new ServerPing.PlayerInfo(p.getName(), p.getUniqueId());
                i++;
                if (i == list.length)
                    break;
            }

            pingPlayers.setSample(list);
        }
    }
}