package com.alicorp.pedidos.dao;

import com.alicorp.pedidos.dto.*;
import com.alicorp.pedidos.exception.BusinessException;
import java.sql.PreparedStatement;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClienteDao implements ClienteDao {
  private final JdbcTemplate jdbc;
  private final RowMapper<ClienteResponse> mapper =
      (rs, n) ->
          new ClienteResponse(
              rs.getLong("id"),
              rs.getString("ruc"),
              rs.getString("razon_social"),
              rs.getString("email"),
              rs.getString("telefono"),
              rs.getString("direccion"));

  public JdbcClienteDao(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<ClienteResponse> listar() {
    return jdbc.query(
        "select id,ruc,razon_social,email,telefono,direccion from clientes order by id", mapper);
  }

  public Optional<ClienteResponse> obtener(Long id) {
    return jdbc
        .query(
            "select id,ruc,razon_social,email,telefono,direccion from clientes where id=?",
            mapper,
            id)
        .stream()
        .findFirst();
  }

  public ClienteResponse crear(ClienteRequest r) {
    KeyHolder key = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement ps =
              connection.prepareStatement(
                  "insert into clientes(ruc,razon_social,email,telefono,direccion) values"
                      + " (?,?,?,?,?)",
                  new String[] {"id"});
          ps.setString(1, r.ruc());
          ps.setString(2, r.razonSocial());
          ps.setString(3, r.email());
          ps.setString(4, r.telefono());
          ps.setString(5, r.direccion());
          return ps;
        },
        key);
    return obtener(Objects.requireNonNull(key.getKey()).longValue()).orElseThrow();
  }

  public ClienteResponse actualizar(Long id, ClienteRequest r) {
    int rows =
        jdbc.update(
            "update clientes set ruc=?,razon_social=?,email=?,telefono=?,direccion=? where id=?",
            r.ruc(),
            r.razonSocial(),
            r.email(),
            r.telefono(),
            r.direccion(),
            id);
    if (rows == 0) throw BusinessException.missing("Cliente");
    return obtener(id).orElseThrow();
  }

  public void eliminar(Long id) {
    if (jdbc.update("delete from clientes where id=?", id) == 0)
      throw BusinessException.missing("Cliente");
  }
}
