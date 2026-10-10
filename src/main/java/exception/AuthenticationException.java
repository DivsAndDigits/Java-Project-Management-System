package exception;

/**
 * Exception thrown when authentication or authorization checks fail.
 */
public class AuthenticationException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
