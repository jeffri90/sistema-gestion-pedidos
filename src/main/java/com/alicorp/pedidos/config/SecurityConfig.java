package com.alicorp.pedidos.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(
      PasswordEncoder encoder,
      @Value("${app.users.admin-password}") String admin,
      @Value("${app.users.ventas-password}") String ventas,
      @Value("${app.users.consulta-password}") String consulta) {
    return new InMemoryUserDetailsManager(
        User.withUsername("admin").password(encoder.encode(admin)).roles("ADMIN").build(),
        User.withUsername("ventas").password(encoder.encode(ventas)).roles("VENTAS").build(),
        User.withUsername("consulta").password(encoder.encode(consulta)).roles("CONSULTA").build());
  }

  @Bean
  AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
    var provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
  }

  private SecretKeySpec key(String secret) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException("JWT_SECRET requiere al menos 32 bytes");
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  JwtEncoder encoder(@Value("${app.jwt.secret}") String secret) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
  }

  @Bean
  JwtDecoder decoder(@Value("${app.jwt.secret}") String secret) {
    var d = NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
    d.setJwtValidator(JwtValidators.createDefaultWithIssuer("sistema-gestion-pedidos"));
    return d;
  }

  @Bean
  @Order(1)
  SecurityFilterChain api(HttpSecurity http) throws Exception {
    var roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(roles);
    return http.securityMatcher("/api/**")
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/**")
                    .hasAnyRole("ADMIN", "VENTAS", "CONSULTA")
                    .requestMatchers(HttpMethod.DELETE, "/api/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/pedidos", "/api/pedidos/**")
                    .hasAnyRole("ADMIN", "VENTAS")
                    .requestMatchers(HttpMethod.PUT, "/api/pedidos/*")
                    .hasAnyRole("ADMIN", "VENTAS")
                    .requestMatchers(HttpMethod.POST, "/api/productos", "/api/clientes")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/productos/*", "/api/clientes/*")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            o ->
                o.jwt(j -> j.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint(
                        (req, res, e) -> jsonError(res, 401, "Token ausente, inválido o vencido"))
                    .accessDeniedHandler(
                        (req, res, e) ->
                            jsonError(res, 403, "No tiene permisos para esta operación")))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> jsonError(res, 401, "Autenticación requerida"))
                    .accessDeniedHandler(
                        (req, res, ex) ->
                            jsonError(res, 403, "No tiene permisos para esta operación")))
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain web(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/login", "/css/**", "/error", "/jakarta.faces.resource/**")
                    .permitAll()
                    .requestMatchers("/clientes/**", "/jsf/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/productos/nuevo", "/productos/*/editar")
                    .hasRole("ADMIN")
                    .requestMatchers("/pedidos/nuevo", "/pedidos/*/editar")
                    .hasAnyRole("ADMIN", "VENTAS")
                    .requestMatchers(HttpMethod.POST, "/productos/**", "/pedidos/*/eliminar")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/pedidos/**")
                    .hasAnyRole("ADMIN", "VENTAS")
                    .anyRequest()
                    .authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
        .logout(l -> l.logoutSuccessUrl("/login?logout"))
        .build();
  }

  private static void jsonError(
      jakarta.servlet.http.HttpServletResponse res, int status, String message)
      throws java.io.IOException {
    res.setStatus(status);
    res.setContentType("application/json;charset=UTF-8");
    res.getWriter()
        .write("{\"status\":" + status + ",\"message\":\"" + message + "\",\"errors\":[]}");
  }
}
