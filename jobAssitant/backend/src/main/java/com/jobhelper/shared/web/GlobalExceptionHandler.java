package com.jobhelper.shared.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> handleApi(ApiException ex) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorBody(ex.getCode(), ex.getMessage(), ex.getDetails(), requestId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.badRequest()
                .body(new ErrorBody("VALIDATION_ERROR", "Request validation failed", ex.getBindingResult().toString(), requestId));
    }
}
