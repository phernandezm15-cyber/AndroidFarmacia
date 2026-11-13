package com.example.data.repository

import com.example.data.local.ClienteEntity
import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.data.local.VentaWithItems
import kotlinx.coroutines.flow.Flow

data class CartLineItem(
  val medId: String,
  val nombre: String,
  val precio: Double,
  val cantidad: Int,
)

interface FarmaciaRepository {
  val isCloudBackend: Boolean

  fun observeUserProfile(userId: String): Flow<UserEntity?>
  fun observeMedicamentos(): Flow<List<MedicamentoEntity>>
  fun observeProveedores(): Flow<List<ProveedorEntity>>
  fun observeClientes(): Flow<List<ClienteEntity>>
  fun observeCompras(): Flow<List<CompraEntity>>
  fun observeVentas(): Flow<List<VentaWithItems>>
  fun observeStaff(): Flow<List<UserEntity>>

  suspend fun findUserById(userId: String): UserEntity?
  suspend fun ensureWorkspaceSeeded(userId: String, ownerName: String): Result<Unit>
  suspend fun refreshAll(): Result<Unit>

  suspend fun registerUser(
    nombre: String,
    email: String,
    password: String,
    requestedRole: String,
    adminCode: String?,
  ): Result<UserEntity>

  suspend fun loginUser(email: String, password: String): Result<UserEntity>
  suspend fun resetPasswordDirect(email: String, newPassword: String): Result<Unit>
  suspend fun changePasswordLoggedIn(
    userId: String,
    currentPassword: String,
    newPassword: String,
  ): Result<Unit>

  suspend fun saveMedicamento(med: MedicamentoEntity, currentCount: Int): Result<MedicamentoEntity>
  suspend fun deleteMedicamento(medId: String): Result<Unit>

  suspend fun saveProveedor(prov: ProveedorEntity, currentCount: Int): Result<ProveedorEntity>
  suspend fun deleteProveedor(provId: String): Result<Unit>

  suspend fun saveCliente(cliente: ClienteEntity, currentCount: Int): Result<ClienteEntity>
  suspend fun deleteCliente(clienteId: String): Result<Unit>

  suspend fun addStaffMember(nombre: String, email: String, role: String): Result<UserEntity>

  suspend fun registrarCompra(
    proveedorId: String,
    medId: String,
    cantidad: Int,
    costo: Double,
    estado: String,
    currentCount: Int,
    nota: String? = null,
  ): Result<CompraEntity>

  suspend fun recibirCompra(compraId: String): Result<Boolean>

  suspend fun registrarVenta(
    items: List<CartLineItem>,
    descPct: Double,
    vendedor: UserEntity,
    clienteId: String?,
    currentSalesCount: Int,
  ): Result<VentaWithItems>

  companion object {
    const val DEFAULT_STAFF_PASSWORD = "Farmacia2026!"
  }
}
