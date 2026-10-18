package com.alicorp.pedidos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "detalle_pedido",
    uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id", "producto_id"}))
public class DetallePedido {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pedido_id")
  private Pedido pedido;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "producto_id")
  private Producto producto;

  @Column(nullable = false)
  private int cantidad;

  @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
  private BigDecimal precioUnitario;

  protected DetallePedido() {}

  public DetallePedido(Producto producto, int cantidad) {
    this.producto = producto;
    this.cantidad = cantidad;
    this.precioUnitario = producto.getPrecio();
  }

  public void setPedido(Pedido pedido) {
    this.pedido = pedido;
  }

  public Producto getProducto() {
    return producto;
  }

  public int getCantidad() {
    return cantidad;
  }

  public BigDecimal getPrecioUnitario() {
    return precioUnitario;
  }

  public BigDecimal getSubtotal() {
    return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
  }
}
