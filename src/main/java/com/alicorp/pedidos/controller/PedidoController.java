package com.alicorp.pedidos.controller;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.model.EstadoPedido;
import com.alicorp.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
  private final PedidoService service;

  public PedidoController(PedidoService service) {
    this.service = service;
  }

  @GetMapping
  public List<PedidoResponse> listar(
      @RequestParam(required = false) Long clienteId,
      @RequestParam(required = false) EstadoPedido estado) {
    return service.listar(clienteId, estado);
  }

  @GetMapping("/{id}")
  public PedidoResponse obtener(@PathVariable Long id) {
    return service.obtener(id);
  }

  @PostMapping
  public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest r) {
    var p = service.crear(r);
    return ResponseEntity.created(URI.create("/api/pedidos/" + p.id())).body(p);
  }

  @PutMapping("/{id}")
  public PedidoResponse actualizar(@PathVariable Long id, @Valid @RequestBody PedidoRequest r) {
    return service.actualizar(id, r);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    service.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/confirmar")
  public PedidoResponse confirmar(@PathVariable Long id) {
    return service.confirmar(id);
  }

  @PostMapping("/{id}/cancelar")
  public PedidoResponse cancelar(@PathVariable Long id) {
    return service.cancelar(id);
  }
}
