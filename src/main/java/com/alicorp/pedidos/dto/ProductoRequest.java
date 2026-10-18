package com.alicorp.pedidos.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductoRequest(
    @NotBlank
        @Pattern(regexp = "[A-Za-z0-9-]{2,30}", message = "SKU: 2 a 30 letras, números o guiones")
        String sku,
    @NotBlank @Size(max = 120) String nombre,
    @NotBlank @Size(max = 60) String categoria,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precio,
    @NotNull @Min(0) @Max(1000000) Integer stock) {}
