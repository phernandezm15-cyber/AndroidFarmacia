package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.repository.CartLineItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmaciaDao {
  // Users & Authentication
  @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
  fun observeUserById(uid: String): Flow<UserEntity?>

  @Query("SELECT * FROM users ORDER BY nombre ASC")
  fun observeAllUsers(): Flow<List<UserEntity>>

  @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
  suspend fun findUserByEmail(email: String): UserEntity?

  @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
  suspend fun findUserById(uid: String): UserEntity?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertUser(user: UserEntity)

  @Query("UPDATE users SET passwordHash = :passwordHash WHERE uid = :uid")
  suspend fun updateUserPasswordHash(uid: String, passwordHash: String): Int

  // Medicamentos
  @Query("SELECT * FROM medicamentos ORDER BY id ASC")
  fun observeMedicamentos(): Flow<List<MedicamentoEntity>>

  @Query("SELECT COUNT(*) FROM medicamentos")
  suspend fun countMedicamentos(): Int

  @Query("SELECT * FROM medicamentos WHERE id = :medId LIMIT 1")
  suspend fun findMedicamentoById(medId: String): MedicamentoEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMedicamento(med: MedicamentoEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMedicamentos(meds: List<MedicamentoEntity>)

  @Query("DELETE FROM medicamentos WHERE id = :medId")
  suspend fun deleteMedicamentoById(medId: String)

  @Query("UPDATE medicamentos SET stock = :newStock WHERE id = :medId")
  suspend fun updateMedicamentoStock(medId: String, newStock: Int)

  // Proveedores
  @Query("SELECT * FROM proveedores ORDER BY id ASC")
  fun observeProveedores(): Flow<List<ProveedorEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProveedor(prov: ProveedorEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProveedores(provs: List<ProveedorEntity>)

  @Query("DELETE FROM proveedores WHERE id = :provId")
  suspend fun deleteProveedorById(provId: String)

  // Clientes
  @Query("SELECT * FROM clientes ORDER BY id ASC")
  fun observeClientes(): Flow<List<ClienteEntity>>

  @Query("SELECT * FROM clientes WHERE id = :clienteId LIMIT 1")
  suspend fun findClienteById(clienteId: String): ClienteEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCliente(cliente: ClienteEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertClientes(clientes: List<ClienteEntity>)

  @Query("DELETE FROM clientes WHERE id = :clienteId")
  suspend fun deleteClienteById(clienteId: String)

  @Query("UPDATE clientes SET puntos = puntos + :delta WHERE id = :clienteId")
  suspend fun addClientePuntos(clienteId: String, delta: Int)

  // Compras
  @Query("SELECT * FROM compras ORDER BY fecha DESC")
  fun observeCompras(): Flow<List<CompraEntity>>

  @Query("SELECT * FROM compras WHERE id = :compraId LIMIT 1")
  suspend fun findCompraById(compraId: String): CompraEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCompra(compra: CompraEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCompras(compras: List<CompraEntity>)

  @Query("UPDATE compras SET estado = :estado WHERE id = :compraId")
  suspend fun updateCompraEstado(compraId: String, estado: String)

  // Ventas
  @Transaction
  @Query("SELECT * FROM ventas ORDER BY fecha DESC")
  fun observeVentas(): Flow<List<VentaWithItems>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVenta(venta: VentaEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVentaItems(items: List<VentaItemEntity>)

  @Transaction
  suspend fun seedInitialDataIfEmpty(bundle: PharmacySeedData.InitialSeedBundle) {
    if (countMedicamentos() > 0) return
    upsertMedicamentos(bundle.medicamentos)
    upsertProveedores(bundle.proveedores)
    upsertClientes(bundle.clientes)
    upsertCompras(bundle.compras)
    bundle.ventas.forEach { v ->
      insertVenta(v.venta)
      insertVentaItems(v.items)
    }
  }

  @Transaction
  suspend fun registrarCompraAtomic(compra: CompraEntity) {
    val med = findMedicamentoById(compra.medId)
      ?: throw IllegalStateException("Medicamento no encontrado: ${compra.medId}")
    if (compra.estado == "Completado") {
      updateMedicamentoStock(med.id, med.stock + compra.cantidad)
    }
    upsertCompra(compra)
  }

  @Transaction
  suspend fun recibirCompraAtomic(compraId: String): Boolean {
    val compra = findCompraById(compraId) ?: return false
    if (compra.estado != "Pendiente") return false
    val med = findMedicamentoById(compra.medId)
      ?: throw IllegalStateException("Medicamento asociado no encontrado: ${compra.medId}")
    updateMedicamentoStock(med.id, med.stock + compra.cantidad)
    updateCompraEstado(compraId, "Completado")
    return true
  }

  @Transaction
  suspend fun registrarVentaAtomic(
    venta: VentaEntity,
    ventaItems: List<VentaItemEntity>,
    cartItems: List<CartLineItem>,
  ) {
    val updatedStocks = cartItems.map { item ->
      val med = findMedicamentoById(item.medId)
        ?: throw IllegalStateException("Producto no encontrado: ${item.nombre}")
      if (med.stock < item.cantidad) {
        throw IllegalStateException("Stock insuficiente para ${item.nombre} (disp: ${med.stock})")
      }
      med.id to (med.stock - item.cantidad)
    }

    updatedStocks.forEach { (medId, newStock) ->
      updateMedicamentoStock(medId, newStock)
    }

    if (!venta.clienteId.isNullOrBlank() && venta.puntos > 0) {
      if (findClienteById(venta.clienteId) != null) {
        addClientePuntos(venta.clienteId, venta.puntos)
      }
    }

    insertVenta(venta)
    insertVentaItems(ventaItems)
  }
}
