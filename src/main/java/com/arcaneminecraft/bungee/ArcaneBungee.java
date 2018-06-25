package com.arcaneminecraft.bungee;

import com.arcaneminecraft.bungee.command.SimpleCommands;
import com.arcaneminecraft.bungee.command.StaffChat;
import com.arcaneminecraft.bungee.command.Tell;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * ArcaneChatUtilPlugin.java
 * Close-chat function for the Arcane Survival server.
 *
 * @author Morios (Mark Talrey)
 * @author SimonOrJ (Simon Chuu)
 * @version 3.0-SNAPSHOT
 */

public final class ArcaneBungee extends Plugin {
    private PluginMessenger pluginMessenger;

    @Override
    public void onEnable() {
        String logIP = "127.0.0.1";
        int logPort = 25555;

        TabCompletePreset.setPlugin(this);

        getProxy().getPluginManager().registerListener(
                this, pluginMessenger = new PluginMessenger(this, logIP, logPort));

        getProxy().getPluginManager().registerListener(this, new VanillaEvents(this));
        getProxy().getPluginManager().registerListener(this, new StaffChat(this));
        new Tell(this);
        new SimpleCommands(this);
    }

    public PluginMessenger getCommandLogger() {
        return pluginMessenger;
    }
}
