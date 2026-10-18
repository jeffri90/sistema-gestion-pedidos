package com.alicorp.pedidos.dto;

import jakarta.validation.constraints.*;

public record ClienteRequest(
    @NotBlank @Pattern(regexp = "[0-9]{11}", message = "El RUC debe tener 11 dígitos") String ruc,
    @NotBlank @Size(max = 120) String razonSocial,
    @NotBlank @Email @Size(max = 150) String email,
    @NotBlank @Pattern(regexp = "[+0-9 ()-]{7,20}", message = "Teléfono inválido") String telefono,
    @NotBlank @Size(max = 200) String direccion) {}
