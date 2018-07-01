package com.arcaneminecraft.bungee.storage;

import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.*;

/**
 * SQL Database must be MariaDB.
 * Stores: uuid, name, firstseen, lastseen
 */
public class SQLDatabase {
    private static final String PLAYER_INSERT = "INSERT INTO data(uuid, username, firstseen) VALUES(?, ?, ?)";
    private static final String PLAYER_SELECT = "SELECT * FROM players WHERE uuid=? LIMIT 1";
    private static final String PLAYER_SELECT_ALL_UUID_BY_NAME = "SELECT uuid FROM players WHERE username=?";
    private static final String PLAYER_UPDATE_NAME = "UPDATE players SET username=? WHERE uuid=?";
    private static final String PLAYER_UPDATE_LAST_SEEN = "UPDATE players SET lastseen=? WHERE uuid=?";

    private final ArcaneBungee plugin;
    // JDBC driver name and database URL
    private final String url;

    //  Database credentials
    private final String user;
    private final String pass;
    private final MariaDbPoolDataSource ds;


    private PreparedStatement statement;

    SQLDatabase(ArcaneBungee plugin) throws SQLException {
        this.plugin = plugin;
        this.url = "jdbc:mariadb://localhost/arcanebungee-test";
        this.user = "arcanebungee-test";
        this.pass = "test";
        this.ds = new MariaDbPoolDataSource(url);
        ds.setUser(user);
        ds.setPassword(pass);
    }

    public void playerJoin(ProxiedPlayer p) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Connection c = ds.getConnection()) {
                ResultSet rs;
                try (PreparedStatement ps = c.prepareStatement(PLAYER_SELECT)) {
                    ps.setString(1, p.getUniqueId().toString());
                    ps.setString(2, p.getName());
                    ps.setLong(3, System.currentTimeMillis());
                    rs = ps.executeQuery();
                }



            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void test() {
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
    }//end main
}//end FirstExample