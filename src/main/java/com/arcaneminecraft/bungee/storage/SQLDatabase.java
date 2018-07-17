package com.arcaneminecraft.bungee.storage;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.ReturnRunnable;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

/**
 * SQL Database must be MariaDB.
 * Stores: String uuid, String username, Timestamp firstseen, Timestamp lastseen, String timezone, int options
 */
public class SQLDatabase {
    private static final String PLAYER_INSERT = "INSERT INTO ab_players(uuid, username) VALUES(?, ?)";
    private static final String PLAYER_SELECT_BY_UUID = "SELECT * FROM ab_players WHERE uuid=? LIMIT 1";
    //private static final String PLAYER_SELECT_BY_USERNAME = "SELECT * FROM ab_players WHERE UPPER(username)=? LIMIT 1";
    private static final String PLAYER_SELECT_ALL_USERNAME_AND_UUID = "SELECT username,uuid FROM ab_players";
    //private static final String PLAYER_SELECT_ALL_UUID_BY_USERNAME = "SELECT uuid FROM ab_players WHERE UPPER(username)=?";
    private static final String PLAYER_SELECT_TIMEZONE_BY_UUID = "SELECT timezone FROM ab_players WHERE uuid=?";
    private static final String PLAYER_UPDATE_USERNAME = "UPDATE ab_players SET username=? WHERE uuid=?";
    private static final String PLAYER_UPDATE_LAST_SEEN_AND_OPTIONS_AND_TIMEZONE = "UPDATE ab_players SET lastseen=?,options=?,timezone=?  WHERE uuid=?";
    private static final String NEWS_SELECT_LATEST = "SELECT * FROM ab_news ORDER BY id DESC LIMIT 1";
    private static final String NEWS_INSERT_NEWS = "INSERT INTO ab_news(content, username, uuid) VALUES(?, ?, ?)";

    private final ArcaneBungee plugin;
    private static SQLDatabase instance;
    private final HashMap<UUID, Cache> onlinePlayerCache;
    private final HashMap<String, UUID> allNameToUuid;
    private final HashMap<UUID, String> allUuidToName;
    private final MariaDbPoolDataSource ds;


    public SQLDatabase(ArcaneBungee plugin) throws SQLException {
        SQLDatabase.instance = this;
        this.plugin = plugin;
        this.onlinePlayerCache = new HashMap<>();
        this.allNameToUuid = new HashMap<>();
        this.allUuidToName = new HashMap<>();

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

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT_ALL_USERNAME_AND_UUID)) {
                    ResultSet rs = ps.executeQuery();

                    while(rs.next()) {
                        String n = rs.getString("username");
                        UUID u = UUID.fromString(rs.getString("uuid"));
                        allNameToUuid.put(n.toLowerCase(), u);
                        allUuidToName.put(u, n);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

    }

    public UUID getPlayerUUID(String name) {
        return allNameToUuid.get(name.toLowerCase());
    }

    public String getPlayerName(UUID uuid) {
        return allUuidToName.get(uuid);
    }

    public void playerJoinThen(ProxiedPlayer p, ReturnRunnable<String> run) {
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
                    allUuidToName.put(p.getUniqueId(), p.getName());
                    allNameToUuid.put(p.getName(), p.getUniqueId());

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
                    // Username changed
                    allUuidToName.put(p.getUniqueId(), p.getName());
                    allNameToUuid.remove(name);
                    allNameToUuid.put(p.getName(), p.getUniqueId());
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
    public void getSeenThen(UUID uuid, boolean first, ReturnRunnable.More<Timestamp, String[]> run) {
        Cache cache = onlinePlayerCache.get(uuid);
        if (cache != null && first) {
            run.run(
                    cache.firstseen,
                    new String[]{
                            cache.name,
                            uuid.toString(),
                            cache.timezone
                    }
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
                            new String[]{
                                    rs.getString("username"),
                                    rs.getString("uuid"),
                                    rs.getString("timezone")
                            }
                    );
                } else {
                    run.run(null, null);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void playerLeave(UUID uuid) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            Cache cache = onlinePlayerCache.get(uuid);
            onlinePlayerCache.remove(uuid);
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(PLAYER_UPDATE_LAST_SEEN_AND_OPTIONS_AND_TIMEZONE)) {
                    ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps.setInt(2, cache.options);
                    ps.setString(3, cache.timezone);
                    ps.setString(4, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
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
     * Gets pre-loaded all players on database to toUpdate set
     */
    public Collection<String> getAllPlayerName() {
        return allUuidToName.values();
    }

    int getOption(ProxiedPlayer player) {
        return onlinePlayerCache.get(player.getUniqueId()).options;
    }

    void setOption(ProxiedPlayer player, int option) {
        onlinePlayerCache.get(player.getUniqueId()).options = option;
    }

    public static String getTimeZoneCache(ProxiedPlayer p) {
        if (p == null)
            return "America/Toronto"; // Default timezone of the server
        return instance.onlinePlayerCache.get(p.getUniqueId()).timezone;
    }

    public static TimeZone setTimeZoneCache(ProxiedPlayer p, String timeZone) {
        instance.onlinePlayerCache.get(p.getUniqueId()).timezone = timeZone;
        return TimeZone.getTimeZone(timeZone);
    }

    /**
     * WARNING!!! When looking for player not currently online, it check database
     * SYNCHRONOUSLY, meaning it will hold the thread until the result is fetched.
     * @param uuid Player's UUID to search
     * @return Timezone in string format
     */
    public String getTimeZoneSync(UUID uuid) {
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

    private class Cache {
        private final String name;
        private final Timestamp firstseen;
        private String timezone;
        private int options;

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
}