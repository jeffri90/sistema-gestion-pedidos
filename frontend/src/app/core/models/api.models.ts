/** Contratos JSON del backend Spring Boot (paquete com.alicorp.pedidos.dto). */

export type Rol = 'ADMIN' | 'VENTAS' | 'CONSULTA';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  roles: Rol[];
}

export interface Producto {
  id: number;
  sku: string;
  nombre: string;
  categoria: string;
  precio: number;
  stock: number;
}

export type ProductoRequest = Omit<Producto, 'id'>;

export interface Cliente {
  id: number;
  ruc: string;
  razonSocial: string;
  email: string;
  telefono: string;
  direccion: string;
}

export type ClienteRequest = Omit<Cliente, 'id'>;

export type EstadoPedido = 'BORRADOR' | 'CONFIRMADO' | 'CANCELADO';

export interface PedidoItem {
  productoId: number;
  producto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Pedido {
  id: number;
  clienteId: number;
  cliente: string;
  fecha: string;
  estado: EstadoPedido;
  total: number;
  items: PedidoItem[];
}

export interface PedidoRequest {
  clienteId: number;
  items: { productoId: number; cantidad: number }[];
}

/** Cuerpo de error común (ApiError y SecurityConfig.jsonError). */
export interface ApiError {
  timestamp?: string;
  status: number;
  message: string;
  errors: string[];
}
