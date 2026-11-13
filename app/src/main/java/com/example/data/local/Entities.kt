package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
  tableName = "users",
  indices = [Index(value = ["email"], unique = true)],
)
data class UserEntity(
  @PrimaryKey val uid: String = "",
  val email: String = "",
  val nombre: String = "",
  val role: String = "empleado", // "admin" | "empleado"
  val passwordHash: String = "",
  val createdAt: Long = System.currentTimeMillis(),
) {
  val isAdmin: Boolean get() = role == "admin"
  val rolDisplay: String get() = if (isAdmin) "Administrador" else "Empleado"
}

@Entity(tableName = "medicamentos")
data class MedicamentoEntity(
  @PrimaryKey val id: String,
  val codigo: String,
  val nombre: String,
  val principio: String,
  val categoria: String,
  val caducidad: String, // yyyy-MM-dd
  val precioCompra: Double,
  val precioVenta: Double,
  val stock: Int,
)

@Entity(tableName = "proveedores")
data class ProveedorEntity(
  @PrimaryKey val id: String,
  val empresa: String,
  val contacto: String,
  val telefono: String,
  val correo: String,
  val direccion: String,
)

@Entity(tableName = "clientes")
data class ClienteEntity(
  @PrimaryKey val id: String,
  val nombre: String,
  val telefono: String,
  val nit: String,
  val puntos: Int,
)

@Entity(tableName = "compras")
data class CompraEntity(
  @PrimaryKey val id: String,
  val codigo: String,
  val fecha: Long,
  val proveedorId: String,
  val medId: String,
  val cantidad: Int,
  val costo: Double,
  val estado: String, // "Completado" | "Pendiente"
  val nota: String? = null,
)

@Entity(tableName = "ventas")
data class VentaEntity(
  @PrimaryKey val uuid: String,
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

@Entity(
  tableName = "venta_items",
  indices = [Index(value = ["ventaId"])],
)
data class VentaItemEntity(
  @PrimaryKey(autoGenerate = true) val itemId: Int = 0,
  val ventaId: String,
  val medId: String,
  val nombre: String,
  val cantidad: Int,
  val precio: Double,
)

data class VentaWithItems(
  @Embedded val venta: VentaEntity,
  @Relation(
    parentColumn = "uuid",
    entityColumn = "ventaId",
  )
  val items: List<VentaItemEntity>,
)
