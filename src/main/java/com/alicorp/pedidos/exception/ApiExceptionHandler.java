package com.alicorp.pedidos.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = "com.alicorp.pedidos.controller")
public class ApiExceptionHandler {
  private ResponseEntity<ApiError> error(HttpStatus status, String message, List<String> errors) {
    return ResponseEntity.status(status)
        .body(new ApiError(Instant.now(), status.value(), message, errors));
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiError> business(BusinessException e) {
    return error(e.getStatus(), e.getMessage(), List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
    return error(
        HttpStatus.BAD_REQUEST,
        "Datos inválidos",
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .toList());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> constraints(ConstraintViolationException e) {
    return error(
        HttpStatus.BAD_REQUEST,
        "Datos inválidos",
        e.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .sorted()
            .toList());
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ApiError> malformed(Exception e) {
    return error(
        HttpStatus.BAD_REQUEST,
        "Revise el formato JSON, los identificadores y el estado solicitado",
        List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> integrity(Exception e) {
    return error(
        HttpStatus.CONFLICT,
        "SKU o RUC duplicado, o registro relacionado con un pedido",
        List.of());
  }

  @ExceptionHandler({
    OptimisticLockingFailureException.class,
    PessimisticLockingFailureException.class
  })
  ResponseEntity<ApiError> concurrent(Exception e) {
    return error(
        HttpStatus.CONFLICT,
        "El registro cambió o está ocupado; vuelva a consultar e intente otra vez",
        List.of());
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ApiError> auth(Exception e) {
    return error(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas", List.of());
  }
}
