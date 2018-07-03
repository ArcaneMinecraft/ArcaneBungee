package com.arcaneminecraft.bungee.storage;

import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.*;
import java.text.SimpleDateFormat;

/**
 * SQL Database must be MariaDB.
 * Stores: String uuid, String username, Date firstseen, Date lastseen, boolean greylist, boolean discord
 */
public class SQLDatabase implements Listener {
    private static final String PLAYER_INSERT = "INSERT INTO ab_players(uuid, username) VALUES(?, ?)";
    private static final String PLAYER_SELECT = "SELECT * FROM ab_players WHERE uuid=? LIMIT 1";
    private static final String PLAYER_SELECT_ALL_UUID_BY_USERNAME = "SELECT uuid FROM ab_players WHERE username=?";
    private static final String PLAYER_UPDATE_USERNAME = "UPDATE ab_players SET username=? WHERE uuid=?";
    private static final String PLAYER_UPDATE_LAST_SEEN = "UPDATE ab_players SET lastseen=? WHERE uuid=?";

    private final ArcaneBungee plugin;
    private final MariaDbPoolDataSource ds;


    private PreparedStatement statement;

    public SQLDatabase(ArcaneBungee plugin) throws SQLException {
        this.plugin = plugin;
        String url = "jdbc:mariadb://localhost/arcanebungee-test";
        String user = "arcanebungee-test";
        String pass = "test";
        this.ds = new MariaDbPoolDataSource(url);
        ds.setUser(user);
        ds.setPassword(pass);
    }

    @EventHandler
    public void playerJoin(PostLoginEvent e) {
        ProxiedPlayer p = e.getPlayer();

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                ResultSet rs;
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT)) {
                    ps.setString(1, p.getUniqueId().toString());
                    rs = ps.executeQuery();
                }

                if (!rs.next()) {
                    // No data = new player
                    try (PreparedStatement ps = c.prepareStatement(PLAYER_INSERT)) {
                        ps.setString(1, p.getUniqueId().toString());
                        ps.setString(2, p.getName());
                        rs = ps.executeQuery();
                    }
                    plugin.getLogger().info("First timer");
                    return;
                }

                String s = rs.getString("username");
                Timestamp date = rs.getTimestamp("firstseen");

                // Over a week
                String d = new SimpleDateFormat("yyyy-MM-dd").format(date);
                String t = new SimpleDateFormat("HH:mm z").format(date);

                String text = "Player " + s +
                        "first joined on " +
                        d +
                        " at " +
                        t;
                plugin.getLogger().info(text);

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

/*    public void test() {
        Connection conn = null;
        PreparedStatement stmt = null;
        try{
            //STEP 3: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(url,user,pass);

            //STEP 4: Execute a query
            System.out.println("Creating statement...");
            stmt = conn.prepareStatement("SELECT id, first, last, age FROM Employees");
            stmt.setString(1, "omg");
            ResultSet rs = stmt.executeQuery();

            //STEP 5: Extract data from result set
            while(rs.next()){
                //Retrieve by column name
                int id  = rs.getInt("id");
                int age = rs.getInt("age");
                String first = rs.getString("first");
                String last = rs.getString("last");

                //Display values
                System.out.print("ID: " + id);
                System.out.print(", Age: " + age);
                System.out.print(", First: " + first);
                System.out.println(", Last: " + last);
            }
            //STEP 6: Clean-up environment
            rs.close();
            stmt.close();
            conn.close();
        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }finally{
            //finally block used to close resources
            try{
                if(stmt!=null)
                    stmt.close();
            }catch(SQLException se2){
            }// nothing we can do
            try{
                if(conn!=null)
                    conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }//end finally try
        }//end try
        System.out.println("Goodbye!");
    }//end main*/
}