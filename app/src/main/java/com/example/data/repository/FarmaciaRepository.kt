package com.example.data.repository

import android.content.Context
import com.example.data.local.ClienteEntity
import com.example.data.local.CompraEntity
import com.example.data.local.FarmaciaDao
import com.example.data.local.FarmaciaDatabase
import com.example.data.local.MedicamentoEntity
import com.example.data.local.PasswordHasher
import com.example.data.local.PharmacySeedData
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.data.local.VentaEntity
import com.example.data.local.VentaItemEntity
import com.example.data.local.VentaWithItems
import com.example.domain.PharmacyUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class CartLineItem(
  val medId: String,
  val nombre: String,
  val precio: Double,
  val cantidad: Int,
)

class FarmaciaRepository(
  private val dao: FarmaciaDao,
) {
  constructor(context: Context) : this(FarmaciaDatabase.getInstance(context).dao())

  fun observeUserProfile(userId: String): Flow<UserEntity?> = dao.observeUserById(userId)

  fun observeMedicamentos(): Flow<List<MedicamentoEntity>> = dao.observeMedicamentos()

  fun observeProveedores(): Flow<List<ProveedorEntity>> = dao.observeProveedores()

  fun observeClientes(): Flow<List<ClienteEntity>> = dao.observeClientes()

  fun observeCompras(): Flow<List<CompraEntity>> = dao.observeCompras()

  fun observeVentas(): Flow<List<VentaWithItems>> = dao.observeVentas()

  fun observeStaff(): Flow<List<UserEntity>> = dao.observeAllUsers()

  suspend fun findUserById(userId: String): UserEntity? = dao.findUserById(userId)

  suspend fun ensureWorkspaceSeeded(userId: String, ownerName: String): Result<Unit> {
    return try {
      val seed = PharmacySeedData.buildInitialSeed(userId, ownerName.ifBlank { "Usuario" })
      dao.seedInitialDataIfEmpty(seed)
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun registerUser(
    nombre: String,
    email: String,
    password: String,
    requestedRole: String,
    adminCode: String?,
  ): Result<UserEntity> {
    return try {
      val cleanName = nombre.trim().take(100)
      val cleanEmail = email.trim().lowercase().take(150)
      if (cleanName.length < 3) {
        return Result.failure(IllegalArgumentException("Ingresa tu nombre completo (mínimo 3 caracteres)."))
      }
      if (!PharmacyUtils.isValidEmail(cleanEmail)) {
        return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido."))
      }
      val analysis = PharmacyUtils.evaluatePasswordStrength(password)
      if (!analysis.isAllowed) {
        return Result.failure(IllegalArgumentException(analysis.feedback))
      }
      val role = if (requestedRole == "admin") {
        if (adminCode != PharmacyUtils.ADMIN_MASTER_PASSWORD) {
          return Result.failure(IllegalArgumentException("Clave de autorización incorrecta para crear cuenta Administrador."))
        }
        "admin"
      } else {
        "empleado"
      }

      if (dao.findUserByEmail(cleanEmail) != null) {
        return Result.failure(IllegalArgumentException("Ya existe una cuenta registrada con ese correo electrónico."))
      }

      val uid = "USR-${UUID.randomUUID().toString().take(8)}"
      val user = UserEntity(
        uid = uid,
        email = cleanEmail,
        nombre = cleanName,
        role = role,
        passwordHash = PasswordHasher.hash(password),
        createdAt = System.currentTimeMillis(),
      )
      dao.insertUser(user)
      val seed = PharmacySeedData.buildInitialSeed(user.uid, user.nombre)
      dao.seedInitialDataIfEmpty(seed)
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun loginUser(email: String, password: String): Result<UserEntity> {
    return try {
      val cleanEmail = email.trim().lowercase()
      val user = dao.findUserByEmail(cleanEmail)
        ?: return Result.failure(IllegalArgumentException("No existe una cuenta registrada con ese correo electrónico."))

      if (user.passwordHash.isBlank() || !PasswordHasher.verify(password, user.passwordHash)) {
        return Result.failure(IllegalArgumentException("Correo electrónico o contraseña incorrectos."))
      }

      val seed = PharmacySeedData.buildInitialSeed(user.uid, user.nombre)
      dao.seedInitialDataIfEmpty(seed)
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun resetPasswordDirect(email: String, newPassword: String): Result<Unit> {
    return try {
      val cleanEmail = email.trim().lowercase()
      if (!PharmacyUtils.isValidEmail(cleanEmail)) {
        return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido."))
      }
      val analysis = PharmacyUtils.evaluatePasswordStrength(newPassword)
      if (!analysis.isAllowed) {
        return Result.failure(IllegalArgumentException(analysis.feedback))
      }
      val user = dao.findUserByEmail(cleanEmail)
        ?: return Result.failure(IllegalArgumentException("No existe una cuenta registrada con ese correo electrónico."))

      val updatedRows = dao.updateUserPasswordHash(user.uid, PasswordHasher.hash(newPassword))
      if (updatedRows <= 0) {
        return Result.failure(IllegalStateException("No se pudo actualizar la contraseña en la base de datos."))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun changePasswordLoggedIn(
    userId: String,
    currentPassword: String,
    newPassword: String,
  ): Result<Unit> {
    return try {
      val user = dao.findUserById(userId)
        ?: return Result.failure(IllegalArgumentException("Usuario no encontrado."))
      if (user.passwordHash.isBlank() || !PasswordHasher.verify(currentPassword, user.passwordHash)) {
        return Result.failure(IllegalArgumentException("La contraseña actual es incorrecta."))
      }
      val analysis = PharmacyUtils.evaluatePasswordStrength(newPassword)
      if (!analysis.isAllowed) {
        return Result.failure(IllegalArgumentException(analysis.feedback))
      }
      dao.updateUserPasswordHash(userId, PasswordHasher.hash(newPassword))
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveMedicamento(med: MedicamentoEntity, currentCount: Int): Result<MedicamentoEntity> {
    return try {
      val isNew = med.id.isBlank()
      val finalId = if (isNew) {
        "MED-${(currentCount + 1).toString().padStart(3, '0')}-${UUID.randomUUID().toString().take(6)}"
      } else {
        med.id
      }
      val finalCode = if (med.codigo.isBlank()) {
        "750${System.currentTimeMillis().toString().takeLast(8)}"
      } else {
        med.codigo.trim()
      }
      val entity = med.copy(id = finalId, codigo = finalCode)
      dao.upsertMedicamento(entity)
      Result.success(entity)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteMedicamento(medId: String): Result<Unit> {
    return try {
      dao.deleteMedicamentoById(medId)
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveProveedor(prov: ProveedorEntity, currentCount: Int): Result<ProveedorEntity> {
    return try {
      val isNew = prov.id.isBlank()
      val finalId = if (isNew) {
        "PRV-${(currentCount + 1).toString().padStart(3, '0')}-${UUID.randomUUID().toString().take(6)}"
      } else {
        prov.id
      }
      val entity = prov.copy(id = finalId)
      dao.upsertProveedor(entity)
      Result.success(entity)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteProveedor(provId: String): Result<Unit> {
    return try {
      dao.deleteProveedorById(provId)
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveCliente(cliente: ClienteEntity, currentCount: Int): Result<ClienteEntity> {
    return try {
      val isNew = cliente.id.isBlank()
      val finalId = if (isNew) {
        "CLI-${(currentCount + 1).toString().padStart(3, '0')}-${UUID.randomUUID().toString().take(6)}"
      } else {
        cliente.id
      }
      val entity = cliente.copy(id = finalId)
      dao.upsertCliente(entity)
      Result.success(entity)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteCliente(clienteId: String): Result<Unit> {
    return try {
      dao.deleteClienteById(clienteId)
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun addStaffMember(nombre: String, email: String, role: String): Result<UserEntity> {
    return try {
      val cleanName = nombre.trim().take(100)
      val cleanEmail = email.trim().lowercase().take(150)
      val cleanRole = if (role == "admin") "admin" else "empleado"
      if (dao.findUserByEmail(cleanEmail) != null) {
        return Result.failure(IllegalArgumentException("Ya existe un usuario con ese correo electrónico."))
      }
      val staffUser = UserEntity(
        uid = "STF-${UUID.randomUUID().toString().take(8)}",
        email = cleanEmail,
        nombre = cleanName,
        role = cleanRole,
        passwordHash = PasswordHasher.hash(DEFAULT_STAFF_PASSWORD),
        createdAt = System.currentTimeMillis(),
      )
      dao.insertUser(staffUser)
      Result.success(staffUser)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun registrarCompra(
    proveedorId: String,
    medId: String,
    cantidad: Int,
    costo: Double,
    estado: String,
    currentCount: Int,
    nota: String? = null,
  ): Result<CompraEntity> {
    return try {
      val seq = (currentCount + 1).toString().padStart(3, '0')
      val suffix = UUID.randomUUID().toString().take(4).uppercase()
      val code = "CMP-$seq-$suffix"
      val entity = CompraEntity(
        id = code,
        codigo = code,
        fecha = System.currentTimeMillis(),
        proveedorId = proveedorId,
        medId = medId,
        cantidad = cantidad,
        costo = costo,
        estado = estado,
        nota = nota,
      )
      dao.registrarCompraAtomic(entity)
      Result.success(entity)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun recibirCompra(compraId: String): Result<Boolean> {
    return try {
      Result.success(dao.recibirCompraAtomic(compraId))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun registrarVenta(
    items: List<CartLineItem>,
    descPct: Double,
    vendedor: UserEntity,
    clienteId: String?,
    currentSalesCount: Int,
  ): Result<VentaWithItems> {
    return try {
      val totals = PharmacyUtils.calcTotals(items, descPct)
      val puntos = if (!clienteId.isNullOrBlank()) PharmacyUtils.puntosPor(totals.total) else 0
      val uuid = UUID.randomUUID().toString()
      val codigo = "VTA-${1000 + currentSalesCount + 1}"

      val venta = VentaEntity(
        uuid = uuid,
        codigo = codigo,
        fecha = System.currentTimeMillis(),
        subtotal = totals.subtotal,
        descuento = totals.descuento,
        descPct = descPct,
        iva = totals.iva,
        total = totals.total,
        vendedorNombre = vendedor.nombre,
        vendedorId = vendedor.uid,
        clienteId = clienteId,
        puntos = puntos,
      )
      val ventaItems = items.map { item ->
        VentaItemEntity(
          itemId = 0,
          ventaId = uuid,
          medId = item.medId,
          nombre = item.nombre,
          cantidad = item.cantidad,
          precio = item.precio,
        )
      }
      dao.registrarVentaAtomic(venta, ventaItems, items)
      Result.success(VentaWithItems(venta, ventaItems))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  companion object {
    const val DEFAULT_STAFF_PASSWORD = "Farmacia2026!"
  }
}
