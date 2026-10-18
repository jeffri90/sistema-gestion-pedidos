package com.alicorp.pedidos.service;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.exception.BusinessException;
import com.alicorp.pedidos.model.*;
import com.alicorp.pedidos.repository.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class PedidoService {
  private final PedidoRepository pedidos;
  private final ClienteRepository clientes;
  private final ProductoRepository productos;

  public PedidoService(
      PedidoRepository pedidos, ClienteRepository clientes, ProductoRepository productos) {
    this.pedidos = pedidos;
    this.clientes = clientes;
    this.productos = productos;
  }

  public List<PedidoResponse> listar(Long clienteId, EstadoPedido estado) {
    return pedidos.filtrar(clienteId, estado).stream().map(PedidoResponse::of).toList();
  }

  public PedidoResponse obtener(Long id) {
    return PedidoResponse.of(
        pedidos.findById(id).orElseThrow(() -> BusinessException.missing("Pedido")));
  }

  @Transactional
  public PedidoResponse crear(@Valid PedidoRequest r) {
    Pedido p = new Pedido();
    completar(p, r);
    return PedidoResponse.of(pedidos.saveAndFlush(p));
  }

  @Transactional
  public PedidoResponse actualizar(Long id, @Valid PedidoRequest r) {
    Pedido p = bloquear(id);
    exigirBorrador(p);
    completar(p, r);
    return PedidoResponse.of(pedidos.saveAndFlush(p));
  }

  @Transactional
  public void eliminar(Long id) {
    Pedido p = bloquear(id);
    if (p.getEstado() == EstadoPedido.CONFIRMADO)
      throw BusinessException.conflict("Cancele el pedido antes de eliminarlo");
    pedidos.delete(p);
    pedidos.flush();
  }

  @Transactional
  public PedidoResponse confirmar(Long id) {
    Pedido p = bloquear(id);
    exigirBorrador(p);
    // Bloqueo ordenado: evita sobreventa y reduce interbloqueos entre pedidos.
    for (DetallePedido d : ordenados(p)) {
      Producto producto =
          productos
              .bloquear(d.getProducto().getId())
              .orElseThrow(() -> BusinessException.missing("Producto"));
      if (producto.getStock() < d.getCantidad())
        throw BusinessException.conflict("Stock insuficiente para " + producto.getNombre());
      producto.setStock(producto.getStock() - d.getCantidad());
    }
    p.setEstado(EstadoPedido.CONFIRMADO);
    pedidos.flush();
    return PedidoResponse.of(p);
  }

  @Transactional
  public PedidoResponse cancelar(Long id) {
    Pedido p = bloquear(id);
    if (p.getEstado() == EstadoPedido.CANCELADO)
      throw BusinessException.conflict("El pedido ya está cancelado");
    if (p.getEstado() == EstadoPedido.CONFIRMADO) {
      for (DetallePedido d : ordenados(p)) {
        Producto producto =
            productos
                .bloquear(d.getProducto().getId())
                .orElseThrow(() -> BusinessException.missing("Producto"));
        int nuevo = producto.getStock() + d.getCantidad();
        if (nuevo > 1000000)
          throw BusinessException.conflict("La devolución supera el stock máximo permitido");
        producto.setStock(nuevo);
      }
    }
    p.setEstado(EstadoPedido.CANCELADO);
    pedidos.flush();
    return PedidoResponse.of(p);
  }

  private Pedido bloquear(Long id) {
    return pedidos.bloquear(id).orElseThrow(() -> BusinessException.missing("Pedido"));
  }

  private void exigirBorrador(Pedido p) {
    if (p.getEstado() != EstadoPedido.BORRADOR)
      throw BusinessException.conflict("Solo se puede modificar o confirmar un pedido en borrador");
  }

  private List<DetallePedido> ordenados(Pedido p) {
    return p.getDetalles().stream()
        .sorted(Comparator.comparing(d -> d.getProducto().getId()))
        .toList();
  }

  private void completar(Pedido p, PedidoRequest r) {
    p.setCliente(
        clientes.findById(r.clienteId()).orElseThrow(() -> BusinessException.missing("Cliente")));
    Set<Long> ids = new HashSet<>();
    List<DetallePedido> detalles = new ArrayList<>();
    for (PedidoRequest.Item item : r.items()) {
      if (!ids.add(item.productoId()))
        throw BusinessException.conflict("El producto no puede repetirse en un pedido");
      Producto producto =
          productos
              .findById(item.productoId())
              .orElseThrow(() -> BusinessException.missing("Producto"));
      detalles.add(new DetallePedido(producto, item.cantidad()));
    }
    // Hibernate inserta antes de eliminar huérfanos: vaciamos las líneas antiguas
    // dentro de esta misma transacción para respetar la clave pedido/producto.
    if (p.getId() != null) {
      p.reemplazarDetalles(List.of());
      pedidos.flush();
    }
    p.reemplazarDetalles(detalles);
  }
}
