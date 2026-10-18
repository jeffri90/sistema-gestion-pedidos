package com.alicorp.pedidos.repository;

import com.alicorp.pedidos.model.Producto;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
  @Query(
      "select p from Producto p where lower(p.nombre) like lower(concat('%', :texto, '%')) or"
          + " lower(p.sku) like lower(concat('%', :texto, '%')) order by p.id")
  List<Producto> buscar(String texto);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Producto p where p.id=:id")
  Optional<Producto> bloquear(Long id);
}
