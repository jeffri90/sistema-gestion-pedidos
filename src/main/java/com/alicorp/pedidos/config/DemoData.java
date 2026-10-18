package com.alicorp.pedidos.config;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.service.*;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
public class DemoData {
  @Bean
  @ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
  ApplicationRunner demo(ProductoService productos, ClienteService clientes) {
    return args -> {
      if (productos.listar("").isEmpty()) {
        productos.crear(
            new ProductoRequest(
                "ALI-001", "Aceite vegetal 1 L", "Alimentos", new BigDecimal("8.50"), 120));
        productos.crear(
            new ProductoRequest(
                "ALI-002", "Fideos 500 g", "Alimentos", new BigDecimal("3.20"), 200));
        productos.crear(
            new ProductoRequest(
                "ALI-003", "Detergente 900 g", "Cuidado del hogar", new BigDecimal("12.90"), 80));
      }
      if (clientes.listar().isEmpty())
        clientes.crear(
            new ClienteRequest(
                "20999999991",
                "Distribuidora Demo S.A.C.",
                "demo@example.com",
                "999111222",
                "Av. Ejemplo 123, Lima"));
    };
  }
}
