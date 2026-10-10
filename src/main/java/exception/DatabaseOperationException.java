package exception;

/**
 * [OOP Concept: Exception Handling]
 * [OOP Concept: Inheritance]
 * Exception thrown when a specific database or JDBC operation fails.
 * Wraps lower-level SQLExceptions to avoid exposing internal schema details to end users.
 */
public class DatabaseOperationException extends DatabaseException {
    private static final long serialVersionUID = 1L;

    public DatabaseOperationException(String userFriendlyMessage) {
        super(userFriendlyMessage);
    }

    public DatabaseOperationException(String userFriendlyMessage, Throwable cause) {
        super(userFriendlyMessage, cause);
    }
}
