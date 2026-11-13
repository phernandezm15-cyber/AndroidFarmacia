package com.example.data.remote

import com.example.data.local.ClienteEntity
import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.data.local.VentaEntity
import com.example.data.local.VentaItemEntity
import com.example.data.local.VentaWithItems
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseUserRow(
  val uid: String,
  val email: String,
  val nombre: String,
  val role: String,
  @Json(name = "password_hash") val passwordHash: String = "",
  @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
) {
  fun toEntity() = UserEntity(
    uid = uid,
    email = email,
    nombre = nombre,
    role = role,
    passwordHash = passwordHash,
    createdAt = createdAt,
  )

  companion object {
    fun fromEntity(u: UserEntity) = SupabaseUserRow(
      uid = u.uid,
      email = u.email,
      nombre = u.nombre,
      role = u.role,
      passwordHash = u.passwordHash,
      createdAt = u.createdAt,
    )
  }
}

@JsonClass(generateAdapter = true)
data class PasswordHashPatch(
  @Json(name = "password_hash") val passwordHash: String,
)

@JsonClass(generateAdapter = true)
data class SupabaseMedicamentoRow(
  val id: String,
  val codigo: String,
  val nombre: String,
  val principio: String,
  val categoria: String,
  val caducidad: String,
  @Json(name = "precio_compra") val precioCompra: Double,
  @Json(name = "precio_venta") val precioVenta: Double,
  val stock: Int,
) {
  fun toEntity() = MedicamentoEntity(
    id = id,
    codigo = codigo,
    nombre = nombre,
    principio = principio,
    categoria = categoria,
    caducidad = caducidad,
    precioCompra = precioCompra,
    precioVenta = precioVenta,
    stock = stock,
  )

  companion object {
    fun fromEntity(m: MedicamentoEntity) = SupabaseMedicamentoRow(
      id = m.id,
      codigo = m.codigo,
      nombre = m.nombre,
      principio = m.principio,
      categoria = m.categoria,
      caducidad = m.caducidad,
      precioCompra = m.precioCompra,
      precioVenta = m.precioVenta,
      stock = m.stock,
    )
  }
}

@JsonClass(generateAdapter = true)
data class SupabaseProveedorRow(
  val id: String,
  val empresa: String,
  val contacto: String,
  val telefono: String,
  val correo: String,
  val direccion: String,
) {
  fun toEntity() = ProveedorEntity(id, empresa, contacto, telefono, correo, direccion)

  companion object {
    fun fromEntity(p: ProveedorEntity) = SupabaseProveedorRow(
      p.id, p.empresa, p.contacto, p.telefono, p.correo, p.direccion,
    )
  }
}

@JsonClass(generateAdapter = true)
data class SupabaseClienteRow(
  val id: String,
  val nombre: String,
  val telefono: String,
  val nit: String,
  val puntos: Int,
) {
  fun toEntity() = ClienteEntity(id, nombre, telefono, nit, puntos)

  companion object {
    fun fromEntity(c: ClienteEntity) = SupabaseClienteRow(c.id, c.nombre, c.telefono, c.nit, c.puntos)
  }
}

@JsonClass(generateAdapter = true)
data class SupabaseCompraRow(
  val id: String,
  val codigo: String,
  val fecha: Long,
  @Json(name = "proveedor_id") val proveedorId: String,
  @Json(name = "med_id") val medId: String,
  val cantidad: Int,
  val costo: Double,
  val estado: String,
  val nota: String? = null,
) {
  fun toEntity() = CompraEntity(id, codigo, fecha, proveedorId, medId, cantidad, costo, estado, nota)

  companion object {
    fun fromEntity(c: CompraEntity) = SupabaseCompraRow(
      c.id, c.codigo, c.fecha, c.proveedorId, c.medId, c.cantidad, c.costo, c.estado, c.nota,
    )
  }
}

