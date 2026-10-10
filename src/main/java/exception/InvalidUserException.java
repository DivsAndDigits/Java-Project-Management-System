package exception;

/**
 * [OOP Concept: Exception Handling]
 * [OOP Concept: Inheritance]
 * Exception thrown when user data validation fails or an invalid user state is detected.
 * Subclass of ValidationException.
 */
public class InvalidUserException extends ValidationException {
    private static final long serialVersionUID = 1L;

    public InvalidUserException(String message) {
        super(message);
    }

    public InvalidUserException(String field, String reason) {
        super(String.format("Invalid user property '%s': %s", field, reason));
    }
}
