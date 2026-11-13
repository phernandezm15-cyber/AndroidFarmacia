package com.example.data.remote

import com.example.data.local.ClienteEntity
import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.data.local.VentaEntity
import com.example.data.local.VentaItemEntity
import com.example.data.local.VentaWithItems
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDto(
  @DocumentId val id: String = "",
  val userId: String = "",
  val nombre: String = "",
  val email: String = "",
  val role: String = "empleado",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(): UserEntity = UserEntity(
    uid = userId.ifBlank { id },
    email = email,
    nombre = nombre,
    role = role,
    createdAt = (createdAt ?: updatedAt ?: Timestamp.now()).toDate().time,
  )
}

@IgnoreExtraProperties
data class MedicamentoDto(
  @DocumentId val id: String = "",
  val codigo: String = "",
  val nombre: String = "",
  val principio: String = "",
  val categoria: String = "",
  val caducidad: String = "",
  val precioCompra: Double = 0.0,
  val precioVenta: Double = 0.0,
  val stock: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(): MedicamentoEntity = MedicamentoEntity(
    id = id,
    codigo = codigo,
    nombre = nombre,
    principio = principio,
    categoria = categoria,
    caducidad = caducidad,
    precioCompra = precioCompra,
    precioVenta = precioVenta,
    stock = stock.toInt(),
  )

  companion object {
    fun toCreateMap(med: MedicamentoEntity): Map<String, Any> = mapOf(
      "codigo" to med.codigo,
      "nombre" to med.nombre,
      "principio" to med.principio,
      "categoria" to med.categoria,
      "caducidad" to med.caducidad,
      "precioCompra" to med.precioCompra,
      "precioVenta" to med.precioVenta,
      "stock" to med.stock.toLong(),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(med: MedicamentoEntity): Map<String, Any> = mapOf(
      "codigo" to med.codigo,
      "nombre" to med.nombre,
      "principio" to med.principio,
      "categoria" to med.categoria,
      "caducidad" to med.caducidad,
      "precioCompra" to med.precioCompra,
      "precioVenta" to med.precioVenta,
      "stock" to med.stock.toLong(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }
}

@IgnoreExtraProperties
data class ProveedorDto(
  @DocumentId val id: String = "",
  val empresa: String = "",
  val contacto: String = "",
  val telefono: String = "",
  val correo: String = "",
  val direccion: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(): ProveedorEntity = ProveedorEntity(
    id = id,
    empresa = empresa,
    contacto = contacto,
    telefono = telefono,
    correo = correo,
    direccion = direccion,
  )

  companion object {
    fun toCreateMap(p: ProveedorEntity): Map<String, Any> = mapOf(
      "empresa" to p.empresa,
      "contacto" to p.contacto,
      "telefono" to p.telefono,
      "correo" to p.correo,
      "direccion" to p.direccion,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(p: ProveedorEntity): Map<String, Any> = mapOf(
      "empresa" to p.empresa,
      "contacto" to p.contacto,
      "telefono" to p.telefono,
      "correo" to p.correo,
      "direccion" to p.direccion,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }
}

@IgnoreExtraProperties
data class ClienteDto(
  @DocumentId val id: String = "",
  val nombre: String = "",
  val telefono: String = "",
  val nit: String = "",
  val puntos: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(): ClienteEntity = ClienteEntity(
    id = id,
    nombre = nombre,
    telefono = telefono,
    nit = nit,
    puntos = puntos.toInt(),
  )

  companion object {
    fun toCreateMap(c: ClienteEntity): Map<String, Any> = mapOf(
      "nombre" to c.nombre,
      "telefono" to c.telefono,
      "nit" to c.nit,
      "puntos" to c.puntos.toLong(),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(c: ClienteEntity): Map<String, Any> = mapOf(
      "nombre" to c.nombre,
      "telefono" to c.telefono,
      "nit" to c.nit,
      "puntos" to c.puntos.toLong(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }
}

@IgnoreExtraProperties
data class CompraDto(
  @DocumentId val id: String = "",
  val codigo: String = "",
  val fecha: Long = 0L,
  val proveedorId: String = "",
  val medId: String = "",
  val cantidad: Long = 0L,
  val costo: Double = 0.0,
  val estado: String = "Pendiente",
  val nota: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(): CompraEntity = CompraEntity(
    id = id,
    codigo = codigo,
    fecha = fecha,
    proveedorId = proveedorId,
    medId = medId,
    cantidad = cantidad.toInt(),
    costo = costo,
    estado = estado,
    nota = nota,
  )

  companion object {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(c: CompraEntity): Map<String, Any> = mapOf(
      "codigo" to c.codigo,
      "fecha" to c.fecha,
      "proveedorId" to c.proveedorId,
      "medId" to c.medId,
      "cantidad" to c.cantidad.toLong(),
      "costo" to c.costo,
      "estado" to c.estado,
      "nota" to c.nota,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
  }
}

@IgnoreExtraProperties
data class VentaDto(
  @DocumentId val id: String = "",
  val codigo: String = "",
  val fecha: Long = 0L,
  val subtotal: Double = 0.0,
  val descuento: Double = 0.0,
  val descPct: Double = 0.0,
  val iva: Double = 0.0,
  val total: Double = 0.0,
  val vendedorNombre: String = "",
  val vendedorId: String? = null,
  val clienteId: String? = null,
  val puntos: Long = 0L,
  val items: List<Map<String, Any?>> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(): VentaWithItems {
    val venta = VentaEntity(
      uuid = id,
      codigo = codigo,
      fecha = fecha,
      subtotal = subtotal,
      descuento = descuento,
      descPct = descPct,
      iva = iva,
      total = total,
      vendedorNombre = vendedorNombre,
      vendedorId = vendedorId,
      clienteId = clienteId,
      puntos = puntos.toInt(),
    )
    val mappedItems = items.mapIndexed { idx, raw ->
      VentaItemEntity(
        itemId = idx + 1,
        ventaId = id,
        medId = raw["medId"] as? String ?: "",
        nombre = raw["nombre"] as? String ?: "",
        cantidad = (raw["cantidad"] as? Number)?.toInt() ?: 0,
        precio = (raw["precio"] as? Number)?.toDouble() ?: 0.0,
      )
    }
    return VentaWithItems(venta = venta, items = mappedItems)
  }

  companion object {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(venta: VentaEntity, items: List<VentaItemEntity>): Map<String, Any> {
      val itemMaps = items.map { item ->
        mapOf(
          "medId" to item.medId,
          "nombre" to item.nombre,
          "cantidad" to item.cantidad.toLong(),
          "precio" to item.precio,
        )
      }
      return mapOf(
        "codigo" to venta.codigo,
        "fecha" to venta.fecha,
        "subtotal" to venta.subtotal,
        "descuento" to venta.descuento,
        "descPct" to venta.descPct,
        "iva" to venta.iva,
        "total" to venta.total,
        "vendedorNombre" to venta.vendedorNombre,
        "vendedorId" to venta.vendedorId,
        "clienteId" to venta.clienteId,
        "puntos" to venta.puntos.toLong(),
        "items" to itemMaps,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null } as Map<String, Any>
    }
  }
}

@IgnoreExtraProperties
data class StaffMemberDto(
  @DocumentId val id: String = "",
  val nombre: String = "",
  val email: String = "",
  val role: String = "empleado",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(): UserEntity = UserEntity(
    uid = id,
    email = email,
    nombre = nombre,
    role = role,
    createdAt = (createdAt ?: updatedAt ?: Timestamp.now()).toDate().time,
  )
}
