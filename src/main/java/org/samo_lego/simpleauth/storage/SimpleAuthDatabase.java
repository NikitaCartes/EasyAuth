package org.samo_lego.simpleauth.storage;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.samo_lego.simpleauth.SimpleAuth;

import java.io.File;
import java.io.IOException;

import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logError;
import static org.samo_lego.simpleauth.utils.SimpleLogger.logInfo;

public class SimpleAuthDatabase {
    private DB levelDBStore;

    public DB getLevelDBStore() {
        return this.levelDBStore;
    }

    // Connects to the DB
    public void openConnection() {
        try {

            Options options = new Options();
            levelDBStore = factory.open(new File(SimpleAuth.gameDirectory + "/mods/SimpleAuth/levelDBStore"), options);
        } catch (Error | IOException e) {
            logError(e.getMessage());
        }
    }
    // Closing connection
    public void close() {
        if (levelDBStore != null) {
            try {
                levelDBStore.close();
                logInfo("Database connection closed successfully.");
            } catch (Error | IOException e) {
                logError(e.getMessage());
            }
        }
    }

    // Tells whether db connection is closed
    public boolean isClosed() {
        return levelDBStore == null;
    }


    // When player registers, we insert the data into DB
    public boolean registerUser(String uuid, String data) {
        try {
            if(!this.isUserRegistered(uuid)) {
                levelDBStore.put(bytes("UUID:" + uuid), bytes("data:" + data));
                return true;
            }
            return false;
        } catch (Error e) {
            logError("Register error: " + e.getMessage());
            return false;
        }
    }

    // Checks if user is registered
    public boolean isUserRegistered(String uuid) {
        try {
            return levelDBStore.get(bytes("UUID:" + uuid)) != null;
        } catch (DBException e) {
            logError(e.getMessage());
        }
        return false;
    }

    // Deletes row containing the username provided
    public void deleteUserData(String uuid) {
        try {
            levelDBStore.delete(bytes("UUID:" + uuid));
        } catch (Error e) {
            logError(e.getMessage());
        }
    }

    // Updates the password of the user
    public void updateUserData(String uuid, String data) {
        try {
            levelDBStore.put(bytes("UUID:" + uuid), bytes("data:" + data));
        } catch (Error e) {
            logError(e.getMessage());
        }
    }

    // Gets the hashed password from DB
    public String getData(String uuid){
        try {
            if(this.isUserRegistered(uuid))  // Gets password from db and removes "data:" prefix from it
                return new String(levelDBStore.get(bytes("UUID:" + uuid))).substring(5);
        } catch (Error e) {
            logError("Error getting password: " + e.getMessage());
        }
        return "";
    }
}