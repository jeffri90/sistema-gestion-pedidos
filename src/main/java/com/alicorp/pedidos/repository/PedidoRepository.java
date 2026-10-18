package com.alicorp.pedidos.repository;

import com.alicorp.pedidos.model.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
  @Query(
      "select p from Pedido p where (:clienteId is null or p.cliente.id=:clienteId) and (:estado is"
          + " null or p.estado=:estado) order by p.id desc")
  List<Pedido> filtrar(Long clienteId, EstadoPedido estado);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Pedido p where p.id=:id")
  Optional<Pedido> bloquear(Long id);
}
