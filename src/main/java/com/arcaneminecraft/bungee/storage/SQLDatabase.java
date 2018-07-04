package com.arcaneminecraft.bungee.storage;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.ReturnRunnable;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.*;
import java.util.Set;

/**
 * SQL Database must be MariaDB.
 * Stores: String uuid, String username, Date firstseen, Date lastseen, boolean greylist, boolean discord
 */
// TODO: Cache
public class SQLDatabase {
    private static final String PLAYER_INSERT = "INSERT INTO ab_players(uuid, username) VALUES(?, ?)";
    private static final String PLAYER_SELECT = "SELECT * FROM ab_players WHERE uuid=? LIMIT 1";
    private static final String PLAYER_SELECT_ALL_USERNAME = "SELECT username FROM ab_players";
    private static final String PLAYER_SELECT_ALL_UUID_BY_USERNAME = "SELECT uuid FROM ab_players WHERE username=?";
    private static final String PLAYER_UPDATE_USERNAME = "UPDATE ab_players SET username=? WHERE uuid=?";
    private static final String PLAYER_UPDATE_LAST_SEEN = "UPDATE ab_players SET lastseen=? WHERE uuid=?";

    private final ArcaneBungee plugin;
    private final MariaDbPoolDataSource ds;


    public SQLDatabase(ArcaneBungee plugin) throws SQLException {
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
    }

    public void checkName(ProxiedPlayer p, ReturnRunnable<String> run) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                ResultSet rs;
                // Get player info: player name
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT)) {
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
                run.run(name);
            } catch (SQLException ex) {
                ex.printStackTrace();
                // Fetch failed: null
                run.run(null);
            }
        });
    }

    public void updateLastSeen(String uuid) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(PLAYER_UPDATE_LAST_SEEN)) {
                    ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps.setString(2, uuid);
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    /**
     * Adds all players on database to toUpdate set.
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
}