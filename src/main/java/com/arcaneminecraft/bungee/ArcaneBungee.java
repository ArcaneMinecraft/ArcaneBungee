package com.arcaneminecraft.bungee;

import com.arcaneminecraft.bungee.channel.DiscordConnection;
import com.arcaneminecraft.bungee.channel.PluginMessenger;
import com.arcaneminecraft.bungee.command.*;
import com.arcaneminecraft.bungee.module.ChatPrefixModule;
import com.arcaneminecraft.bungee.module.DiscordUserModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.arcaneminecraft.bungee.module.NewsModule;
import com.arcaneminecraft.bungee.storage.OptionsStorage;
import com.arcaneminecraft.bungee.storage.SQLDatabase;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.logging.Level;

public class ArcaneBungee extends Plugin {
    private File configFile;
    private File cacheDataFile;
    private Configuration config = null;
    private Configuration cacheData = null;
    private SQLDatabase sqlDatabase = null;
    private PluginMessenger pluginMessenger;
    private SpyAlert spyAlert;
    private DiscordConnection discordConnection;

    /* Modules */
    private final ChatPrefixModule chatPrefixModule = new ChatPrefixModule();
    private final DiscordUserModule discordUserModule = new DiscordUserModule();
    private final MinecraftPlayerModule minecraftPlayerModule = new MinecraftPlayerModule();
    private final NewsModule newsModule = new NewsModule();


    private static final String CONFIG_FILENAME = "cachedata.yml";

    // TODO: Move this to Player module
    private ArrayList<ProxiedPlayer> afkPlayers;

    private static ArcaneBungee instance;

    public static ArcaneBungee getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        ArcaneBungee.instance = this;
        this.configFile = new File(getDataFolder(), "config.yml");
        this.cacheDataFile = new File(getDataFolder(), CONFIG_FILENAME);
        this.afkPlayers = new ArrayList<>();

        saveDefaultConfigs();

        // Alert

        getProxy().registerChannel("arcaneserver:alert");

        this.spyAlert = new SpyAlert(this);
        getProxy().getPluginManager().registerListener(this, this.spyAlert);

        this.pluginMessenger = new PluginMessenger(this, spyAlert);
        getProxy().getPluginManager().registerListener(this, this.pluginMessenger);

        if (getConfig().getBoolean("mariadb.enabled")) {
            try {
                this.sqlDatabase = new SQLDatabase(this);
            } catch (SQLNonTransientConnectionException e) {
                getLogger().log(Level.SEVERE, "Cannot connect to database! Check configuration and reload the plugin.", e);
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "SQL Exception occured. Please try restarting.", e);
                //shrug
            }
        }
        new OptionsStorage(this);

        if (!getConfig().getString("discord.token", "0").equals("0")) {
            try {
                this.discordConnection = new DiscordConnection(this);
            } catch (LoginException e) {
                getLogger().log(Level.SEVERE, "Discord: Invalid Token. Please restart with valid token.", e);
            } catch (InterruptedException e) {
                getLogger().log(Level.SEVERE, "Discord: Login Interrupted. ", e);
            }
        } else {
            getLogger().warning("Discord token is not specified. Restart with valid token if Discord is to be connected and used.");
        }

        getProxy().getPluginManager().registerListener(this, new JoinLeaveEvents(this));

        // MC Version Limiter
        if (getConfig().getBoolean("mc-version-limit.enabled"))
            getProxy().getPluginManager().registerListener(this, new ProtocolEvent(this));

        // Player list
        getProxy().getPluginManager().registerListener(this, new ServerListListener(this));

        // Commnads that directly depend on SQL
        if (sqlDatabase != null) {
            SeenCommands fs = new SeenCommands(this);
            getProxy().getPluginManager().registerCommand(this, fs.new Seen());
            getProxy().getPluginManager().registerCommand(this, fs.new FirstSeen());
            getProxy().getPluginManager().registerCommand(this, new FindPlayer(this));
            getProxy().getPluginManager().registerCommand(this, new News(this));
        }

        // Rest of the commands
        GreylistCommands g = new GreylistCommands(this);
        TellCommands t = new TellCommands(this);
        LinkCommands l = new LinkCommands();
        ServerCommands s = new ServerCommands(this);
        StaffChatCommands sc = new StaffChatCommands(this);
        getProxy().getPluginManager().registerCommand(this, new Badge());
        getProxy().getPluginManager().registerCommand(this, new BadgeAdmin());
        getProxy().getPluginManager().registerCommand(this, new Discord());
        getProxy().getPluginManager().registerCommand(this, g.new Apply());
        getProxy().getPluginManager().registerCommand(this, g.new Greylist());
        getProxy().getPluginManager().registerCommand(this, t.new Message());
        getProxy().getPluginManager().registerCommand(this, t.new Reply());
        getProxy().getPluginManager().registerCommand(this, l.new Links());
        getProxy().getPluginManager().registerCommand(this, l.new Donate());
        getProxy().getPluginManager().registerCommand(this, l.new Forum());
        getProxy().getPluginManager().registerCommand(this, l.new Rules());
        getProxy().getPluginManager().registerCommand(this, l.new Website());
        getProxy().getPluginManager().registerCommand(this, s.new Creative());
        getProxy().getPluginManager().registerCommand(this, s.new Event());
        getProxy().getPluginManager().registerCommand(this, s.new Survival());
        getProxy().getPluginManager().registerListener(this, sc);
        getProxy().getPluginManager().registerCommand(this, sc.new Chat());
        getProxy().getPluginManager().registerCommand(this, sc.new Toggle());
        getProxy().getPluginManager().registerCommand(this, new ListPlayers(this));
        getProxy().getPluginManager().registerCommand(this, new Me(this));
        getProxy().getPluginManager().registerCommand(this, new Options(this));
        getProxy().getPluginManager().registerCommand(this, new Ping(this));
        getProxy().getPluginManager().registerCommand(this, new Slap(this));

        getProxy().getPluginManager().registerListener(this, new CommandEvent());
    }

    @Override
    public void onDisable() {
        config = null;
        chatPrefixModule.saveConfig();
        spyAlert.saveConfig();
        if (discordConnection != null)
            discordConnection.disable();
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(cacheData, cacheDataFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save " + CONFIG_FILENAME, e);
        }
    }

    public ArrayList<ProxiedPlayer> getAfkList() {
        return afkPlayers;
    }

    @Deprecated
    public void logCommand(CommandSender sender, String cmd, String[] args) {
    }

    @Deprecated
    public void logCommand(CommandSender sender, String msg) {
    }

    @Deprecated
    public void logCommand(String name, String displayName, String uuid, String msg) {
    }

    public PluginMessenger getPluginMessenger() {
        return pluginMessenger;
    }

    public DiscordConnection getDiscordConnection() {
        return discordConnection;
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

    public ChatPrefixModule getChatPrefixModule() {
        return chatPrefixModule;
    }

    public DiscordUserModule getDiscordUserModule() {
        return discordUserModule;
    }

    public MinecraftPlayerModule getMinecraftPlayerModule() {
        return minecraftPlayerModule;
    }

    public NewsModule getNewsModule() {
        return newsModule;
    }
}
