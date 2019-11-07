package org.samo_lego.simpleauth.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.UUID;

/**
 * @author sqlitetutorial.net
 */

public class SimpleAuthDatabase {
    private static final Logger LOGGER = LogManager.getLogger();
    /*public static void connect() {
        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:mods/SimpleAuth/players.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);

            // Creating database table
            String sql = "CREATE TABLE IF NOT EXISTS users (\n" +
                    "  `UserID`    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "  `UUID`      BINARY(16)  NOT NULL,\n" +
                    "  `Username`  VARCHAR(16) NOT NULL,\n" +
                    "  `Password`  VARCHAR(64) NOT NULL,\n" +
                    "  UNIQUE (`UUID`)\n" +
                    ");";
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            //Main?
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }*/
    private static Connection connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:mods/SimpleAuth/players.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            // Creating database table if it doesn't exist yet
            String sql = "CREATE TABLE IF NOT EXISTS users (\n" +
                    "  `UserID`    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "  `UUID`      BINARY(16)  NOT NULL,\n" +
                    "  `Username`  VARCHAR(16) NOT NULL,\n" +
                    "  `Password`  VARCHAR(64) NOT NULL,\n" +
                    "  UNIQUE (`UUID`)\n" +
                    ");";
            Statement stmt = conn.createStatement();
            stmt.execute(sql);

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return conn;
    }

    public static void insert(String uuid, String username, String password) {
        String sql = "INSERT INTO users(uuid, username, password) VALUES(?,?,?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private static void disconnect(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    public static void main() {
        Connection conn = connect();
    }

}
