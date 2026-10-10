package exception;

/**
 * [OOP Concept: Exception Handling]
 * [OOP Concept: Inheritance]
 * Exception thrown when a user attempts an action outside their granted role permissions
 * or privileges. Subclass of ApplicationException.
 */
public class UnauthorizedAccessException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String username, String actionAttempted) {
        super(String.format("User '%s' is not authorized to perform action: %s", username, actionAttempted));
    }
}
