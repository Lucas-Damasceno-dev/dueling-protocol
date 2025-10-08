package exception;

/**
 * Exception thrown when invalid input is provided.
 */
public class InvalidInputException extends BaseException {
    
    public static final String ERROR_CODE = "INVALID_INPUT";
    
    /**
     * Constructs a new InvalidInputException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidInputException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new InvalidInputException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public InvalidInputException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}