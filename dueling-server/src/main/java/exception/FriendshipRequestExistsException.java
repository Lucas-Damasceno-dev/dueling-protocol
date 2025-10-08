package exception;

/**
 * Exception thrown when a friendship request already exists.
 */
public class FriendshipRequestExistsException extends BaseException {
    
    public static final String ERROR_CODE = "FRIENDSHIP_REQUEST_EXISTS";
    
    /**
     * Constructs a new FriendshipRequestExistsException with the specified detail message.
     *
     * @param message the detail message
     */
    public FriendshipRequestExistsException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new FriendshipRequestExistsException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public FriendshipRequestExistsException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}