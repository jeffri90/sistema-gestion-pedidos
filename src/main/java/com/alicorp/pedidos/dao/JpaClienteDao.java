package com.alicorp.pedidos.dao;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.exception.BusinessException;
import com.alicorp.pedidos.model.Cliente;
import com.alicorp.pedidos.repository.ClienteRepository;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaClienteDao implements ClienteDao {
  private final ClienteRepository repository;

  public JpaClienteDao(ClienteRepository repository) {
    this.repository = repository;
  }

  public List<ClienteResponse> listar() {
    return repository.findAll(Sort.by("id")).stream().map(ClienteResponse::of).toList();
  }

  public Optional<ClienteResponse> obtener(Long id) {
    return repository.findById(id).map(ClienteResponse::of);
  }

  public ClienteResponse crear(ClienteRequest r) {
    return ClienteResponse.of(
        repository.saveAndFlush(
            new Cliente(r.ruc(), r.razonSocial(), r.email(), r.telefono(), r.direccion())));
  }

  public ClienteResponse actualizar(Long id, ClienteRequest r) {
    Cliente c = repository.findById(id).orElseThrow(() -> BusinessException.missing("Cliente"));
    c.actualizar(r.ruc(), r.razonSocial(), r.email(), r.telefono(), r.direccion());
    return ClienteResponse.of(repository.saveAndFlush(c));
  }

  public void eliminar(Long id) {
    repository.deleteById(id);
    repository.flush();
  }
}
