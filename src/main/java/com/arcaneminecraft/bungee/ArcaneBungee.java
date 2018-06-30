package com.arcaneminecraft.bungee;

import com.arcaneminecraft.bungee.command.*;
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
    private TabCompletePreset tabCompletePreset;

    @Override
    public void onEnable() {
        String logIP = "127.0.0.1";
        int logPort = 25555;
        getProxy().registerChannel("ArcaneAlert");

        this.tabCompletePreset = new TabCompletePreset(this);
        this.pluginMessenger = new PluginMessenger(this, logIP, logPort);

        getProxy().getPluginManager().registerListener(this, pluginMessenger);

        getProxy().getPluginManager().registerListener(this, new VanillaEvents(this));
        getProxy().getPluginManager().registerListener(this, new StaffChat(this));
        new Tell(this);

        StaffChat sc = new StaffChat(this);
        Tell t = new Tell(this);
        Links l = new Links(this);
        ServerCommands s = new ServerCommands(this);
        getProxy().getPluginManager().registerCommand(this, sc.new Chat()); // Staff Chat
        getProxy().getPluginManager().registerCommand(this, sc.new Toggle());
        getProxy().getPluginManager().registerCommand(this, t.new Message());
        getProxy().getPluginManager().registerCommand(this, t.new Reply());
        getProxy().getPluginManager().registerCommand(this, l.new Discord());
        getProxy().getPluginManager().registerCommand(this, l.new Forum());
        getProxy().getPluginManager().registerCommand(this, l.new Website());
        getProxy().getPluginManager().registerCommand(this, s.new Creative());
        getProxy().getPluginManager().registerCommand(this, s.new Event());
        getProxy().getPluginManager().registerCommand(this, s.new Survival());
        getProxy().getPluginManager().registerCommand(this, new Apply(this));
        getProxy().getPluginManager().registerCommand(this, new ListPlayers(this));
        getProxy().getPluginManager().registerCommand(this, new Me(this));
        getProxy().getPluginManager().registerCommand(this, new Ping(this));
        getProxy().getPluginManager().registerCommand(this, new Slap(this));
    }

    public PluginMessenger getCommandLogger() {
        return pluginMessenger;
    }

    public TabCompletePreset getTabCompletePreset() {
        return tabCompletePreset;
    }
}
