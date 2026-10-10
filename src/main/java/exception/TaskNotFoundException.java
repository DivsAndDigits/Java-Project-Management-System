package exception;

/**
 * [OOP Concept: Exception Handling]
 * [OOP Concept: Inheritance]
 * Exception thrown when a specific task work item cannot be found.
 * Subclass of ResourceNotFoundException.
 */
public class TaskNotFoundException extends ResourceNotFoundException {
    private static final long serialVersionUID = 1L;

    public TaskNotFoundException(Long taskId) {
        super(String.format("Task with ID %d was not found.", taskId));
    }

    public TaskNotFoundException(String taskTitle) {
        super(String.format("Task with title '%s' was not found.", taskTitle));
    }

    public TaskNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
