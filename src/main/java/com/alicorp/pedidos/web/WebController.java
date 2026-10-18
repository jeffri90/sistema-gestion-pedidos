package com.alicorp.pedidos.web;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.exception.BusinessException;
import com.alicorp.pedidos.model.EstadoPedido;
import com.alicorp.pedidos.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {
  private final ProductoService productos;
  private final ClienteService clientes;
  private final PedidoService pedidos;

  public WebController(
      ProductoService productos,
      @Qualifier("clienteJdbcService") ClienteService clientes,
      PedidoService pedidos) {
    this.productos = productos;
    this.clientes = clientes;
    this.pedidos = pedidos;
  }

  @ModelAttribute
  void common(Model model, HttpServletRequest request) {
    model.addAttribute("admin", request.isUserInRole("ADMIN"));
    model.addAttribute("operador", request.isUserInRole("ADMIN") || request.isUserInRole("VENTAS"));
  }

  @GetMapping("/login")
  String login() {
    return "login";
  }

  @GetMapping("/")
  String dashboard(Model m) {
    var ps = pedidos.listar(null, null);
    m.addAttribute("productos", productos.listar(""));
    m.addAttribute("clientes", clientes.listar());
    m.addAttribute("pedidos", ps);
    m.addAttribute(
        "pendientes", ps.stream().filter(p -> p.estado() == EstadoPedido.BORRADOR).count());
    m.addAttribute(
        "confirmados", ps.stream().filter(p -> p.estado() == EstadoPedido.CONFIRMADO).count());
    return "inicio";
  }

  @GetMapping("/productos")
  String productos(@RequestParam(defaultValue = "") String texto, Model m) {
    m.addAttribute("productos", productos.listar(texto));
    m.addAttribute("texto", texto);
    return "productos";
  }

  @GetMapping("/productos/nuevo")
  String nuevoProducto() {
    return "producto-form";
  }

  @GetMapping("/productos/{id}/editar")
  String editarProducto(@PathVariable Long id, Model m) {
    m.addAttribute("producto", productos.obtener(id));
    return "producto-form";
  }

  @PostMapping("/productos/guardar")
  String guardarProducto(
      @RequestParam(required = false) Long id,
      @RequestParam String sku,
      @RequestParam String nombre,
      @RequestParam String categoria,
      @RequestParam BigDecimal precio,
      @RequestParam Integer stock,
      RedirectAttributes flash) {
    var r = new ProductoRequest(sku, nombre, categoria, precio, stock);
    if (id == null) productos.crear(r);
    else productos.actualizar(id, r);
    flash.addFlashAttribute("mensaje", "Producto guardado");
    return "redirect:/productos";
  }

  @PostMapping("/productos/{id}/eliminar")
  String eliminarProducto(@PathVariable Long id, RedirectAttributes flash) {
    productos.eliminar(id);
    flash.addFlashAttribute("mensaje", "Producto eliminado");
    return "redirect:/productos";
  }

  @GetMapping("/clientes")
  String clientes(Model m) {
    m.addAttribute("clientes", clientes.listar());
    return "clientes";
  }

  @GetMapping("/clientes/nuevo")
  String nuevoCliente() {
    return "cliente-form";
  }

  @GetMapping("/clientes/{id}/editar")
  String editarCliente(@PathVariable Long id, Model m) {
    m.addAttribute("cliente", clientes.obtener(id));
    return "cliente-form";
  }

  @PostMapping("/clientes/guardar")
  String guardarCliente(
      @RequestParam(required = false) Long id,
      @RequestParam String ruc,
      @RequestParam String razonSocial,
      @RequestParam String email,
      @RequestParam String telefono,
      @RequestParam String direccion,
      RedirectAttributes flash) {
    var r = new ClienteRequest(ruc, razonSocial, email, telefono, direccion);
    if (id == null) clientes.crear(r);
    else clientes.actualizar(id, r);
    flash.addFlashAttribute("mensaje", "Cliente guardado");
    return "redirect:/clientes";
  }

  @PostMapping("/clientes/{id}/eliminar")
  String eliminarCliente(@PathVariable Long id, RedirectAttributes flash) {
    clientes.eliminar(id);
    flash.addFlashAttribute("mensaje", "Cliente eliminado");
    return "redirect:/clientes";
  }

  @GetMapping("/pedidos")
  String pedidos(
      @RequestParam(required = false) Long clienteId,
      @RequestParam(required = false) EstadoPedido estado,
      Model m) {
    m.addAttribute("pedidos", pedidos.listar(clienteId, estado));
    m.addAttribute("clientes", clientes.listar());
    m.addAttribute("estado", estado);
    m.addAttribute("clienteId", clienteId);
    return "pedidos";
  }

  @GetMapping("/pedidos/nuevo")
  String nuevoPedido(Model m) {
    catalogos(m);
    return "pedido-form";
  }

  @GetMapping("/pedidos/{id}/editar")
  String editarPedido(@PathVariable Long id, Model m) {
    var p = pedidos.obtener(id);
    if (p.estado() != EstadoPedido.BORRADOR)
      throw BusinessException.conflict("Solo se editan borradores");
    if (p.items().size() > 4)
      throw BusinessException.conflict("Edite pedidos de más de cuatro líneas mediante la API");
    m.addAttribute("pedido", p);
    catalogos(m);
    return "pedido-form";
  }

  private void catalogos(Model m) {
    m.addAttribute("productos", productos.listar(""));
    m.addAttribute("clientes", clientes.listar());
  }

  @PostMapping("/pedidos/guardar")
  String guardarPedido(
      @RequestParam(required = false) Long id,
      @RequestParam Long clienteId,
      @RequestParam List<Long> productoId,
      @RequestParam List<Integer> cantidad,
      RedirectAttributes flash) {
    if (productoId.size() != cantidad.size())
      throw BusinessException.conflict("Revise las líneas del pedido");
    List<PedidoRequest.Item> items = new ArrayList<>();
    for (int i = 0; i < productoId.size(); i++)
      if (productoId.get(i) != null)
        items.add(new PedidoRequest.Item(productoId.get(i), cantidad.get(i)));
    var r = new PedidoRequest(clienteId, items);
    var p = id == null ? pedidos.crear(r) : pedidos.actualizar(id, r);
    flash.addFlashAttribute("mensaje", "Pedido guardado como borrador");
    return "redirect:/pedidos/" + p.id();
  }

  @GetMapping("/pedidos/{id}")
  String verPedido(@PathVariable Long id, Model m) {
    m.addAttribute("pedido", pedidos.obtener(id));
    return "pedido-detalle";
  }

  @PostMapping("/pedidos/{id}/confirmar")
  String confirmar(@PathVariable Long id, RedirectAttributes flash) {
    pedidos.confirmar(id);
    flash.addFlashAttribute("mensaje", "Pedido confirmado. Stock actualizado.");
    return "redirect:/pedidos/" + id;
  }

  @PostMapping("/pedidos/{id}/cancelar")
  String cancelar(@PathVariable Long id, RedirectAttributes flash) {
    pedidos.cancelar(id);
    flash.addFlashAttribute("mensaje", "Pedido cancelado");
    return "redirect:/pedidos/" + id;
  }

  @PostMapping("/pedidos/{id}/eliminar")
  String eliminarPedido(@PathVariable Long id, RedirectAttributes flash) {
    pedidos.eliminar(id);
    flash.addFlashAttribute("mensaje", "Pedido eliminado");
    return "redirect:/pedidos";
  }
}
