package com.arcaneminecraft.bungee.storage;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.ReturnRunnable;
import com.arcaneminecraft.bungee.module.DiscordUserModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.arcaneminecraft.bungee.module.data.Player;
import com.arcaneminecraft.bungee.storage.sql.ReportDatabase;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * SQL Database must be MariaDB.
 * ab_players:
 * Stores: String uuid, String username, Timestamp firstseen, Timestamp lastseen, String timezone, long discord, int options
 *
 * ab_reports:
 * Stores: String uuid, String body, String server, String world, int x, int y, int z, int priority,
 */
public class SQLDatabase {
    private static SQLDatabase instance;

    private static final String PLAYER_INSERT = "INSERT INTO ab_players(uuid, username) VALUES(?, ?)";
    private static final String PLAYER_SELECT_BY_UUID = "SELECT * FROM ab_players WHERE uuid=? LIMIT 1";
    //private static final String PLAYER_SELECT_BY_USERNAME = "SELECT * FROM ab_players WHERE UPPER(username)=? LIMIT 1";
    private static final String PLAYER_SELECT_ALL_USERNAME_AND_UUID_AND_DISCORD = "SELECT username,uuid,discord FROM ab_players";
    //private static final String PLAYER_SELECT_ALL_UUID_BY_USERNAME = "SELECT uuid FROM ab_players WHERE UPPER(username)=?";
    private static final String PLAYER_SELECT_TIMEZONE_BY_UUID = "SELECT timezone FROM ab_players WHERE uuid=?";
    private static final String PLAYER_SELECT_DISCORD_BY_UUID = "SELECT discord FROM ab_players WHERE uuid=?";
    private static final String PLAYER_UPDATE_USERNAME = "UPDATE ab_players SET username=? WHERE uuid=?";
    private static final String PLAYER_UPDATE_LAST_SEEN_AND_OPTIONS_AND_TIMEZONE_AND_DISCORD = "UPDATE ab_players SET lastseen=?,options=?,timezone=?,discord=?  WHERE uuid=?";
    private static final String PLAYER_UPDATE_DISCORD_BY_DISCORD = "UPDATE ab_players SET discord=? WHERE discord=?";
    private static final String PLAYER_UPDATE_DISCORD_BY_UUID = "UPDATE ab_players SET discord=? WHERE uuid=?";

    //private static final String REPORT_INSERT = "INSERT INTO ab_reports(id, uuid, body) VALUES(?, ?, ?)";
    private static final String REPORT_UPDATE_LAST_AND_PRIORITY_BY_ID = "UPDATE ab_reports SET last=?,priority=? WHERE id=?";

    private static final String NEWS_SELECT_LATEST = "SELECT * FROM ab_news ORDER BY id DESC LIMIT 1";
    private static final String NEWS_INSERT_NEWS = "INSERT INTO ab_news(content, username, uuid) VALUES(?, ?, ?)";

    private final ArcaneBungee plugin;
    private final MariaDbPoolDataSource ds;


