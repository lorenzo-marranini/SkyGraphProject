package it.unipi.SkyGraph.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing parameter",
                "message", "Required parameter '" + ex.getParameterName() + "' is missing"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid request",
                "message", ex.getMessage()
        ));
    }
}