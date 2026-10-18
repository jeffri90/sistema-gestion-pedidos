package com.alicorp.pedidos.controller;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.service.ClienteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
  private final ClienteService service;

  public ClienteController(ClienteService service) {
    this.service = service;
  }

  @GetMapping
  public List<ClienteResponse> listar() {
    return service.listar();
  }

  @GetMapping("/{id}")
  public ClienteResponse obtener(@PathVariable Long id) {
    return service.obtener(id);
  }

  @PostMapping
  public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest r) {
    var creado = service.crear(r);
    return ResponseEntity.created(URI.create("/api/clientes/" + creado.id())).body(creado);
  }

  @PutMapping("/{id}")
  public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest r) {
    return service.actualizar(id, r);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    service.eliminar(id);
    return ResponseEntity.noContent().build();
  }
}
