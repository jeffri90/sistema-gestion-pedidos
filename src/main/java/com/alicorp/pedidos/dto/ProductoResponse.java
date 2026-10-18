package com.alicorp.pedidos.dto;

import com.alicorp.pedidos.model.Producto;
import java.math.BigDecimal;

public record ProductoResponse(
    Long id, String sku, String nombre, String categoria, BigDecimal precio, int stock) {
  public static ProductoResponse of(Producto p) {
    return new ProductoResponse(
        p.getId(), p.getSku(), p.getNombre(), p.getCategoria(), p.getPrecio(), p.getStock());
  }
}
