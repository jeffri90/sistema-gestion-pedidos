package com.alicorp.pedidos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class Producto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 30)
  private String sku;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(nullable = false, length = 60)
  private String categoria;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal precio;

  @Column(nullable = false)
  private int stock;

  @Version private long version;

  protected Producto() {}

  public Producto(String sku, String nombre, String categoria, BigDecimal precio, int stock) {
    actualizar(sku, nombre, categoria, precio, stock);
  }

  public void actualizar(
      String sku, String nombre, String categoria, BigDecimal precio, int stock) {
    this.sku = sku;
    this.nombre = nombre;
    this.categoria = categoria;
    this.precio = precio;
    this.stock = stock;
  }

  public Long getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getNombre() {
    return nombre;
  }

  public String getCategoria() {
    return categoria;
  }

  public BigDecimal getPrecio() {
    return precio;
  }

  public int getStock() {
    return stock;
  }

  public void setStock(int stock) {
    this.stock = stock;
  }
}
