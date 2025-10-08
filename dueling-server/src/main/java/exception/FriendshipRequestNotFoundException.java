package exception;

/**
 * Exception thrown when a friendship request is not found.
 */
public class FriendshipRequestNotFoundException extends BaseException {
    
    public static final String ERROR_CODE = "FRIENDSHIP_REQUEST_NOT_FOUND";
    
    /**
     * Constructs a new FriendshipRequestNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public FriendshipRequestNotFoundException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new FriendshipRequestNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public FriendshipRequestNotFoundException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}