package com.alicorp.pedidos.web;

import com.alicorp.pedidos.dto.ClienteRequest;
import com.alicorp.pedidos.exception.BusinessException;
import com.alicorp.pedidos.service.ClienteService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("clienteBean")
@RequestScope
public class ClienteBean {
  private final ClienteService service;
  private String ruc, razonSocial, email, telefono, direccion;

  public ClienteBean(@Qualifier("clienteJdbcService") ClienteService service) {
    this.service = service;
  }

  public String guardar() {
    try {
      service.crear(new ClienteRequest(ruc, razonSocial, email, telefono, direccion));
      return "/jsf/cliente.xhtml?faces-redirect=true&creado=true";
    } catch (ConstraintViolationException e) {
      message(
          e.getConstraintViolations().stream()
              .map(v -> v.getMessage())
              .sorted()
              .reduce((a, b) -> a + ". " + b)
              .orElse("Datos inválidos"));
    } catch (DataIntegrityViolationException e) {
      message("Ya existe un cliente con ese RUC");
    } catch (BusinessException e) {
      message(e.getMessage());
    }
    return null;
  }

  private void message(String text) {
    FacesContext.getCurrentInstance()
        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, text, null));
  }

  public String getRuc() {
    return ruc;
  }

  public void setRuc(String v) {
    ruc = v;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public void setRazonSocial(String v) {
    razonSocial = v;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String v) {
    email = v;
  }

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String v) {
    telefono = v;
  }

  public String getDireccion() {
    return direccion;
  }

  public void setDireccion(String v) {
    direccion = v;
  }
}
