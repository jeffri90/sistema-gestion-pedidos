package com.alicorp.pedidos.dao;

import com.alicorp.pedidos.dto.*;
import java.util.*;

public interface ClienteDao {
  List<ClienteResponse> listar();

  Optional<ClienteResponse> obtener(Long id);

  ClienteResponse crear(ClienteRequest r);

  ClienteResponse actualizar(Long id, ClienteRequest r);

  void eliminar(Long id);
}
