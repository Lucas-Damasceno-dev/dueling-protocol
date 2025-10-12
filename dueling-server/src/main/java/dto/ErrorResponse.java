package dto;

/**
 * Standardized error response DTO for consistent API error handling.
 */
public class ErrorResponse {
    private String error;
    private String errorCode;
    private String message;
    private long timestamp;
    private String path;

    /**
     * Default constructor.
     */
    public ErrorResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Constructor for error responses with message.
     *
     * @param message the message
     */
    public ErrorResponse(String message) {
        this();
        this.message = message;
    }

    /**
     * Constructor for error responses with error and error code.
     *
     * @param error the error message
     * @param errorCode the error code
     */
    public ErrorResponse(String error, String errorCode) {
        this();
        this.error = error;
        this.errorCode = errorCode;
    }

    /**
     * Constructor for error responses with error, error code, and path.
     *
     * @param error the error message
     * @param errorCode the error code
     * @param path the request path
     */
    public ErrorResponse(String error, String errorCode, String path) {
        this(error, errorCode);
        this.path = path;
    }

    // Getters and setters
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "error='" + error + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", path='" + path + '\'' +
                '}';
    }
}