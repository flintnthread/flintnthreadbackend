package com.ecommerce.authdemo.exception;

import com.ecommerce.authdemo.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse<>(false, ex.getMessage(), null)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                .orElse("Validation error");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(false, errorMessage, null)
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraint(ConstraintViolationException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(false, ex.getMessage(), null)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = ex.getName() + " : invalid value";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(false, msg, null)
        );
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(Exception ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ApiResponse<>(false, "Access Denied", null)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegal(IllegalArgumentException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(false, ex.getMessage(), null)
        );
    }

    @ExceptionHandler(OtpNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleOtpNotFound(OtpNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse<>(false, ex.getMessage(), null)
        );
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleOtpExpired(OtpExpiredException ex) {

        return ResponseEntity.status(HttpStatus.GONE).body(
                new ApiResponse<>(false, ex.getMessage(), null)
        );
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ApiResponse<Object>> handleTooManyAttempts(TooManyAttemptsException ex) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                new ApiResponse<>(false, ex.getMessage(), null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAll(Exception ex) {

        ex.printStackTrace(); // 🔥 VERY IMPORTANT (logs full error)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, ex.getMessage(), null) // ✅ show real error
        );
    }
}