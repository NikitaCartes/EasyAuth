package org.samo_lego.simpleauth.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.samo_lego.simpleauth.SimpleAuth;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

public class SimpleAuthDatabase {
    private static final Logger LOGGER = LogManager.getLogger();
    private DB levelDBStore;

    // Connects to the DB
    public void openConnection() {
        try {

            Options options = new Options();
            levelDBStore = factory.open(new File(SimpleAuth.gameDirectory + "/mods/SimpleAuth/levelDBStore"), options);
        } catch (Error | IOException e) {
            LOGGER.error("[SimpleAuth] " + e.getMessage());
        }
    }
    // Closing connection
    public void close() {
        if (levelDBStore != null) {
            try {
                levelDBStore.close();
                LOGGER.info("[SimpleAuth] Database connection closed successfully.");
            } catch (Error | IOException e) {
                LOGGER.info("[SimpleAuth] Error: " + e.getMessage());
            }
        }
    }

    // When player registers, we insert the data into DB
    public boolean registerUser(String uuid, String password) {
        System.out.println(Arrays.toString(levelDBStore.get(bytes("UUID:" + uuid))));
        try {
            if(levelDBStore.get(bytes("UUID:" + uuid)) == null) {
                levelDBStore.put(bytes("UUID:" + uuid), bytes("password:" + password));
                return true;
            }
            return false;
        } catch (Error e) {
            LOGGER.error("[SimpleAuth] Register error: " + e.getMessage());
            return false;
        }
    }

    // Deletes row containing the username provided
    public void delete(String uuid) {
        try {
            levelDBStore.delete(bytes("UUID:" + uuid));
        } catch (Error e) {
            LOGGER.error("[SimpleAuth] " + e.getMessage());
        }
    }

    // Updates the password of the user
    public void update(String uuid, String password) {
        try {
            levelDBStore.put(bytes("UUID:" + uuid),bytes("password:" + password));
        } catch (Error e) {
            LOGGER.error("[SimpleAuth] " + e.getMessage());
        }
    }

    // Gets the hashed password from DB
    public String getPassword(String uuid){
        String password = null;
        try {
            // Gets password from db and removes "password:" prefix from it
            password = new String(levelDBStore.get(bytes("UUID:" + uuid))).substring(9);
        } catch (Error e) {
            LOGGER.error("[SimpleAuth] Error getting password: " + e.getMessage());
        }
        return password;
    }
}