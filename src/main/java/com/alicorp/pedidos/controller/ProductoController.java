package com.alicorp.pedidos.controller;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.service.ProductoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
  private final ProductoService service;

  public ProductoController(ProductoService service) {
    this.service = service;
  }

  @GetMapping
  public List<ProductoResponse> listar(@RequestParam(defaultValue = "") String texto) {
    return service.listar(texto);
  }

  @GetMapping("/{id}")
  public ProductoResponse obtener(@PathVariable Long id) {
    return service.obtener(id);
  }

  @PostMapping
  public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest r) {
    var creado = service.crear(r);
    return ResponseEntity.created(URI.create("/api/productos/" + creado.id())).body(creado);
  }

  @PutMapping("/{id}")
  public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest r) {
    return service.actualizar(id, r);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    service.eliminar(id);
    return ResponseEntity.noContent().build();
  }
}
