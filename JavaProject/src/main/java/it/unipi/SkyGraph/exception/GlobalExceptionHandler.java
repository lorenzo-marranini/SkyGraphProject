//package it.unipi.SkyGraph.exception;
//
//import org.springframework.core.annotation.Order;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
//
//@RestControllerAdvice
//@Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
//        return ResponseEntity.badRequest().body(ex.getCause().getMessage());
//    }
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
//        return ResponseEntity.badRequest().body(ex.getMessage());
//    }
//
//    @ExceptionHandler(AccessDeniedException.class)
//    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
//        throw ex;
//    }
//
//    @ExceptionHandler(AuthenticationException.class)
//    public void handleAuthentication(AuthenticationException ex) throws AuthenticationException {
//        throw ex;
//    }
//
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
//    }
//}
