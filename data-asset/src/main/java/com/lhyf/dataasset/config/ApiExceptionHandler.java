package com.lhyf.dataasset.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", 400, "message", e.getMessage() == null ? "bad request" : e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> serverError(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", e.getMessage() == null ? "server error" : e.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> forwarded(ResponseStatusException e) {
        String reason = e.getReason() == null || e.getReason().isEmpty() ? e.getStatus().getReasonPhrase()
                : e.getReason();
        return ResponseEntity.status(e.getStatus())
                .body(Map.of("code", e.getStatus().value(), "message", reason));
    }
}
