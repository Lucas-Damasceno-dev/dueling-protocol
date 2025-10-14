package exception;

import dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;



/**
 * Global exception handler for the dueling protocol application.
 * Centralizes error handling and provides consistent error responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles BaseException and its subclasses.
     *
     * @param ex the BaseException
     * @param request the WebRequest
     * @return ResponseEntity with standardized error response
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, WebRequest request) {
        logger.error("BaseException occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), ex.getErrorCode());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        HttpStatus status = HttpStatus.BAD_REQUEST; // Default status
        
        // Map specific error codes to HTTP status codes
        if (ex instanceof UserNotFoundException || ex instanceof UserNotAuthenticatedException) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ResourceConflictException) {
            status = HttpStatus.CONFLICT;
        } else if (ex instanceof AuthenticationException) {
            status = HttpStatus.UNAUTHORIZED;
        }
        
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles IllegalArgumentException.
     *
     * @param ex the IllegalArgumentException
     * @param request the WebRequest
     * @return ResponseEntity with standardized error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.error("IllegalArgumentException occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse("Invalid argument provided", "INVALID_ARGUMENT");
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles BadCredentialsException from Spring Security.
     *
     * @param ex the BadCredentialsException
     * @param request the WebRequest
     * @return ResponseEntity with standardized error response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        logger.warn("Bad credentials provided: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse("Invalid username or password", "BAD_CREDENTIALS");
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles generic Exception (fallback for unexpected errors).
     *
     * @param ex the Exception
     * @param request the WebRequest
     * @return ResponseEntity with standardized error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse("An unexpected error occurred", "INTERNAL_ERROR");
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}