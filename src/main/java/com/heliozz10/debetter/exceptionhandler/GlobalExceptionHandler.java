package com.heliozz10.debetter.exceptionhandler;

import com.heliozz10.debetter.service.util.media.FileStorageException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
//    private ResponseEntity<ApiErrorResponse> buildError(HttpStatus status, String message, WebRequest request) {
//        return ResponseEntity.status(status).body(
//                ApiErrorResponse.of(
//                        status.value(),
//                        status.getReasonPhrase(),
//                        message,
//                        request.getDescription(false).replace("uri=", "")
//                )
//        );
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
//            MethodArgumentNotValidException ex, WebRequest request
//    ) {
//        String message = ex.getBindingResult().getFieldErrors().stream()
//                .map(err -> err.getField() + ": " + err.getDefaultMessage())
//                .findFirst()
//                .orElse("Validation failed");
//        return buildError(HttpStatus.BAD_REQUEST, message, request);
//    }
//
//    @ExceptionHandler(IllegalStateException.class)
//    public ResponseEntity<ApiErrorResponse> handleIllegalState(
//            IllegalStateException ex, WebRequest request
//    ) {
//        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
//            ConstraintViolationException ex, WebRequest request
//    ) {
//        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
//    }
//
//    @ExceptionHandler(MissingServletRequestParameterException.class)
//    public ResponseEntity<ApiErrorResponse> handleMissingParam(
//            MissingServletRequestParameterException ex, WebRequest request
//    ) {
//        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
//    }
//
//    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
//    public ResponseEntity<ApiErrorResponse> handleNotFound(
//            RuntimeException ex, WebRequest request
//    ) {
//        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
//    }
//
//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
//            DataIntegrityViolationException ex, WebRequest request
//    ) {
//        return buildError(HttpStatus.CONFLICT, "Database constraint violation", request);
//    }
//
//    @ExceptionHandler(AuthenticationException.class)
//    public ResponseEntity<ApiErrorResponse> handleAuth(
//            AuthenticationException ex, WebRequest request
//    ) {
//        return buildError(HttpStatus.UNAUTHORIZED, "Authentication failed", request);
//    }
//
//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
//            AccessDeniedException ex, WebRequest request
//    ) {
//        return buildError(HttpStatus.FORBIDDEN, "Access denied", request);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiErrorResponse> handleGeneral(
//            Exception ex, WebRequest request
//    ) {
//        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
//    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Services throw this exception directly (no cause) with a user-facing message.
        // Violations coming from the database arrive wrapped around a driver exception,
        // where the original message is unsafe to show verbatim.
        String message = ex.getCause() == null && ex.getMessage() != null
                ? ex.getMessage()
                : "This change conflicts with existing data. Please refresh the page and try again.";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", message
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", message
        ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, String>> handleMethodValidation(HandlerMethodValidationException ex) {
        String detail = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse("Validation failed");

        String message = (ex.isForReturnValue() ? "Response validation failed: " : "Invalid request: ") + detail;
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "message", message
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Invalid request body."
        ));
    }

    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, String>> handleFileStorage(FileStorageException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", ex.getMessage() + ". Please try again."
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        long maxSizeInMegabytes = ex.getMaxUploadSize() / 1024 / 1024;
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                "message", "Uploaded file exceeds the maximum allowed size of "
                        + maxSizeInMegabytes + " MB."
        ));
    }

}
