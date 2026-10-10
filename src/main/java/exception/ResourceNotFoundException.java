package exception;

/**
 * Exception thrown when a requested resource (e.g., Project, Task, User) cannot be found.
 */
public class ResourceNotFoundException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s with identifier '%s' was not found.", resourceName, identifier));
    }
}
