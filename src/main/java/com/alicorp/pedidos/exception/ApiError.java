package com.alicorp.pedidos.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String message, List<String> errors) {}
