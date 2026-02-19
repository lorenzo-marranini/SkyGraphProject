//package it.unipi.SkyGraph.exception;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
//import io.swagger.v3.oas.annotations.Hidden;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@ControllerAdvice
//@Hidden
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
//        Map<String, Object> body = new HashMap<>();
//
//        // Se l'errore viene dal tuo Converter, prendiamo il messaggio personalizzato
//        String message = (ex.getCause() != null && ex.getCause().getCause() != null)
//                ? ex.getCause().getCause().getMessage()
//                : ex.getMessage();
//
//        body.put("error", "Bad Request");
//        body.put("message", message);
//        body.put("parameter", ex.getName());
//
//        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
//    }
//}
