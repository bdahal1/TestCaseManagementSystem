package com.tcms.errors;

import com.tcms.helper.pojo.CustomResponseMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Date;

@RestControllerAdvice
public class ApiExceptionHandler {
    private ResponseEntity<CustomResponseMessage> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new CustomResponseMessage(new Date(), status.getReasonPhrase(), message));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomResponseMessage> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).sorted().distinct()
                .collect(java.util.stream.Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message);
    }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<CustomResponseMessage> invalid(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request parameters or body.");
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CustomResponseMessage> forbidden(AccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "You do not have access to this resource.");
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<CustomResponseMessage> status(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new CustomResponseMessage(new Date(), "Request failed", e.getReason()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CustomResponseMessage> conflict(DataIntegrityViolationException e) {
        return error(HttpStatus.CONFLICT, "This change conflicts with existing data.");
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<CustomResponseMessage> oversized(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Evidence files must be 5 MB or smaller.");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomResponseMessage> unexpected(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof DataIntegrityViolationException) return conflict((DataIntegrityViolationException) cause);
            cause = cause.getCause();
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Request failed", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the operation.");
    }
}
