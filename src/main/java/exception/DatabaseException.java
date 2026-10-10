package exception;

/**
 * Exception thrown when a persistence or JDBC database operation encounters a failure.
 */
public class DatabaseException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
