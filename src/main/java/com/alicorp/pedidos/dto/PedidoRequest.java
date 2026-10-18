package com.alicorp.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record PedidoRequest(
    @NotNull @Positive Long clienteId, @NotEmpty @Size(max = 50) List<@NotNull @Valid Item> items) {
  public record Item(
      @NotNull @Positive Long productoId, @NotNull @Min(1) @Max(10000) Integer cantidad) {}
}
