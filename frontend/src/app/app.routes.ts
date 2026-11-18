import { Routes } from '@angular/router';
import { authGuard, invitadoGuard, rolGuard } from './core/auth/auth.guards';

const ADMIN = { roles: ['ADMIN'] };
const GESTION_PEDIDOS = { roles: ['ADMIN', 'VENTAS'] };

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [invitadoGuard],
    title: 'Iniciar sesión',
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        title: 'Inicio',
        loadComponent: () => import('./features/inicio/inicio').then((m) => m.Inicio),
      },
      {
        path: 'productos',
        children: [
          {
            path: '',
            title: 'Productos',
            loadComponent: () => import('./features/productos/producto-lista').then((m) => m.ProductoLista),
          },
          {
            path: 'nuevo',
            title: 'Nuevo producto',
            canActivate: [rolGuard],
            data: ADMIN,
            loadComponent: () => import('./features/productos/producto-form').then((m) => m.ProductoForm),
          },
          {
            path: ':id/editar',
            title: 'Editar producto',
            canActivate: [rolGuard],
            data: ADMIN,
            loadComponent: () => import('./features/productos/producto-form').then((m) => m.ProductoForm),
          },
        ],
      },
      {
        path: 'clientes',
        children: [
          {
            path: '',
            title: 'Clientes',
            loadComponent: () => import('./features/clientes/cliente-lista').then((m) => m.ClienteLista),
          },
          {
            path: 'nuevo',
            title: 'Nuevo cliente',
            canActivate: [rolGuard],
            data: ADMIN,
            loadComponent: () => import('./features/clientes/cliente-form').then((m) => m.ClienteForm),
          },
          {
            path: ':id/editar',
            title: 'Editar cliente',
            canActivate: [rolGuard],
            data: ADMIN,
            loadComponent: () => import('./features/clientes/cliente-form').then((m) => m.ClienteForm),
          },
        ],
      },
      {
        path: 'pedidos',
        children: [
          {
            path: '',
            title: 'Pedidos',
            loadComponent: () => import('./features/pedidos/pedido-lista').then((m) => m.PedidoLista),
          },
          {
            path: 'nuevo',
            title: 'Nuevo pedido',
            canActivate: [rolGuard],
            data: GESTION_PEDIDOS,
            loadComponent: () => import('./features/pedidos/pedido-form').then((m) => m.PedidoForm),
          },
          {
            path: ':id',
            title: 'Detalle del pedido',
            loadComponent: () => import('./features/pedidos/pedido-detalle').then((m) => m.PedidoDetalle),
          },
          {
            path: ':id/editar',
            title: 'Editar pedido',
            canActivate: [rolGuard],
            data: GESTION_PEDIDOS,
            loadComponent: () => import('./features/pedidos/pedido-form').then((m) => m.PedidoForm),
          },
        ],
      },
      {
        path: 'diagnostico',
        title: 'Errores HTTP',
        loadComponent: () => import('./features/diagnostico/diagnostico').then((m) => m.Diagnostico),
      },
      {
        path: 'sin-permiso',
        title: 'Sin permiso',
        loadComponent: () => import('./features/errores/sin-permiso').then((m) => m.SinPermiso),
      },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () => import('./features/errores/no-encontrada').then((m) => m.NoEncontrada),
  },
];
