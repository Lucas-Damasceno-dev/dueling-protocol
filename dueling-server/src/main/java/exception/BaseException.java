package exception;

/**
 * Base exception class for the dueling protocol application.
 * All custom exceptions should extend this class.
 */
public abstract class BaseException extends RuntimeException {
    
    private final String errorCode;
    
    /**
     * Constructs a new BaseException with the specified detail message.
     *
     * @param message the detail message
     */
    public BaseException(String message) {
        super(message);
        this.errorCode = null;
    }
    
    /**
     * Constructs a new BaseException with the specified detail message and error code.
     *
     * @param message the detail message
     * @param errorCode the error code
     */
    public BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Constructs a new BaseException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }
    
    /**
     * Constructs a new BaseException with the specified detail message, error code, and cause.
     *
     * @param message the detail message
     * @param errorCode the error code
     * @param cause the cause
     */
    public BaseException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the error code associated with this exception.
     *
     * @return the error code, or null if no error code was provided
     */
    public String getErrorCode() {
        return errorCode;
    }
}