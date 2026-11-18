# Verificación del tercer avance — Frontend Angular (18 de noviembre de 2026)

Entorno: Windows, Node 24.11.1, Angular 21.2, JDK 25.0.1, Spring Boot 4.1.1.
El backend se ejecutó con una base H2 **en disco** separada (`data/angular-e2e`) para no mezclar
datos de prueba con la base predeterminada:

```powershell
$env:DB_URL = "jdbc:h2:file:./data/angular-e2e;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE"
.\mvnw.cmd spring-boot:run
```

El frontend se ejecutó con `npm start` en `frontend/` (http://localhost:4200). El proxy
`frontend/proxy.conf.json` reenvía `/api/**` a `http://localhost:8080`, de modo que el navegador
consume la API real de Spring Boot.

## Resumen

| Requisito | Resultado | Evidencia |
| --- | --- | --- |
| Proyecto organizado | `core/` (auth, http, models, services), `shared/` (componentes reutilizables), `features/` (una carpeta por pantalla). Compila con `ng build` sin errores ni advertencias. | `frontend/src/app` |
| Rutas principales | 14 rutas con carga diferida, `authGuard`, `rolGuard`, `invitadoGuard`, página 403 y página 404. | `app.routes.ts`, captura 15 |
| Formularios con validación | Validaciones iguales a las de los DTO (SKU, RUC de 11 dígitos, teléfono, correo, precio ≥ 0.01 con 2 decimales, stock entero, cantidad 1–10000, producto repetido). Los mensajes aparecen debajo de cada campo. | capturas 03 y 08 |
| Consumo real de API REST | `HttpClient` contra `/api/auth`, `/api/productos`, `/api/clientes` y `/api/pedidos` (GET, POST, PUT, DELETE, confirmar y cancelar). | capturas 02–12 |
| CRUD visible | Productos y clientes: listar, buscar, crear, editar y eliminar. Pedidos: crear borrador con varias líneas, editar, confirmar, cancelar y eliminar. | capturas 04, 06, 09, 11, 17 |
| Manejo de errores HTTP | Componente `<app-error-api>` para 400, 401, 403, 404 y 409, con el mensaje del backend. Los 400 se asocian a los campos del formulario. Un 401 cierra la sesión y lleva al login. | capturas 01, 05, 10, 12–16 |
| Autenticación desde Angular | Login contra `/api/auth/login`. El JWT se guarda en `sessionStorage`, el `authInterceptor` lo envía como `Authorization: Bearer` y la interfaz oculta las acciones según el rol (ADMIN, VENTAS o CONSULTA). | capturas 02, 13–16 |
| Evidencia end-to-end | El flujo completo se probó desde el navegador hasta la base de datos, y se consultó H2 directamente (ver más abajo). | capturas 04–12, 17 |

## Flujo end-to-end ejecutado

Todos los pasos se hicieron desde la interfaz Angular en el navegador.

1. **Login con contraseña incorrecta**: se muestra *No autenticado (401) – Credenciales incorrectas*. (01)
2. **Login como `admin`**: el resumen se carga desde la API con 3 productos y 1 cliente de demostración. (02)
3. **Producto con datos inválidos**: Angular bloquea el envío y muestra un error en cada campo. (03)
4. **Producto `E2E-ANG-01`** con stock 5 y precio S/ 4.50: se crea (201) y aparece en la lista. (04)
5. **Mismo SKU otra vez**: *Conflicto (409) – SKU o RUC duplicado…*. (05)
6. **Cliente `Bodega Prueba Angular S.A.C.`** (RUC 20555000111): se crea. (06)
7. **Pedido nuevo**: muestra un aviso cuando la cantidad supera el stock (07) y un error si se repite un producto (08).
8. **Pedido #1 en BORRADOR**: 6 × E2E-ANG-01 y 2 × ALI-001, total S/ 44.00. (09)
9. **Confirmar el pedido #1**: *Conflicto (409) – Stock insuficiente para Galletas de prueba Angular*. El pedido sigue en BORRADOR, porque el backend hace rollback. (10)
10. **Editar el borrador a 3 unidades y confirmar**: estado CONFIRMADO con total S/ 30.50. El stock baja de 5 a 2 y de 120 a 118. (11)
11. **Eliminar el producto `E2E-ANG-01`**: *Conflicto (409)*, porque el producto está en un pedido. (12)
12. **Página «Errores HTTP» como admin**: 400 (lista de campos), 401 (token falso, sin cerrar la sesión real), 404 y 409. El caso 403 devuelve 404 para ADMIN, como está previsto. (13)
13. **Como `ventas`**: el caso 403 muestra *Acceso denegado (403)* (14). Al abrir `/productos/nuevo`, el `rolGuard` muestra la página 403. (15)
14. **Token alterado en `sessionStorage`**: el backend responde 401 y Angular cierra la sesión y redirige a `/login?sesion=expirada`. (16)
15. **Como `consulta`**: no se muestran «Nuevo pedido», «Nuevo producto», «Editar» ni «Eliminar».
16. **Persistencia**: se detuvo Spring Boot, se consultó H2 (abajo) y se volvió a iniciar. El pedido #1 seguía CONFIRMADO. Al cancelarlo desde Angular, el stock volvió a 5 y 120. (17)
17. **Limpieza desde la interfaz**: se eliminaron el pedido #1, el producto `E2E-ANG-01` y el cliente de prueba.

## Consulta directa a la base de datos (paso 16)

Ejecutada con `org.h2.tools.Shell` (H2 2.4.240) mientras el backend estaba detenido:

```text
SELECT id, sku, nombre, stock FROM productos ORDER BY id
ID | SKU        | NOMBRE                     | STOCK
1  | ALI-001    | Aceite vegetal 1 L         | 118
2  | ALI-002    | Fideos 500 g               | 200
3  | ALI-003    | Detergente 900 g           | 80
4  | E2E-ANG-01 | Galletas de prueba Angular | 2

SELECT id, ruc, razon_social FROM clientes ORDER BY id
ID | RUC         | RAZON_SOCIAL
1  | 20999999991 | Distribuidora Demo S.A.C.
2  | 20555000111 | Bodega Prueba Angular S.A.C.

SELECT * FROM pedidos
ID | ESTADO     | FECHA                      | TOTAL | VERSION | CLIENTE_ID
1  | CONFIRMADO | 2026-11-18 12:26:44.135992 | 30.50 | 3       | 2

SELECT d.pedido_id, p.sku, d.cantidad, d.precio_unitario
FROM detalle_pedido d JOIN productos p ON p.id = d.producto_id
PEDIDO_ID | SKU        | CANTIDAD | PRECIO_UNITARIO
1         | ALI-001    | 2        | 8.50
1         | E2E-ANG-01 | 3        | 4.50
```

Los registros creados en Angular (producto 4, cliente 2 y pedido 1 con dos líneas) están en las
tablas, y el stock refleja el descuento hecho al confirmar.

## Limitaciones

- Las capturas se tomaron con el navegador integrado del entorno de desarrollo. Las ventanas de
  confirmación nativas (`confirm()`) se aceptaron automáticamente durante la prueba.
- No se agregaron pruebas unitarias de Angular. La verificación del frontend es la prueba
  end-to-end manual de este documento.
- La prueba se hizo con H2 en disco. No se ejecutó contra PostgreSQL.
