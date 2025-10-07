package dto;

/**
 * Data Transfer Object for authentication responses.
 * Contains the information returned after a successful authentication.
 */
public class AuthenticationResponse {
    private String token;
    private String message;
    private String error;

    // Default constructor
    public AuthenticationResponse() {
    }

    // Constructor for successful authentication
    public AuthenticationResponse(String token, String message) {
        this.token = token;
        this.message = message;
    }

    // Constructor for message responses
    public AuthenticationResponse(String message) {
        this.message = message;
    }

    // Constructor for error responses
    public AuthenticationResponse(String error, boolean isError) {
        if (isError) {
            this.error = error;
        } else {
            this.message = error;
        }
    }

    // Getters and setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "AuthenticationResponse{" +
                "token='" + (token != null ? "[PROVIDED]" : "null") + '\'' +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}