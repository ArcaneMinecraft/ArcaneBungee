package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.ProtocolConstants;

public class ProtocolEvent implements Listener {
    private final String versionToDisplay;

    ProtocolEvent(ArcaneBungee plugin) {
        this.versionToDisplay = plugin.getConfig().getString("mc-version-limit.version-to-display");
    }

    @EventHandler
    public void OnlyAbove1_13(ProxyPingEvent e){
        if (e.getConnection().getVersion() < ProtocolConstants.MINECRAFT_1_13) {
            ServerPing.Protocol p = new ServerPing.Protocol(versionToDisplay, ProtocolConstants.MINECRAFT_1_13);
            e.getResponse().setVersion(p);
        }
    }
}
