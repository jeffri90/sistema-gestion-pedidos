package com.alicorp.pedidos.service;

import com.alicorp.pedidos.dao.ClienteDao;
import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.exception.BusinessException;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Transactional(readOnly = true)
public class ClienteService {
  private final ClienteDao dao;

  public ClienteService(ClienteDao dao) {
    this.dao = dao;
  }

  public List<ClienteResponse> listar() {
    return dao.listar();
  }

  public ClienteResponse obtener(Long id) {
    return dao.obtener(id).orElseThrow(() -> BusinessException.missing("Cliente"));
  }

  @Transactional
  public ClienteResponse crear(@Valid ClienteRequest r) {
    return dao.crear(limpiar(r));
  }

  @Transactional
  public ClienteResponse actualizar(Long id, @Valid ClienteRequest r) {
    obtener(id);
    return dao.actualizar(id, limpiar(r));
  }

  @Transactional
  public void eliminar(Long id) {
    obtener(id);
    dao.eliminar(id);
  }

  private ClienteRequest limpiar(ClienteRequest r) {
    return new ClienteRequest(
        r.ruc(),
        r.razonSocial().trim(),
        r.email().trim().toLowerCase(Locale.ROOT),
        r.telefono().trim(),
        r.direccion().trim());
  }
}
