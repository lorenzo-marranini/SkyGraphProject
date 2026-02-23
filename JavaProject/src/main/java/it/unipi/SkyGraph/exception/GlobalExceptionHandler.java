package it.unipi.SkyGraph.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/** Global exception handler that intercepts controller-level exceptions and returns structured JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles type mismatch errors on request parameters. For enum parameters, lists all
     * accepted values in the response; for other types, reports the invalid parameter name and value.
     *
     * @param ex exception carrying the parameter name, the supplied value, and the expected type
     * @return 400 Bad Request with an {@code error} and {@code message} body
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleEnumMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> requiredType = ex.getRequiredType();
        String message;

        if (requiredType != null && requiredType.isEnum()) {
            Object[] constants = requiredType.getEnumConstants();
            String validValues = java.util.Arrays.stream(constants)
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));
            message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() +
                      "'. Accepted values: " + validValues;
        } else {
            message = "Invalid parameter '" + ex.getName() + "': " + ex.getValue();
        }

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid request",
                "message", message
        ));
    }

    /**
     * Handles missing required request parameters, returning the name of the absent parameter.
     *
     * @param ex exception carrying the missing parameter name and type
     * @return 400 Bad Request with an {@code error} and {@code message} body
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing parameter",
                "message", "Required parameter '" + ex.getParameterName() + "' is missing"
        ));
    }

    /**
     * Handles {@link IllegalArgumentException} thrown by service or validation logic,
     * forwarding the exception message directly to the client.
     *
     * @param ex exception carrying a human-readable error description
     * @return 400 Bad Request with an {@code error} and {@code message} body
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid request",
                "message", ex.getMessage()
        ));
    }
}