package exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends BaseException {
    
    public static final String ERROR_CODE = "AUTH_FAILED";
    
    /**
     * Constructs a new AuthenticationException with the specified detail message.
     *
     * @param message the detail message
     */
    public AuthenticationException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new AuthenticationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}