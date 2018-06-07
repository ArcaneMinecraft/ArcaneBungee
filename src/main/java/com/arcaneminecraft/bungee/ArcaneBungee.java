package com.arcaneminecraft.bungee;

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
    private ArcaneLogSender arcaneLogSender;

    @Override
    public void onEnable() {
        String logIP = "127.0.0.1";
        int logPort = 25555;

        TabCompletePreset.setPlugin(this);

        getProxy().getPluginManager().registerListener(
                this, arcaneLogSender = new ArcaneLogSender(this, logIP, logPort));

        StaffChat sc = new StaffChat(this);
        getProxy().getPluginManager().registerCommand(this, sc.getChatListener());
        getProxy().getPluginManager().registerCommand(this, sc.getToggleListener());
        getProxy().getPluginManager().registerListener(this, sc);

        Tell tell = new Tell(this);
        getProxy().getPluginManager().registerCommand(this, tell.getMessage());
        getProxy().getPluginManager().registerCommand(this, tell.getReply());
    }

    ArcaneLogSender getArcaneLogger() {
        return arcaneLogSender;
    }
}
