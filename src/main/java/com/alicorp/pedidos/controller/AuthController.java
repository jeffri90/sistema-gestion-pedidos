package com.alicorp.pedidos.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthenticationManager manager;
  private final JwtEncoder encoder;
  private final long minutes;

  public AuthController(
      AuthenticationManager manager,
      JwtEncoder encoder,
      @Value("${app.jwt.minutes}") long minutes) {
    this.manager = manager;
    this.encoder = encoder;
    this.minutes = minutes;
  }

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  public record LoginResponse(String token, String tokenType, long expiresIn, List<String> roles) {}

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    var auth =
        manager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.username(), request.password()));
    var roles =
        auth.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.substring(5))
            .toList();
    var now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer("sistema-gestion-pedidos")
            .subject(auth.getName())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(minutes * 60))
            .claim("roles", roles)
            .build();
    String token =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new LoginResponse(token, "Bearer", minutes * 60, roles);
  }
}
