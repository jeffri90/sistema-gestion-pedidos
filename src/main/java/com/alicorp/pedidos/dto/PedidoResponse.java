package com.alicorp.pedidos.dto;

import com.alicorp.pedidos.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
    Long id,
    Long clienteId,
    String cliente,
    LocalDateTime fecha,
    EstadoPedido estado,
    BigDecimal total,
    List<Item> items) {
  public record Item(
      Long productoId,
      String producto,
      int cantidad,
      BigDecimal precioUnitario,
      BigDecimal subtotal) {}

  public static PedidoResponse of(Pedido p) {
    return new PedidoResponse(
        p.getId(),
        p.getCliente().getId(),
        p.getCliente().getRazonSocial(),
        p.getFecha(),
        p.getEstado(),
        p.getTotal(),
        p.getDetalles().stream()
            .map(
                d ->
                    new Item(
                        d.getProducto().getId(),
                        d.getProducto().getNombre(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal()))
            .toList());
  }
}
