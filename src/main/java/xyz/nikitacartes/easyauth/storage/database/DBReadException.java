package xyz.nikitacartes.easyauth.storage.database;

/** Брошено, когда чтение БД упало (в отличие от "юзера нет" = null). Заставляет вызывающего fail-close. */
public class DBReadException extends RuntimeException {
    public DBReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
