package com.alicorp.pedidos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 11)
  private String ruc;

  @Column(name = "razon_social", nullable = false, length = 120)
  private String razonSocial;

  @Column(nullable = false, length = 150)
  private String email;

  @Column(nullable = false, length = 20)
  private String telefono;

  @Column(nullable = false, length = 200)
  private String direccion;

  protected Cliente() {}

  public Cliente(String ruc, String razonSocial, String email, String telefono, String direccion) {
    actualizar(ruc, razonSocial, email, telefono, direccion);
  }

  public void actualizar(
      String ruc, String razonSocial, String email, String telefono, String direccion) {
    this.ruc = ruc;
    this.razonSocial = razonSocial;
    this.email = email;
    this.telefono = telefono;
    this.direccion = direccion;
  }

  public Long getId() {
    return id;
  }

  public String getRuc() {
    return ruc;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public String getEmail() {
    return email;
  }

  public String getTelefono() {
    return telefono;
  }

  public String getDireccion() {
    return direccion;
  }
}
