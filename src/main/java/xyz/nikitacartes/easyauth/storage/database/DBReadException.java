package xyz.nikitacartes.easyauth.storage.database;

/** Thrown when a DB read fails (as opposed to "no such user" = null), forcing the caller to fail closed. */
public class DBReadException extends RuntimeException {
    public DBReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
