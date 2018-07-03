package com.arcaneminecraft.bungee;

import com.arcaneminecraft.bungee.command.*;
import com.arcaneminecraft.bungee.storage.SQLDatabase;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import java.sql.SQLException;

/**
 * ArcaneChatUtilPlugin.java
 * Close-chat function for the Arcane Survival server.
 *
 * @author Morios (Mark Talrey)
 * @author SimonOrJ (Simon Chuu)
 * @version 3.0-SNAPSHOT
 */

public final class ArcaneBungee extends Plugin {
    private File file;
    private Configuration config = null;
    private PluginMessenger pluginMessenger;
    private TabCompletePreset tabCompletePreset;

    @Override
    public void onEnable() {
        file = new File(getDataFolder(), "config.yml");

        saveDefaultConfig();

        getProxy().registerChannel("ArcaneAlert");

        this.tabCompletePreset = new TabCompletePreset(this);
        getProxy().getPluginManager().registerListener(this, this.pluginMessenger = new PluginMessenger(this));

        getProxy().getPluginManager().registerListener(this, new VanillaEvents(this));

        try {
            getProxy().getPluginManager().registerListener(this, new SQLDatabase(this));
        } catch (SQLException e) {
            e.printStackTrace();
            //shrug
        }

        StaffChatCommands sc = new StaffChatCommands(this);
        TellCommands t = new TellCommands(this);
        LinkCommands l = new LinkCommands(this);
        ServerCommands s = new ServerCommands(this);
        getProxy().getPluginManager().registerListener(this, sc);
        getProxy().getPluginManager().registerCommand(this, sc.new Chat());
        getProxy().getPluginManager().registerCommand(this, sc.new Toggle());
        getProxy().getPluginManager().registerCommand(this, t.new Message());
        getProxy().getPluginManager().registerCommand(this, t.new Reply());
        getProxy().getPluginManager().registerCommand(this, l.new Discord());
        getProxy().getPluginManager().registerCommand(this, l.new Links());
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

    @Override
    public void onDisable() {
        config = null;
    }

    public PluginMessenger getCommandLogger() {
        return pluginMessenger;
    }

    public TabCompletePreset getTabCompletePreset() {
        return tabCompletePreset;
    }

    public Configuration getConfig() {
        if (config == null) {
            try {
                config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return config;
    }

    private void saveDefaultConfig() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