@JsonClass(generateAdapter = true)
data class SupabaseVentaItemRow(
  @Json(name = "item_id") val itemId: Int? = null,
  @Json(name = "venta_id") val ventaId: String,
  @Json(name = "med_id") val medId: String,
  val nombre: String,
  val cantidad: Int,
  val precio: Double,
) {
  fun toEntity() = VentaItemEntity(
    itemId = itemId ?: 0,
    ventaId = ventaId,
    medId = medId,
    nombre = nombre,
    cantidad = cantidad,
    precio = precio,
  )
}

@JsonClass(generateAdapter = true)
data class SupabaseVentaItemInsert(
  @Json(name = "venta_id") val ventaId: String,
  @Json(name = "med_id") val medId: String,
  val nombre: String,
  val cantidad: Int,
  val precio: Double,
)

@JsonClass(generateAdapter = true)
data class SupabaseVentaRow(
  val uuid: String,
  val codigo: String,
  val fecha: Long,
  val subtotal: Double,
  val descuento: Double,
  @Json(name = "desc_pct") val descPct: Double,
  val iva: Double,
  val total: Double,
  @Json(name = "vendedor_nombre") val vendedorNombre: String,
  @Json(name = "vendedor_id") val vendedorId: String? = null,
  @Json(name = "cliente_id") val clienteId: String? = null,
  val puntos: Int = 0,
  @Json(name = "venta_items") val ventaItems: List<SupabaseVentaItemRow>? = null,
) {
  fun toVentaWithItems(): VentaWithItems = VentaWithItems(
    venta = VentaEntity(
      uuid = uuid,
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
      puntos = puntos,
    ),
    items = ventaItems.orEmpty().map { it.toEntity() },
  )

  companion object {
    fun fromVenta(v: VentaEntity) = SupabaseVentaRow(
      uuid = v.uuid,
      codigo = v.codigo,
      fecha = v.fecha,
      subtotal = v.subtotal,
      descuento = v.descuento,
      descPct = v.descPct,
      iva = v.iva,
      total = v.total,
      vendedorNombre = v.vendedorNombre,
      vendedorId = v.vendedorId,
      clienteId = v.clienteId,
      puntos = v.puntos,
      ventaItems = null,
    )
  }
}

@JsonClass(generateAdapter = true)
data class RegistrarCompraRpcRequest(
  @Json(name = "p_id") val id: String,
  @Json(name = "p_codigo") val codigo: String,
  @Json(name = "p_fecha") val fecha: Long,
  @Json(name = "p_proveedor_id") val proveedorId: String,
  @Json(name = "p_med_id") val medId: String,
  @Json(name = "p_cantidad") val cantidad: Int,
  @Json(name = "p_costo") val costo: Double,
  @Json(name = "p_estado") val estado: String,
  @Json(name = "p_nota") val nota: String? = null,
)

@JsonClass(generateAdapter = true)
data class RecibirCompraRpcRequest(
  @Json(name = "p_compra_id") val compraId: String,
)

@JsonClass(generateAdapter = true)
data class SaleItemRpcPayload(
  @Json(name = "med_id") val medId: String,
  val nombre: String,
  val cantidad: Int,
  val precio: Double,
)

@JsonClass(generateAdapter = true)
data class RegistrarVentaRpcRequest(
  @Json(name = "p_uuid") val uuid: String,
  @Json(name = "p_codigo") val codigo: String,
  @Json(name = "p_fecha") val fecha: Long,
  @Json(name = "p_subtotal") val subtotal: Double,
  @Json(name = "p_descuento") val descuento: Double,
  @Json(name = "p_desc_pct") val descPct: Double,
  @Json(name = "p_iva") val iva: Double,
  @Json(name = "p_total") val total: Double,
  @Json(name = "p_vendedor_nombre") val vendedorNombre: String,
  @Json(name = "p_vendedor_id") val vendedorId: String?,
  @Json(name = "p_cliente_id") val clienteId: String?,
  @Json(name = "p_puntos") val puntos: Int,
  @Json(name = "p_items") val items: List<SaleItemRpcPayload>,
)

@JsonClass(generateAdapter = true)
data class PostgrestErrorBody(
  val message: String? = null,
  val details: String? = null,
  val hint: String? = null,
  val code: String? = null,
)
