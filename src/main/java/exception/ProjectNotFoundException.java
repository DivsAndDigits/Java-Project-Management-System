package exception;

/**
 * [OOP Concept: Exception Handling]
 * [OOP Concept: Inheritance]
 * Exception thrown when a requested Project does not exist in the system.
 * Subclass of ResourceNotFoundException.
 */
public class ProjectNotFoundException extends ResourceNotFoundException {
    private static final long serialVersionUID = 1L;

    public ProjectNotFoundException(Long projectId) {
        super(String.format("Project with ID %d was not found.", projectId));
    }

    public ProjectNotFoundException(String projectCode) {
        super(String.format("Project with code '%s' was not found.", projectCode));
    }

    public ProjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
