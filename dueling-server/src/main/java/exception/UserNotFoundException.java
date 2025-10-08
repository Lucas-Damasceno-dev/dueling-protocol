package exception;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends BaseException {
    
    public static final String ERROR_CODE = "USER_NOT_FOUND";
    
    /**
     * Constructs a new UserNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public UserNotFoundException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new UserNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public UserNotFoundException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}