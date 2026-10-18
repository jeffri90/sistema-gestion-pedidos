package com.alicorp.pedidos.service;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.exception.BusinessException;
import com.alicorp.pedidos.model.Producto;
import com.alicorp.pedidos.repository.ProductoRepository;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class ProductoService {
  private final ProductoRepository repository;

  public ProductoService(ProductoRepository repository) {
    this.repository = repository;
  }

  public List<ProductoResponse> listar(String texto) {
    return repository.buscar(texto == null ? "" : texto.trim()).stream()
        .map(ProductoResponse::of)
        .toList();
  }

  public ProductoResponse obtener(Long id) {
    return ProductoResponse.of(
        repository.findById(id).orElseThrow(() -> BusinessException.missing("Producto")));
  }

  @Transactional
  public ProductoResponse crear(@Valid ProductoRequest r) {
    return ProductoResponse.of(
        repository.saveAndFlush(
            new Producto(
                r.sku().trim().toUpperCase(Locale.ROOT),
                r.nombre().trim(),
                r.categoria().trim(),
                r.precio(),
                r.stock())));
  }

  @Transactional
  public ProductoResponse actualizar(Long id, @Valid ProductoRequest r) {
    Producto p = repository.bloquear(id).orElseThrow(() -> BusinessException.missing("Producto"));
    p.actualizar(
        r.sku().trim().toUpperCase(Locale.ROOT),
        r.nombre().trim(),
        r.categoria().trim(),
        r.precio(),
        r.stock());
    return ProductoResponse.of(repository.saveAndFlush(p));
  }

  @Transactional
  public void eliminar(Long id) {
    Producto p = repository.bloquear(id).orElseThrow(() -> BusinessException.missing("Producto"));
    repository.delete(p);
    repository.flush();
  }
}
