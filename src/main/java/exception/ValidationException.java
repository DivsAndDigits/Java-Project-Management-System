package exception;

/**
 * Exception thrown when business validation fails for user inputs or entity states.
 */
public class ValidationException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
