package com.alicorp.pedidos.dto;

import com.alicorp.pedidos.model.Cliente;

public record ClienteResponse(
    Long id, String ruc, String razonSocial, String email, String telefono, String direccion) {
  public static ClienteResponse of(Cliente c) {
    return new ClienteResponse(
        c.getId(), c.getRuc(), c.getRazonSocial(), c.getEmail(), c.getTelefono(), c.getDireccion());
  }
}
