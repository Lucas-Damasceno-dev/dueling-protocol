package exception;

/**
 * Exception thrown when a user is not authenticated.
 */
public class UserNotAuthenticatedException extends BaseException {
    
    public static final String ERROR_CODE = "USER_NOT_AUTHENTICATED";
    
    /**
     * Constructs a new UserNotAuthenticatedException with the specified detail message.
     *
     * @param message the detail message
     */
    public UserNotAuthenticatedException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new UserNotAuthenticatedException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public UserNotAuthenticatedException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}