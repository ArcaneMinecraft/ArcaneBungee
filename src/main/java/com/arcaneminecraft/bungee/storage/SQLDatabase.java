package com.arcaneminecraft.bungee.storage;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.ReturnRunnable;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ConnectedPlayer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.*;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * SQL Database must be MariaDB.
 * Stores: String uuid, String username, Date firstseen, Date lastseen, boolean greylist, boolean discord
 */
// TODO: Cache
public class SQLDatabase {
    private static final String PLAYER_INSERT = "INSERT INTO ab_players(uuid, username) VALUES(?, ?)";
    private static final String PLAYER_SELECT_BY_UUID = "SELECT * FROM ab_players WHERE uuid=? LIMIT 1";
    private static final String PLAYER_SELECT_BY_USERNAME = "SELECT * FROM ab_players WHERE UPPER(username)=? LIMIT 1";
    private static final String PLAYER_SELECT_ALL_USERNAME = "SELECT username FROM ab_players";
    private static final String PLAYER_SELECT_ALL_UUID_BY_USERNAME = "SELECT uuid FROM ab_players WHERE UPPER(username)=?";
    private static final String PLAYER_SELECT_TIMEZONE_BY_UUID = "SELECT timezone FROM ab_players WHERE uuid=?";
    private static final String PLAYER_UPDATE_USERNAME = "UPDATE ab_players SET username=? WHERE uuid=?";
    private static final String PLAYER_UPDATE_LAST_SEEN = "UPDATE ab_players SET lastseen=? WHERE uuid=?";
    private static final String NEWS_SELECT_LATEST = "SELECT * FROM ab_news ORDER BY id DESC LIMIT 1";
    private static final String NEWS_INSERT_NEWS = "INSERT INTO ab_news(content, username, uuid) VALUES(?, ?, ?)";

    private final ArcaneBungee plugin;
    private final HashMap<UUID, Cache> onlinePlayerCache;
    private final MariaDbPoolDataSource ds;


    public SQLDatabase(ArcaneBungee plugin) throws SQLException {
        this.plugin = plugin;
        this.onlinePlayerCache = new HashMap<>();

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
    }

    public void getPlayerUUID(String name, ReturnRunnable<UUID> run) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                ResultSet rs;
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT_BY_USERNAME)) {
                    ps.setString(1, name.toUpperCase());
                    rs = ps.executeQuery();
                }
                if (rs.next())
                    run.run(UUID.fromString(rs.getString("uuid")));
                else
                    run.run(null);
            } catch (SQLException e) {
                e.printStackTrace();
                run.run(null);
            }
        });
    }

    public void playerJoin(ProxiedPlayer p, ReturnRunnable<String> run) {
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
                    // is new player: empty string
                    onlinePlayerCache.put(p.getUniqueId(), new Cache(p));
                    run.run("");
                    return;
                }

                String name = rs.getString("username");

                if (!p.getName().equals(name)) {
                    try (PreparedStatement ps = c.prepareStatement(PLAYER_UPDATE_USERNAME)) {
                        ps.setString(1, p.getName());
                        ps.setString(2, p.getUniqueId().toString());
                        ps.executeUpdate();
                    }
                }

                // Query returned data; give username from database
                onlinePlayerCache.put(p.getUniqueId(), new Cache(p, rs));
                run.run(name);
            } catch (SQLException ex) {
                ex.printStackTrace();
                // Fetch failed: null
                run.run(null);
            }
        });
    }

    /**
     *
     * @param uuid UUID of player to look up
     * @param first True = First join, false = last seen
     * @param run Parameters consist of: Timestamp time, String[] {username, uuid, timezone}
     */
    public void getSeen(UUID uuid, boolean first, ReturnRunnable.More<Timestamp, String> run) {
        Cache cache = onlinePlayerCache.get(uuid);
        if (cache != null && first) {
            run.run(
                    cache.firstseen,
                    cache.name,
                    uuid.toString(),
                    cache.timezone
            );
            return;
        }
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                ResultSet rs;
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT_BY_UUID)) {
                    ps.setString(1, uuid.toString());
                    rs = ps.executeQuery();
                }
                if (rs.next()) {
                    run.run(
                            rs.getTimestamp(first ? "firstseen" : "lastseen"),
                            rs.getString("username"),
                            rs.getString("uuid"),
                            rs.getString("timezone")
                    );
                } else {
                    run.run(null, (String[])null);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    /**
     * @param name Name of player to look up
     * @see #getSeen(UUID, boolean, ReturnRunnable.More)
     */
    public void getSeen(String name, boolean first, ReturnRunnable.More<Timestamp, String> run) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                ResultSet rs;
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT_BY_USERNAME)) {
                    ps.setString(1, name.toUpperCase());
                    rs = ps.executeQuery();
                }
                if (rs.next()) {
                    run.run(
                            rs.getTimestamp(first ? "firstseen" : "lastseen"),
                            rs.getString("username"),
                            rs.getString("uuid"),
                            rs.getString("timezone")
                    );
                } else {
                    run.run(null, (String[])null);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void playerLeave(UUID uuid) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(PLAYER_UPDATE_LAST_SEEN)) {
                    ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        onlinePlayerCache.remove(uuid);
    }

    public void getLatestNews(ReturnRunnable<String> run) {
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

    /**
     * Adds all players on database to toUpdate set
     * @param toUpdate Set to add players to
     */
    public void getAllPlayers(Set<String> toUpdate) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT_ALL_USERNAME)) {
                    ResultSet rs = ps.executeQuery();

                    while(rs.next())
                        toUpdate.add(rs.getString("username"));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    private class Cache {
        private final String name;
        private final Timestamp firstseen;
        final String timezone;
        final int options;

        private Cache(ProxiedPlayer p, ResultSet rs) throws SQLException {
            this.name = p.getName();
            this.firstseen = rs.getTimestamp("firstseen");
            this.timezone = rs.getString("timezone"); // physical server location
            this.options = rs.getInt("options");
        }

        private Cache(ProxiedPlayer p) {
            this.name = p.getName();
            this.firstseen = new Timestamp(System.currentTimeMillis());
            this.timezone = null;
            this.options = 0;
        }
    }
    // Cache-based methods below

    /**
     * WARNING!!! When looking for player not currently online, it check database
     * SYNCHRONOUSLY, meaning it will hold the thread until the result is fetched.
     * @param uuid Player's UUID to search
     * @return Timezone in string format
     */
    public String getTimeZone(UUID uuid) {
        Cache c = onlinePlayerCache.get(uuid);
        if (c != null) {
            return c.timezone;
        }

        // If player is not online
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(PLAYER_SELECT_TIMEZONE_BY_UUID)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next())
                    return rs.getString("timezone");
                else
                    return null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}