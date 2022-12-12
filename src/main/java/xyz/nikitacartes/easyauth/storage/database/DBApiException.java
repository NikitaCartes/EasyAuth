package xyz.nikitacartes.easyauth.storage.database;

/**
 * Generic exception wrapping errors from DB
 */
public class DBApiException extends Exception {

    public DBApiException(String errorMessage, Exception e) {
        super("[EasyAuth] " + errorMessage, e);
    }
}