    public SQLDatabase(ArcaneBungee plugin) throws SQLException {
        SQLDatabase.instance = this;
        this.plugin = plugin;

        String url = "jdbc:mariadb://"
                + plugin.getConfig().getString("mariadb.hostname")
                + "/" + plugin.getConfig().getString("mariadb.database");
        String user = plugin.getConfig().getString("mariadb.username");
        String pass = plugin.getConfig().getString("mariadb.password");

        this.ds = new MariaDbPoolDataSource(url);
        ds.setUser(user);
        ds.setPassword(pass);
        ds.setLoginTimeout(10); // localhost connection shouldn't take long

        // Ping/test the server
        long timer = System.currentTimeMillis();
        try (Connection c = ds.getConnection()) {
            c.prepareStatement("/* ping */ SELECT 1").executeQuery().close();
        }
        long time = System.currentTimeMillis() - timer;
        if (time > 1000) {
            plugin.getLogger().warning("Connecting to database takes over 1 second: " + time);
        }

        final MinecraftPlayerModule mcmodule = plugin.getMinecraftPlayerModule();
        final DiscordUserModule dcmodule = plugin.getDiscordUserModule();

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT_ALL_USERNAME_AND_UUID_AND_DISCORD)) {
                    ResultSet rs = ps.executeQuery();

                    while(rs.next()) {
                        String n = rs.getString("username");
                        UUID u = UUID.fromString(rs.getString("uuid"));
                        long d = rs.getLong("discord");

                        mcmodule.put(u, n);
                        dcmodule.put(u, d);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public static SQLDatabase getInstance() {
        return instance;
    }

    private CompletableFuture<ResultSet> getPlayerResultSet(UUID uuid) {
        CompletableFuture<ResultSet> future = new CompletableFuture<>();

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                ResultSet rs;
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT_BY_UUID)) {
                    ps.setString(1, uuid.toString());
                    rs = ps.executeQuery();
                }

                future.complete(rs);
            } catch (SQLException ex) {
                ex.printStackTrace();
                future.complete(null);
            }
        });
        return future;
    }

    public CompletableFuture<Player> playerJoin(ProxiedPlayer p) {
        CompletableFuture<Player> future = new CompletableFuture<>();

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                ResultSet rs;

                // Get player info: player name
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT_BY_UUID)) {
                    ps.setString(1, p.getUniqueId().toString());
                    rs = ps.executeQuery();
                }

                // Check if query returned any data.
                if (!rs.next()) {
                    // There is no data = new player

                    try (PreparedStatement ps = c.prepareStatement(PLAYER_INSERT)) {
                        ps.setString(1, p.getUniqueId().toString());
                        ps.setString(2, p.getName());
                        ps.executeUpdate();
                    }

                    future.complete(new Player(p));
                    return;
                }

                String name = rs.getString("username");

                if (!p.getName().equals(name)) {
                    // Username changed
                    ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                        try (PreparedStatement ps = c.prepareStatement(PLAYER_UPDATE_USERNAME)) {
                            ps.setString(1, p.getName());
                            ps.setString(2, p.getUniqueId().toString());
                            ps.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
                Timestamp firstseen = rs.getTimestamp("firstseen");
                Timestamp lastseen = rs.getTimestamp("lastseen");
                String timezone = rs.getString("timezone"); // physical server location
                long discord = rs.getLong("discord");
                int options = rs.getInt("options");

                // Query returned data; give username from database
                future.complete(new Player(p, name, firstseen, lastseen, TimeZone.getTimeZone(timezone), discord, options));

            } catch (SQLException ex) {
                ex.printStackTrace();
                // Fetch failed: null
                future.complete(null);
            }
        });

        return future;
    }

    public CompletableFuture<Timestamp> getFirstSeen(UUID uuid) {
        CompletableFuture<Timestamp> future = new CompletableFuture<>();

        getPlayerResultSet(uuid).thenAcceptAsync(rs -> {
            try {
                future.complete(rs.getTimestamp("firstseen"));
            } catch (SQLException e) {
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    public CompletableFuture<Timestamp> getLastSeen(UUID uuid) {
        CompletableFuture<Timestamp> future = new CompletableFuture<>();

        getPlayerResultSet(uuid).thenAcceptAsync(rs -> {
            try {
                future.complete(rs.getTimestamp("lastSeen"));
            } catch (SQLException e) {
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    public void updatePlayer(Player p) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(PLAYER_UPDATE_LAST_SEEN_AND_OPTIONS_AND_TIMEZONE_AND_DISCORD)) {
                    ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps.setInt(2, p.getOptions());
                    ps.setString(3, p.getTimezone().getID());
                    ps.setLong(4, p.getDiscord());
                    ps.setString(5, p.getProxiedPlayer().getUniqueId().toString());
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public CompletableFuture<TimeZone> getTimeZone(UUID uuid) {
        CompletableFuture<TimeZone> future = new CompletableFuture<>();

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(PLAYER_SELECT_TIMEZONE_BY_UUID)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    future.complete(TimeZone.getTimeZone(rs.getString("timezone")));
                } else {
                    future.complete(null);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            future.complete(null);
        }});

        return future;
    }

    public void setDiscord(UUID uuid, long id) {
        // Remove possibly duplicate Discord
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try (Connection conn = ds.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(PLAYER_UPDATE_DISCORD_BY_DISCORD)) {
                    ps.setLong(1, 0);
                    ps.setLong(2, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(PLAYER_UPDATE_DISCORD_BY_UUID)) {
                    ps.setLong(1, id);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public CompletableFuture<Long> getDiscord(UUID uuid) {
        CompletableFuture<Long> future = new CompletableFuture<>();

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try (Connection conn = ds.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(PLAYER_SELECT_DISCORD_BY_UUID)) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();

                    if (rs.next())
                        future.complete(rs.getLong("discord"));
                    else
                        future.complete(0L);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    public void getLatestNewsThen(ReturnRunnable<String> run) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(NEWS_SELECT_LATEST)) {
                    ResultSet rs = ps.executeQuery();
                    // Push news posted date
                    if (rs.next())
                        run.run(rs.getString("content"));
                    else
                        run.run("There is no news");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void addNews(CommandSender sender, String news, ReturnRunnable<Boolean> run) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            String name, uuid;
            if (sender instanceof ProxiedPlayer) {
                name = sender.getName();
                uuid = ((ProxiedPlayer) sender).getUniqueId().toString();
            } else {
                name = "Server";
                uuid = null;
            }

            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(NEWS_INSERT_NEWS)) {
                    ps.setString(1, news);
                    ps.setString(2, name);
                    ps.setString(3, uuid);
                    int rs = ps.executeUpdate();
                    if (rs == 1)
                        run.run(true);
                    else
                        run.run(false);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }


    /*
     *
     *
     * Report Database stuff
     *
     *
     *
     *
     */

    public void reportUpdatePriority(int id, ReportDatabase.Priority priority) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(REPORT_UPDATE_LAST_AND_PRIORITY_BY_ID)) {
                    ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps.setInt(2, priority.getValue());
                    ps.setInt(3, id);
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }
}