package com.example.data.local

data class UserEntity(
  val uid: String = "",
  val email: String = "",
  val nombre: String = "",
  val role: String = "empleado", // "admin" | "empleado"
  val createdAt: Long = System.currentTimeMillis(),
) {
  val isAdmin: Boolean get() = role == "admin"
  val rolDisplay: String get() = if (isAdmin) "Administrador" else "Empleado"
}

data class MedicamentoEntity(
  val id: String,
  val codigo: String,
  val nombre: String,
  val principio: String,
  val categoria: String,
  val caducidad: String, // yyyy-MM-dd
  val precioCompra: Double,
  val precioVenta: Double,
  val stock: Int,
)

data class ProveedorEntity(
  val id: String,
  val empresa: String,
  val contacto: String,
  val telefono: String,
  val correo: String,
  val direccion: String,
)

data class ClienteEntity(
  val id: String,
  val nombre: String,
  val telefono: String,
  val nit: String,
  val puntos: Int,
)

data class CompraEntity(
  val id: String,
  val codigo: String,
  val fecha: Long,
  val proveedorId: String,
  val medId: String,
  val cantidad: Int,
  val costo: Double,
  val estado: String, // "Completado" | "Pendiente"
  val nota: String? = null,
)

data class VentaEntity(
  val uuid: String,
  val codigo: String,
  val fecha: Long,
  val subtotal: Double,
  val descuento: Double,
  val descPct: Double,
  val iva: Double,
  val total: Double,
  val vendedorNombre: String,
  val vendedorId: String?,
  val clienteId: String?,
  val puntos: Int,
)

data class VentaItemEntity(
  val itemId: Int = 0,
  val ventaId: String,
  val medId: String,
  val nombre: String,
  val cantidad: Int,
  val precio: Double,
)

data class VentaWithItems(
  val venta: VentaEntity,
  val items: List<VentaItemEntity>,
)
