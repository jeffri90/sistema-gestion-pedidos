package com.alicorp.pedidos.web;

import com.alicorp.pedidos.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice(basePackages = "com.alicorp.pedidos.web")
public class WebExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  String business(BusinessException e, Model m, HttpServletResponse r) {
    r.setStatus(e.getStatus().value());
    m.addAttribute("error", e.getMessage());
    return "error-operacion";
  }

  @ExceptionHandler(ConstraintViolationException.class)
  String invalid(ConstraintViolationException e, Model m, HttpServletResponse r) {
    r.setStatus(400);
    m.addAttribute(
        "error",
        e.getConstraintViolations().stream()
            .map(v -> v.getMessage())
            .sorted()
            .reduce((a, b) -> a + ". " + b)
            .orElse("Datos inválidos"));
    return "error-operacion";
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  String conflict(Exception e, Model m, HttpServletResponse r) {
    r.setStatus(409);
    m.addAttribute(
        "error", "SKU o RUC duplicado, o registro vinculado a un pedido. Revise los datos.");
    return "error-operacion";
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  String format(Exception e, Model m, HttpServletResponse r) {
    r.setStatus(400);
    m.addAttribute("error", "Revise los números y campos del formulario.");
    return "error-operacion";
  }
}
