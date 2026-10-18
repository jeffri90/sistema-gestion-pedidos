package com.alicorp.pedidos.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "pedidos")
public class Pedido {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  @Column(nullable = false)
  private LocalDateTime fecha = LocalDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoPedido estado = EstadoPedido.BORRADOR;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;

  @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<DetallePedido> detalles = new ArrayList<>();

  @Version private long version;

  public Pedido() {}

  public void setCliente(Cliente cliente) {
    this.cliente = cliente;
  }

  public void reemplazarDetalles(List<DetallePedido> nuevos) {
    detalles.clear();
    nuevos.forEach(
        d -> {
          d.setPedido(this);
          detalles.add(d);
        });
    total =
        detalles.stream().map(DetallePedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public Long getId() {
    return id;
  }

  public Cliente getCliente() {
    return cliente;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public EstadoPedido getEstado() {
    return estado;
  }

  public void setEstado(EstadoPedido estado) {
    this.estado = estado;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public List<DetallePedido> getDetalles() {
    return detalles;
  }
}
