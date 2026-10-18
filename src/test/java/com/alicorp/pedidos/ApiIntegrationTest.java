package com.alicorp.pedidos;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.exception.BusinessException;
import com.alicorp.pedidos.model.*;
import com.alicorp.pedidos.repository.*;
import com.alicorp.pedidos.service.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ProductoRepository productos;
  @Autowired ClienteRepository clientes;
  @Autowired PedidoRepository pedidos;
  @Autowired ProductoService productoService;
  @Autowired ClienteService clienteService;
  @Autowired PedidoService pedidoService;

  @Autowired
  @Qualifier("clienteJdbcService")
  ClienteService jdbcClientes;

  @Autowired JwtDecoder decoder;
  @Autowired JwtEncoder encoder;
  private final ObjectMapper json = new ObjectMapper();
  private final Map<String, String> tokens = new HashMap<>();
  private Long productoId, clienteId;

  @BeforeAll
  void login() throws Exception {
    for (String role : List.of("admin", "ventas", "consulta")) {
      String password = Character.toUpperCase(role.charAt(0)) + role.substring(1) + "123!";
      var result =
          mvc.perform(
                  post("/api/auth/login")
                      .contentType("application/json")
                      .content(
                          json.writeValueAsString(Map.of("username", role, "password", password))))
              .andExpect(status().isOk())
              .andReturn();
      tokens.put(
          role, json.readTree(result.getResponse().getContentAsString()).get("token").asString());
    }
  }

  @BeforeEach
  void clean() {
    pedidos.deleteAll();
    clientes.deleteAll();
    productos.deleteAll();
    productoId = productoService.crear(pr("TEST-1", 10)).id();
    clienteId = clienteService.crear(cr("20123456789")).id();
  }

  private ProductoRequest pr(String sku, int stock) {
    return new ProductoRequest(sku, "Aceite de prueba", "Alimentos", new BigDecimal("8.50"), stock);
  }

  private ClienteRequest cr(String ruc) {
    return new ClienteRequest(
        ruc, "Distribuidor de prueba", "demo@example.com", "999888777", "Lima");
  }

  private PedidoRequest order(int cantidad) {
    return new PedidoRequest(clienteId, List.of(new PedidoRequest.Item(productoId, cantidad)));
  }

  private ResultActions call(String role, String method, String path, Object body)
      throws Exception {
    var r = request(HttpMethod.valueOf(method), "/api" + path);
    if (role != null) r.header("Authorization", "Bearer " + tokens.get(role));
    if (body != null) r.contentType("application/json").content(json.writeValueAsString(body));
    return mvc.perform(r);
  }

  private JsonNode node(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin", "ventas", "consulta"})
  void loginEntregaFirmaRolYExpiracion(String role) {
    var jwt = decoder.decode(tokens.get(role));
    assertEquals(role, jwt.getSubject());
    assertEquals(List.of(role.toUpperCase()), jwt.getClaimAsStringList("roles"));
    assertEquals(1800, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
  }

  @Test
  void credencialesIncorrectas401() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginVacio400() throws Exception {
    mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest());
  }

  static Stream<Arguments> routes() {
    return Stream.of("productos", "clientes", "pedidos")
        .flatMap(
            r ->
                Stream.of(
                    Arguments.of("GET", "/" + r),
                    Arguments.of("GET", "/" + r + "/1"),
                    Arguments.of("POST", "/" + r),
                    Arguments.of("PUT", "/" + r + "/1"),
                    Arguments.of("DELETE", "/" + r + "/1")));
  }

  @ParameterizedTest
  @MethodSource("routes")
  void rutasCrudRequierenJwt(String method, String path) throws Exception {
    call(null, method, path, null).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {"confirmar", "cancelar"})
  void transicionesRequierenJwt(String action) throws Exception {
    call(null, "POST", "/pedidos/1/" + action, null).andExpect(status().isUnauthorized());
  }

  @Test
  void tokenAlterado401() throws Exception {
    mvc.perform(get("/api/productos").header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenVencido401() throws Exception {
    var claims =
        JwtClaimsSet.builder()
            .issuer("sistema-gestion-pedidos")
            .subject("admin")
            .issuedAt(Instant.now().minusSeconds(7200))
            .expiresAt(Instant.now().minusSeconds(3600))
            .claim("roles", List.of("ADMIN"))
            .build();
    String token =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    mvc.perform(get("/api/productos").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void sesionWebNoAutorizaApiSinBearer() throws Exception {
    var login =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", "admin")
                    .param("password", "Admin123!"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    var session =
        (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
    assertNotNull(session);
    mvc.perform(get("/api/productos").session(session)).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @CsvSource({
    "admin,productos",
    "ventas,productos",
    "consulta,productos",
    "admin,clientes",
    "ventas,clientes",
    "consulta,clientes",
    "admin,pedidos",
    "ventas,pedidos",
    "consulta,pedidos"
  })
  void rolesPuedenConsultar(String role, String resource) throws Exception {
    call(role, "GET", "/" + resource, null).andExpect(status().isOk());
  }

  @ParameterizedTest
  @CsvSource({"ventas,productos", "consulta,productos", "ventas,clientes", "consulta,clientes"})
  void catalogosSoloAdmin(String role, String resource) throws Exception {
    Object body = resource.equals("productos") ? pr("NUEVO", 5) : cr("20987654321");
    call(role, "POST", "/" + resource, body).andExpect(status().isForbidden());
    call(
            role,
            "PUT",
            "/" + resource + "/" + (resource.equals("productos") ? productoId : clienteId),
            body)
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @CsvSource({
    "ventas,productos",
    "consulta,productos",
    "ventas,clientes",
    "consulta,clientes",
    "ventas,pedidos",
    "consulta,pedidos"
  })
  void eliminacionSoloAdmin(String role, String resource) throws Exception {
    call(role, "DELETE", "/" + resource + "/1", null).andExpect(status().isForbidden());
  }

  @Test
  void consultaNoPuedeCrearNiConfirmarPedido() throws Exception {
    var p = pedidoService.crear(order(2));
    call("consulta", "POST", "/pedidos", order(2)).andExpect(status().isForbidden());
    call("consulta", "PUT", "/pedidos/" + p.id(), order(1)).andExpect(status().isForbidden());
    call("consulta", "POST", "/pedidos/" + p.id() + "/confirmar", null)
        .andExpect(status().isForbidden());
    call("consulta", "POST", "/pedidos/" + p.id() + "/cancelar", null)
        .andExpect(status().isForbidden());
  }

  @Test
  void crudProductoPersistente() throws Exception {
    long id =
        node(call("admin", "POST", "/productos", pr("NUEVO", 20))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location")))
            .get("id")
            .asLong();
    assertTrue(productos.existsById(id));
    call("consulta", "GET", "/productos/" + id, null).andExpect(jsonPath("$.stock").value(20));
    call("admin", "PUT", "/productos/" + id, pr("NUEVO", 25)).andExpect(status().isOk());
    assertEquals(25, productos.findById(id).orElseThrow().getStock());
    call("admin", "DELETE", "/productos/" + id, null).andExpect(status().isNoContent());
    call("admin", "GET", "/productos/" + id, null).andExpect(status().isNotFound());
  }

  @Test
  void crudClienteJpaPersistente() throws Exception {
    long id =
        node(call("admin", "POST", "/clientes", cr("20987654321")).andExpect(status().isCreated()))
            .get("id")
            .asLong();
    call(
            "admin",
            "PUT",
            "/clientes/" + id,
            new ClienteRequest(
                "20987654321", "Cliente editado", "nuevo@example.com", "999777666", "Callao"))
        .andExpect(status().isOk());
    assertEquals("Cliente editado", clientes.findById(id).orElseThrow().getRazonSocial());
    call("admin", "GET", "/clientes/" + id, null).andExpect(status().isOk());
    call("admin", "DELETE", "/clientes/" + id, null).andExpect(status().isNoContent());
    assertFalse(clientes.existsById(id));
  }

  @Test
  void crudJdbcComparteBaseConJpa() {
    var c = jdbcClientes.crear(cr("20987654321"));
    assertEquals(c.ruc(), clienteService.obtener(c.id()).ruc());
    jdbcClientes.actualizar(
        c.id(),
        new ClienteRequest(
            "20987654321", "Editado por JDBC", "nuevo@example.com", "999111222", "Callao"));
    assertEquals("Editado por JDBC", clienteService.obtener(c.id()).razonSocial());
    assertTrue(jdbcClientes.listar().stream().anyMatch(x -> x.id().equals(c.id())));
    jdbcClientes.eliminar(c.id());
    assertFalse(clientes.existsById(c.id()));
  }

  @Test
  void jdbcParametrizaValoresSinEjecutarSql() {
    var c =
        jdbcClientes.crear(
            new ClienteRequest(
                "20987654321",
                "O'Reilly; DROP TABLE clientes; --",
                "demo@example.com",
                "999111222",
                "Lima"));
    assertTrue(clienteService.obtener(c.id()).razonSocial().contains("DROP TABLE"));
    assertEquals(2, clientes.count());
  }

  @Test
  void pedidoBorradorSePuedeEditarConMismoProducto() throws Exception {
    long id =
        node(call("ventas", "POST", "/pedidos", order(2)).andExpect(status().isCreated()))
            .get("id")
            .asLong();
    call("ventas", "PUT", "/pedidos/" + id, order(3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(25.5))
        .andExpect(jsonPath("$.items[0].cantidad").value(3));
    assertEquals(10, productos.findById(productoId).orElseThrow().getStock());
    call("admin", "DELETE", "/pedidos/" + id, null).andExpect(status().isNoContent());
    call("admin", "GET", "/pedidos/" + id, null).andExpect(status().isNotFound());
  }

  @Test
  void confirmarDescuentaYCancelarDevuelveUnaSolaVez() throws Exception {
    var p = pedidoService.crear(order(3));
    call("ventas", "POST", "/pedidos/" + p.id() + "/confirmar", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    assertEquals(7, productoService.obtener(productoId).stock());
    call("ventas", "POST", "/pedidos/" + p.id() + "/confirmar", null)
        .andExpect(status().isConflict());
    call("admin", "DELETE", "/pedidos/" + p.id(), null).andExpect(status().isConflict());
    call("ventas", "POST", "/pedidos/" + p.id() + "/cancelar", null).andExpect(status().isOk());
    assertEquals(10, productoService.obtener(productoId).stock());
    call("ventas", "POST", "/pedidos/" + p.id() + "/cancelar", null)
        .andExpect(status().isConflict());
    assertEquals(10, productoService.obtener(productoId).stock());
    call("admin", "DELETE", "/pedidos/" + p.id(), null).andExpect(status().isNoContent());
  }

  @Test
  void rollbackRevierteDescuentoPrevioSiOtraLineaNoTieneStock() throws Exception {
    var segundo = productoService.crear(pr("TEST-2", 0));
    var p =
        pedidoService.crear(
            new PedidoRequest(
                clienteId,
                List.of(
                    new PedidoRequest.Item(productoId, 3),
                    new PedidoRequest.Item(segundo.id(), 1))));
    call("ventas", "POST", "/pedidos/" + p.id() + "/confirmar", null)
        .andExpect(status().isConflict());
    assertEquals(10, productoService.obtener(productoId).stock());
    assertEquals(0, productoService.obtener(segundo.id()).stock());
    assertEquals(EstadoPedido.BORRADOR, pedidoService.obtener(p.id()).estado());
  }

  @Test
  void elPrecioDelPedidoNoCambiaSiCambiaElCatalogo() {
    var p = pedidoService.crear(order(2));
    productoService.actualizar(
        productoId,
        new ProductoRequest("TEST-1", "Aceite", "Alimentos", new BigDecimal("99.00"), 10));
    var confirmado = pedidoService.confirmar(p.id());
    assertEquals(0, new BigDecimal("17.00").compareTo(confirmado.total()));
  }

  @Test
  void jpqlFiltraPorClienteYEstado() throws Exception {
    var p = pedidoService.crear(order(1));
    pedidoService.crear(order(2));
    pedidoService.confirmar(p.id());
    call("admin", "GET", "/pedidos?clienteId=" + clienteId + "&estado=CONFIRMADO", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(p.id()));
    call("consulta", "GET", "/productos?texto=aceite", null)
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void rechazaSkuYRucDuplicados409() throws Exception {
    call("admin", "POST", "/productos", pr("TEST-1", 2)).andExpect(status().isConflict());
    call("admin", "POST", "/clientes", cr("20123456789")).andExpect(status().isConflict());
  }

  @Test
  void noEliminaReferenciasDeUnPedido() throws Exception {
    pedidoService.crear(order(2));
    call("admin", "DELETE", "/productos/" + productoId, null).andExpect(status().isConflict());
    call("admin", "DELETE", "/clientes/" + clienteId, null).andExpect(status().isConflict());
  }

  @Test
  void productoInvalido400() throws Exception {
    call("admin", "POST", "/productos", new ProductoRequest("X", "", "", new BigDecimal("-1"), -1))
        .andExpect(status().isBadRequest());
    assertEquals(1, productos.count());
  }

  @Test
  void clienteInvalido400() throws Exception {
    call("admin", "POST", "/clientes", new ClienteRequest("123", "", "correo", "x", ""))
        .andExpect(status().isBadRequest());
  }

  @Test
  void pedidoNoPermiteProductosDuplicados() throws Exception {
    call(
            "ventas",
            "POST",
            "/pedidos",
            new PedidoRequest(
                clienteId,
                List.of(
                    new PedidoRequest.Item(productoId, 1), new PedidoRequest.Item(productoId, 2))))
        .andExpect(status().isConflict());
    assertEquals(0, pedidos.count());
  }

  @Test
  void pedidoNoPermiteClienteInexistente() throws Exception {
    call(
            "ventas",
            "POST",
            "/pedidos",
            new PedidoRequest(999999L, List.of(new PedidoRequest.Item(productoId, 1))))
        .andExpect(status().isNotFound());
  }

  @Test
  void pedidoNoPermiteProductoInexistente() throws Exception {
    call(
            "ventas",
            "POST",
            "/pedidos",
            new PedidoRequest(clienteId, List.of(new PedidoRequest.Item(999999L, 1))))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "[]",
        "[null]",
        "[{\"productoId\":1,\"cantidad\":0}]",
        "[{\"productoId\":null,\"cantidad\":1}]"
      })
  void validaItemsAnidados(String items) throws Exception {
    mvc.perform(
            post("/api/pedidos")
                .header("Authorization", "Bearer " + tokens.get("ventas"))
                .contentType("application/json")
                .content("{\"clienteId\":" + clienteId + ",\"items\":" + items + "}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void estadoDesconocido400() throws Exception {
    call("admin", "GET", "/pedidos?estado=OTRO", null).andExpect(status().isBadRequest());
  }

  @Test
  void dosPedidosConcurrentesNoSobrevenden() throws Exception {
    productoService.actualizar(productoId, pr("TEST-1", 5));
    var p1 = pedidoService.crear(order(4));
    var p2 = pedidoService.crear(order(4));
    var latch = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var a = executor.submit(() -> confirmConcurrent(p1.id(), latch));
      var b = executor.submit(() -> confirmConcurrent(p2.id(), latch));
      latch.countDown();
      int ok = (a.get(20, TimeUnit.SECONDS) ? 1 : 0) + (b.get(20, TimeUnit.SECONDS) ? 1 : 0);
      assertEquals(1, ok);
    }
    assertEquals(1, productoService.obtener(productoId).stock());
  }

  private boolean confirmConcurrent(Long id, CountDownLatch latch) throws Exception {
    latch.await();
    try {
      pedidoService.confirmar(id);
      return true;
    } catch (BusinessException ex) {
      assertEquals(409, ex.getStatus().value());
      return false;
    }
  }

  @Test
  void formularioWebExigeCsrf() throws Exception {
    mvc.perform(post("/clientes/guardar").with(user("admin").roles("ADMIN")))
        .andExpect(status().isForbidden());
  }

  @Test
  void formularioWebJdbcGuardaClienteConCsrf() throws Exception {
    mvc.perform(
            post("/clientes/guardar")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("ruc", "20987654321")
                .param("razonSocial", "Cliente web")
                .param("email", "web@example.com")
                .param("telefono", "999888777")
                .param("direccion", "Lima"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/clientes"));
    assertEquals(2, clientes.count());
  }

  @Test
  void formularioWebValidaAntesDeInsertar() throws Exception {
    mvc.perform(
            post("/clientes/guardar")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("ruc", "x")
                .param("razonSocial", " ")
                .param("email", "mal")
                .param("telefono", "x")
                .param("direccion", " "))
        .andExpect(status().isBadRequest());
    assertEquals(1, clientes.count());
  }

  @Test
  void ventasNoPuedeUsarFormularioClienteNiJsf() throws Exception {
    mvc.perform(get("/clientes").with(user("ventas").roles("VENTAS")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/jsf/cliente.xhtml").with(user("ventas").roles("VENTAS")))
        .andExpect(status().isForbidden());
  }
}
