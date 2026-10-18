<%@ page pageEncoding="UTF-8" %>
<%@ include file="fragments/header.jspf" %>
<div class="page-heading"><div><p class="eyebrow">CATÁLOGO</p><h1>${empty producto ? 'Nuevo producto' : 'Editar producto'}</h1><p class="text-secondary">Completa los datos del producto. Los importes se expresan en soles.</p></div></div>
<form method="post" action="${b}/productos/guardar" class="panel form-panel"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><c:if test="${not empty producto}"><input type="hidden" name="id" value="${producto.id()}"></c:if><div class="row g-4">
<div class="col-md-4"><label for="sku" class="form-label">SKU</label><input id="sku" name="sku" class="form-control" required pattern="[A-Za-z0-9\-]{2,30}" maxlength="30" value="${empty producto ? '' : fn:escapeXml(producto.sku())}"><div class="form-text">Código único, por ejemplo ALI-004.</div></div>
<div class="col-md-8"><label for="nombre" class="form-label">Nombre</label><input id="nombre" name="nombre" class="form-control" required maxlength="120" value="${empty producto ? '' : fn:escapeXml(producto.nombre())}"></div>
<div class="col-md-6"><label for="categoria" class="form-label">Categoría</label><input id="categoria" name="categoria" class="form-control" required maxlength="60" value="${empty producto ? '' : fn:escapeXml(producto.categoria())}"></div>
<div class="col-md-3"><label for="precio" class="form-label">Precio unitario (S/)</label><input id="precio" name="precio" type="number" class="form-control" min="0.01" max="9999999999.99" step="0.01" required value="${empty producto ? '' : producto.precio()}"></div>
<div class="col-md-3"><label for="stock" class="form-label">Stock</label><input id="stock" name="stock" type="number" class="form-control" min="0" max="1000000" required value="${empty producto ? 0 : producto.stock()}"></div>
</div><div class="form-actions"><button class="btn btn-primary">Guardar producto</button><a class="btn btn-light" href="${b}/productos">Volver al catálogo</a></div></form>

<%@ include file="fragments/footer.jspf" %>
