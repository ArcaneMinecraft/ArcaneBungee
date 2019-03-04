package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class CommandEvent implements Listener {
    @EventHandler
    public void onCommand(ChatEvent e) {
        if (!e.isProxyCommand())
            return;

        // Log only player commands
        if (e.getSender() instanceof ProxiedPlayer)
            ArcaneBungee.getInstance().getPluginMessenger().coreprotect((CommandSender) e.getSender(), e.getMessage());
    }
}
