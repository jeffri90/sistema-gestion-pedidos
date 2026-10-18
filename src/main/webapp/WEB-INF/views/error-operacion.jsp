<%@ page pageEncoding="UTF-8" %>
<%@ include file="fragments/header.jspf" %>
<div class="page-heading"><div><p class="eyebrow">REVISIÓN NECESARIA</p><h1>No se pudo completar la operación</h1><p class="text-secondary">Corrige los datos o revisa el estado del registro.</p></div></div><div class="alert alert-danger"><c:out value="${error}"/></div><div class="d-flex gap-2"><a class="btn btn-primary" href="${b}/">Ir al resumen</a><a class="btn btn-outline-secondary" href="${b}/pedidos">Ver pedidos</a></div>
<%@ include file="fragments/footer.jspf" %>
