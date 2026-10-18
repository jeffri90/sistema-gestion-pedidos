package com.alicorp.pedidos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alicorp.pedidos.dao.ClienteDao;
import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.exception.BusinessException;
import com.alicorp.pedidos.model.*;
import com.alicorp.pedidos.repository.*;
import com.alicorp.pedidos.service.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServiceTest {
  @Test
  void crearProductoNormalizaYConservaPrecio() {
    var repo = mock(ProductoRepository.class);
    when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    var r =
        new ProductoService(repo)
            .crear(
                new ProductoRequest(
                    "abc-1", "  Aceite  ", " Alimentos ", new BigDecimal("8.50"), 12));
    assertEquals("ABC-1", r.sku());
    assertEquals("Aceite", r.nombre());
    assertEquals(new BigDecimal("8.50"), r.precio());
    assertEquals(12, r.stock());
  }

  @Test
  void productoInexistenteGenera404() {
    var repo = mock(ProductoRepository.class);
    when(repo.findById(1L)).thenReturn(Optional.empty());
    assertEquals(
        404,
        assertThrows(BusinessException.class, () -> new ProductoService(repo).obtener(1L))
            .getStatus()
            .value());
  }

  @Test
  void noEliminaProductoInexistente() {
    var repo = mock(ProductoRepository.class);
    when(repo.bloquear(1L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> new ProductoService(repo).eliminar(1L));
    verify(repo, never()).delete(any(Producto.class));
  }

  @Test
  void clienteNormalizaDatosAntesDePersistir() {
    var dao = mock(ClienteDao.class);
    var service = new ClienteService(dao);
    service.crear(
        new ClienteRequest(
            "20123456789", " Bodega Demo ", "VENTAS@example.com", " 999888777 ", " Lima "));
    var cap = ArgumentCaptor.forClass(ClienteRequest.class);
    verify(dao).crear(cap.capture());
    assertEquals("Bodega Demo", cap.getValue().razonSocial());
    assertEquals("ventas@example.com", cap.getValue().email());
    assertEquals("Lima", cap.getValue().direccion());
  }

  @Test
  void actualizarClienteInexistenteNoEscribe() {
    var dao = mock(ClienteDao.class);
    when(dao.obtener(1L)).thenReturn(Optional.empty());
    assertThrows(
        BusinessException.class,
        () ->
            new ClienteService(dao)
                .actualizar(
                    1L,
                    new ClienteRequest(
                        "20123456789", "Demo", "demo@example.com", "999888777", "Lima")));
    verify(dao, never()).actualizar(any(), any());
  }

  @Test
  void pedidoConClienteInexistenteNoSeGuarda() {
    var repo = mock(PedidoRepository.class);
    var clientes = mock(ClienteRepository.class);
    var productos = mock(ProductoRepository.class);
    when(clientes.findById(5L)).thenReturn(Optional.empty());
    assertThrows(
        BusinessException.class,
        () ->
            new PedidoService(repo, clientes, productos)
                .crear(new PedidoRequest(5L, List.of(new PedidoRequest.Item(1L, 2)))));
    verify(repo, never()).saveAndFlush(any());
  }
}
