package org.samo_lego.simpleauth.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.simpleauth.SimpleAuth;

import java.sql.*;

/**
 * Thanks to
 * @author sqlitetutorial.net
 */

public class SimpleAuthDatabase {
    private static final Logger LOGGER = LogManager.getLogger();

    // Connects to the DB
    private Connection conn;

    public void openConnection() {
        // SQLite connection string
        String url = "jdbc:sqlite:" + SimpleAuth.gameDirectory + "/mods/SimpleAuth/players.db";
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            LOGGER.error("[SimpleAuth] " + e.getMessage());
        }
    }
    // Closing connection
    public void close() {
        if (conn != null) {
            try {
                conn.close();
                LOGGER.info("[SimpleAuth] Database connection closed successfully.");
            } catch (SQLException e) {
                LOGGER.info("[SimpleAuth] Error: " + e.getMessage());
            }
        }
    }

    // If the mod runs for the first time, we need to create the DB table
    public void makeTable() {
        try {
            // Creating database table if it doesn't exist yet
            String sql = "CREATE TABLE IF NOT EXISTS users (\n" +
                    "  `UserID`    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "  `UUID`      BINARY(16)  NOT NULL,\n" +
                    "  `Username`  VARCHAR(16) NOT NULL,\n" +
                    "  `Password`  VARCHAR(64) NOT NULL,\n" +
                    "  UNIQUE (`UUID`)\n" +
                    "  UNIQUE (`Username`)\n" +
                    ");";
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            LOGGER.error("[SimpleAuth] Error: " + e.getMessage());
        }
    }

    // When player registers, we insert the data into DB
    public boolean registerUser(String uuid, String username, String password) {
        String sql = "INSERT INTO users(uuid, username, password) VALUES(?,?,?)";
        String sqlCheck = "SELECT UUID "
                + "FROM users WHERE UUID = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
            PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheck)) {

            pstmtCheck.setString(1, uuid);
            ResultSet rs  = pstmtCheck.executeQuery();

            // Getting the password
            //String dbUuid = null;
            try {
                rs.getString("UUID");
                return false;
            } catch(SQLException ignored) {
                // User is not registered
            } finally {
                pstmt.setString(1, uuid);
                pstmt.setString(2, username);
                pstmt.setString(3, password);

                pstmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            LOGGER.error("[SimpleAuth] Register error: " + e.getMessage());
            return false;
        }
    }

    // Deletes row containing the username provided
    public void delete(String uuid, String username) {
        String sql = "DELETE FROM users WHERE uuid = ? OR username = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the corresponding param
            pstmt.setString(1, uuid);
            pstmt.setString(2, username);
            // execute the delete statement
            pstmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.error("[SimpleAuth] " + e.getMessage());
        }
    }

    // Updates the password of the user
    public void update(String uuid, String username, String pass) {
        String sql = "UPDATE users SET password = ? "
                + "WHERE uuid = ? OR username = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the corresponding param
            pstmt.setString(1, pass);
            pstmt.setString(2, uuid);
            pstmt.setString(3, username);

            // update
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[SimpleAuth] " + e.getMessage());
        }
    }

    // Gets the hashed password from DB
    public String getPassword(String uuid){
        String sql = "SELECT UUID, Password "
                + "FROM users WHERE UUID = ?";
        String pass = null;

        try (PreparedStatement pstmt  = conn.prepareStatement(sql)) {
            // Setting statement
            pstmt.setString(1, uuid);
            ResultSet rs  = pstmt.executeQuery();

            // Getting the password
            pass = rs.getString("Password");
        } catch (SQLException e) {
            LOGGER.error("[SimpleAuth] Error getting password: " + e.getMessage());
        }
        return pass;
    }
}