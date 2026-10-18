package com.alicorp.pedidos.config;

import com.alicorp.pedidos.dao.*;
import com.alicorp.pedidos.service.ClienteService;
import org.springframework.context.annotation.*;

@Configuration
public class ClienteConfig {
  @Bean
  @Primary
  public ClienteService clienteJpaService(JpaClienteDao dao) {
    return new ClienteService(dao);
  }

  @Bean
  public ClienteService clienteJdbcService(JdbcClienteDao dao) {
    return new ClienteService(dao);
  }
}
