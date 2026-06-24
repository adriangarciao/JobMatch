// ...existing code...
package com.adriangarciao.jobmatch.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // simple JSON error envelope
    public record ApiError(int status, String error, String message, Instant timestamp) {}

    // --- 404s ---
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleUserNotFound(UserNotFoundException ex) {
        return new ApiError(404, "Not Found", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleApplicationNotFound(ApplicationNotFoundException ex) {
        return new ApiError(404, "Not Found", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(ResumeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleResumeNotFound(ResumeNotFoundException ex) {
        return new ApiError(404, "Not Found", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        return new ApiError(409, "Conflict", ex.getMessage(), Instant.now());
    }

    // --- 400s (validation) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (msg.isBlank()) msg = "Validation error";
        return new ApiError(400, "Bad Request", msg, Instant.now());
    }

    // (optional) Bean Validation thrown outside @RequestBody binding
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        if (msg.isBlank()) msg = "Validation error";
        return new ApiError(400, "Bad Request", msg, Instant.now());
    }

    // --- 400/500 custom domain errors ---
    @ExceptionHandler(InvalidFileTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidFileType(InvalidFileTypeException ex) {
        return new ApiError(400, "Bad Request", ex.getMessage(), Instant.now());
    }

    @ExceptionHandler(FileStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleFileStorage(FileStorageException ex) {
        return new ApiError(500, "Internal Server Error", ex.getMessage(), Instant.now());
    }

    // --- 403 ---
    @ExceptionHandler(ForbiddenOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbidden(ForbiddenOperationException ex) {
        return new ApiError(403, "Forbidden", ex.getMessage(), Instant.now());
    }

    // --- generic ResponseStatusException passthrough ---
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleRse(ResponseStatusException ex) {
        HttpStatusCode sc = ex.getStatusCode();
        HttpStatus status = (sc instanceof HttpStatus hs) ? hs : HttpStatus.valueOf(sc.value());
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(),
                ex.getReason(), Instant.now());
        return ResponseEntity.status(status).body(body);
    }

    // --- last resort ---
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleAny(Exception ex) {
        return new ApiError(500, "Internal Server Error", ex.getMessage(), Instant.now());
    }
}
