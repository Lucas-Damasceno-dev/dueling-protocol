package exception;

/**
 * Exception thrown when a resource conflict occurs (e.g., duplicate username).
 */
public class ResourceConflictException extends BaseException {
    
    public static final String ERROR_CODE = "RESOURCE_CONFLICT";
    
    /**
     * Constructs a new ResourceConflictException with the specified detail message.
     *
     * @param message the detail message
     */
    public ResourceConflictException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new ResourceConflictException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ResourceConflictException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}