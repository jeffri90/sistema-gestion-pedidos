# Frontend Angular — Sistema de Gestión de Pedidos

Interfaz Angular 21 que consume la API REST de Spring Boot del proyecto (tercer avance).

## Requisitos

- Node.js 20.19+, 22.12+ o 24+ y npm
- El backend en ejecución en `http://localhost:8080` (ver el README principal)

## Ejecución

```powershell
cd frontend
npm install
npm start
```

Abrir **http://localhost:4200**. Durante el desarrollo, `proxy.conf.json` reenvía `/api/**` al
backend en el puerto 8080, así que no hace falta configurar CORS.

Usuarios de demostración: `admin`, `ventas` y `consulta` (contraseñas en el README principal).

Compilación de producción: `npm run build` (salida en `dist/frontend`).

## Estructura

```
src/app/
├── app.ts, app.config.ts, app.routes.ts   Shell, proveedores (HttpClient + interceptores) y rutas
├── core/
│   ├── auth/      auth.service (login, JWT, roles), auth.interceptor (Bearer), auth.guards
│   ├── http/      error.interceptor (401 → cerrar sesión), http-error (traducción de errores)
│   ├── models/    Interfaces de los DTO del backend
│   ├── services/  producto.service, cliente.service, pedido.service
│   └── notificacion.service.ts   Avisos tipo toast
├── shared/        navbar, error-api, campo-error, notificaciones, validadores
└── features/
    ├── auth/          Login
    ├── inicio/        Resumen con métricas y permisos del rol
    ├── productos/     Lista con búsqueda JPQL + formulario crear/editar
    ├── clientes/      Lista con filtro + formulario crear/editar
    ├── pedidos/       Lista con filtros, formulario con líneas dinámicas, detalle con confirmar/cancelar/eliminar
    ├── diagnostico/   Página «Errores HTTP» que provoca 400/401/403/404/409 reales
    └── errores/       Páginas 403 (sin permiso) y 404 (ruta inexistente)
```

## Rutas

| Ruta | Acceso |
| --- | --- |
| `/login` | Público (solo sin sesión) |
| `/` | Cualquier rol |
| `/productos`, `/clientes`, `/pedidos`, `/pedidos/:id` | Cualquier rol |
| `/productos/nuevo`, `/productos/:id/editar`, `/clientes/nuevo`, `/clientes/:id/editar` | ADMIN |
| `/pedidos/nuevo`, `/pedidos/:id/editar` | ADMIN, VENTAS |
| `/diagnostico` | Cualquier rol |
| `/sin-permiso`, `**` | Páginas 403 y 404 |

Las guardas de Angular solo mejoran la experiencia. La seguridad real la aplica el backend con
JWT y roles, y la interfaz muestra sus respuestas 401 y 403.

## Manejo de errores

- **400**: mensaje general y errores por campo (`campo: mensaje` → debajo del control).
- **401**: en el login muestra «Credenciales incorrectas». En cualquier otra petición cierra la
  sesión y redirige a `/login?sesion=expirada`.
- **403**: «Acceso denegado» con el mensaje del backend.
- **404**: «No encontrado» (por ejemplo, `/pedidos/999999`).
- **409**: mensaje de la regla de negocio (SKU o RUC duplicado, stock insuficiente, pedido no
  editable, registro referenciado).
- **Sin conexión**: indica que el backend no responde en el puerto 8080.

La evidencia end-to-end está en `docs/evidencias/VERIFICACION-ANGULAR.md`.
