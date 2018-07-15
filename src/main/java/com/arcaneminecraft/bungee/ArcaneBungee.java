package com.arcaneminecraft.bungee;

import com.arcaneminecraft.bungee.command.*;
import com.arcaneminecraft.bungee.storage.OptionsStorage;
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
import java.sql.SQLNonTransientConnectionException;
import java.util.logging.Level;

public class ArcaneBungee extends Plugin {
    private File configFile;
    private File cacheDataFile;
    private Configuration config = null;
    private Configuration cacheData = null;
    private SQLDatabase sqlDatabase = null;
    private PluginMessenger pluginMessenger;
    private TabCompletePreset tabCompletePreset;
    private BadgeCommands badgeCommands;
    private SpyAlert spyAlert;
    private static final String CONFIG_FILENAME = "cachedata.yml";

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder(), "config.yml");
        cacheDataFile = new File(getDataFolder(), CONFIG_FILENAME);

        saveDefaultConfigs();

        getProxy().registerChannel("ArcaneAlert");

        this.spyAlert = new SpyAlert(this);
        getProxy().getPluginManager().registerListener(this, this.spyAlert);

        this.pluginMessenger = new PluginMessenger(this, spyAlert);
        getProxy().getPluginManager().registerListener(this, this.pluginMessenger);

        if (getConfig().getBoolean("mariadb.enabled")) {
            try {
                this.sqlDatabase = new SQLDatabase(this);
            } catch (SQLNonTransientConnectionException e) {
                getLogger().severe("Cannot connect to database! Check configuration and reload the plugin.");
            } catch (SQLException e) {
                e.printStackTrace();
                //shrug
            }
        }
        new OptionsStorage(this);

        this.tabCompletePreset = new TabCompletePreset(this);
        getProxy().getPluginManager().registerListener(this, new VanillaEvents(this));



        // Commnads that directly depend on SQL
        if (sqlDatabase != null) {
            SeenCommands fs = new SeenCommands(this);
            getProxy().getPluginManager().registerCommand(this, fs.new Seen());
            getProxy().getPluginManager().registerCommand(this, fs.new FirstSeen());
            getProxy().getPluginManager().registerCommand(this, new FindPlayer(this));
            getProxy().getPluginManager().registerCommand(this, new News(this));
        }

        // Rest of the commands
        this.badgeCommands = new BadgeCommands(this);
        GreylistCommands g = new GreylistCommands(this);
        TellCommands t = new TellCommands(this);
        LinkCommands l = new LinkCommands(this);
        ServerCommands s = new ServerCommands(this);
        StaffChatCommands sc = new StaffChatCommands(this);
        getProxy().getPluginManager().registerCommand(this, badgeCommands.new Badge());
        getProxy().getPluginManager().registerCommand(this, badgeCommands.new BadgeAdmin());
        getProxy().getPluginManager().registerCommand(this, g.new Apply());
        getProxy().getPluginManager().registerCommand(this, g.new Greylist());
        getProxy().getPluginManager().registerCommand(this, t.new Message());
        getProxy().getPluginManager().registerCommand(this, t.new Reply());
        getProxy().getPluginManager().registerCommand(this, l.new Discord());
        getProxy().getPluginManager().registerCommand(this, l.new Links());
        getProxy().getPluginManager().registerCommand(this, l.new Forum());
        getProxy().getPluginManager().registerCommand(this, l.new Website());
        getProxy().getPluginManager().registerCommand(this, s.new Creative());
        getProxy().getPluginManager().registerCommand(this, s.new Event());
        getProxy().getPluginManager().registerCommand(this, s.new Survival());
        getProxy().getPluginManager().registerListener(this, sc);
        getProxy().getPluginManager().registerCommand(this, sc.new Chat());
        getProxy().getPluginManager().registerCommand(this, sc.new Toggle());
        getProxy().getPluginManager().registerCommand(this, new ListPlayers(this));
        getProxy().getPluginManager().registerCommand(this, new Me(this));
        getProxy().getPluginManager().registerCommand(this, new Ping(this));
        getProxy().getPluginManager().registerCommand(this, new Slap(this));
    }

    @Override
    public void onDisable() {
        config = null;
        badgeCommands.saveConfig();
        spyAlert.saveConfig();
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(cacheData, cacheDataFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save " + CONFIG_FILENAME, e);
        }
    }

    public SQLDatabase getSqlDatabase() {
        return sqlDatabase;
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
                config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return config;
    }

    public Configuration getCacheData() {
        if (cacheData == null) {
            try {
                this.cacheData = ConfigurationProvider.getProvider(YamlConfiguration.class).load(cacheDataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return cacheData;
    }

    private void saveDefaultConfigs() {
        if (!getDataFolder().exists())
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdir();

        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!cacheDataFile.exists()) {
            try (InputStream in = getResourceAsStream(CONFIG_FILENAME)) {
                Files.copy(in, cacheDataFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
