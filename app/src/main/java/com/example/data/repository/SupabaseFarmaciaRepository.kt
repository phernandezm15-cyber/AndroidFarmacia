package com.example.data.repository

import com.example.data.local.ClienteEntity
import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.PasswordHasher
import com.example.data.local.PharmacySeedData
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.data.local.VentaEntity
import com.example.data.local.VentaItemEntity
import com.example.data.local.VentaWithItems
import com.example.data.remote.PasswordHashPatch
import com.example.data.remote.PostgrestErrorBody
import com.example.data.remote.RecibirCompraRpcRequest
import com.example.data.remote.RegistrarCompraRpcRequest
import com.example.data.remote.RegistrarVentaRpcRequest
import com.example.data.remote.SaleItemRpcPayload
import com.example.data.remote.SupabaseApi
import com.example.data.remote.SupabaseClienteRow
import com.example.data.remote.SupabaseCompraRow
import com.example.data.remote.SupabaseMedicamentoRow
import com.example.data.remote.SupabaseProveedorRow
import com.example.data.remote.SupabaseUserRow
import com.example.data.remote.SupabaseVentaItemInsert
import com.example.data.remote.SupabaseVentaRow
import com.example.domain.PharmacyUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

class SupabaseFarmaciaRepository(
  private val api: SupabaseApi,
) : FarmaciaRepository {

  override val isCloudBackend: Boolean = true

  private val refreshMutex = Mutex()
  private val usersFlow = MutableStateFlow<List<UserEntity>>(emptyList())
  private val medsFlow = MutableStateFlow<List<MedicamentoEntity>>(emptyList())
  private val provsFlow = MutableStateFlow<List<ProveedorEntity>>(emptyList())
  private val clientesFlow = MutableStateFlow<List<ClienteEntity>>(emptyList())
  private val comprasFlow = MutableStateFlow<List<CompraEntity>>(emptyList())
  private val ventasFlow = MutableStateFlow<List<VentaWithItems>>(emptyList())

  private val errorAdapter by lazy {
    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter(PostgrestErrorBody::class.java)
  }

  override fun observeUserProfile(userId: String): Flow<UserEntity?> =
    usersFlow.map { list -> list.find { it.uid == userId } }

  override fun observeMedicamentos(): Flow<List<MedicamentoEntity>> = medsFlow

  override fun observeProveedores(): Flow<List<ProveedorEntity>> = provsFlow

  override fun observeClientes(): Flow<List<ClienteEntity>> = clientesFlow

  override fun observeCompras(): Flow<List<CompraEntity>> = comprasFlow

  override fun observeVentas(): Flow<List<VentaWithItems>> = ventasFlow

  override fun observeStaff(): Flow<List<UserEntity>> = usersFlow

  override suspend fun findUserById(userId: String): UserEntity? {
    val cached = usersFlow.value.find { it.uid == userId }
    if (cached != null) return cached
    return runCatching {
      api.findUserById("eq.$userId").firstOrNull()?.toEntity()
    }.getOrNull()
  }

  override suspend fun refreshAll(): Result<Unit> = refreshMutex.withLock {
    try {
      coroutineScope {
        val usersDef = async { api.getUsers().map { it.toEntity() } }
        val medsDef = async { api.getMedicamentos().map { it.toEntity() } }
        val provsDef = async { api.getProveedores().map { it.toEntity() } }
        val cliDef = async { api.getClientes().map { it.toEntity() } }
        val cmpDef = async { api.getCompras().map { it.toEntity() } }
        val vtaDef = async { api.getVentas().map { it.toVentaWithItems() } }

        usersFlow.value = usersDef.await()
        medsFlow.value = medsDef.await()
        provsFlow.value = provsDef.await()
        clientesFlow.value = cliDef.await()
        comprasFlow.value = cmpDef.await()
        ventasFlow.value = vtaDef.await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun ensureWorkspaceSeeded(userId: String, ownerName: String): Result<Unit> {
    return try {
      seedIfEmptyRemote(userId, ownerName.ifBlank { "Usuario" })
      refreshAll()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  private suspend fun seedIfEmptyRemote(ownerUid: String, ownerName: String) {
    val existingMeds = api.getMedicamentos(limit = 1)
    if (existingMeds.isNotEmpty()) return

    val bundle = PharmacySeedData.buildInitialSeed(ownerUid, ownerName)
    api.upsertProveedores(bundle.proveedores.map { SupabaseProveedorRow.fromEntity(it) })
    api.upsertClientes(bundle.clientes.map { SupabaseClienteRow.fromEntity(it) })
    api.upsertMedicamentos(bundle.medicamentos.map { SupabaseMedicamentoRow.fromEntity(it) })
    api.upsertCompras(bundle.compras.map { SupabaseCompraRow.fromEntity(it) })

    val existingVentas = api.getVentas()
    if (existingVentas.isEmpty()) {
      api.upsertVentas(bundle.ventas.map { SupabaseVentaRow.fromVenta(it.venta) })
      val itemRows = bundle.ventas.flatMap { v ->
        v.items.map { item ->
          SupabaseVentaItemInsert(
            ventaId = v.venta.uuid,
            medId = item.medId,
            nombre = item.nombre,
            cantidad = item.cantidad,
            precio = item.precio,
          )
        }
      }
      if (itemRows.isNotEmpty()) {
        api.insertVentaItems(itemRows)
      }
    }
  }

  override suspend fun registerUser(
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

      if (api.findUserByEmail("eq.$cleanEmail").isNotEmpty()) {
        return Result.failure(IllegalArgumentException("Ya existe una cuenta registrada con ese correo electrónico."))
      }

      val newUser = UserEntity(
        uid = "USR-${UUID.randomUUID().toString().take(8)}",
        email = cleanEmail,
        nombre = cleanName,
        role = role,
        passwordHash = PasswordHasher.hash(password),
        createdAt = System.currentTimeMillis(),
      )
      val inserted = api.insertUser(SupabaseUserRow.fromEntity(newUser)).firstOrNull()?.toEntity() ?: newUser
      seedIfEmptyRemote(inserted.uid, inserted.nombre)
      refreshAll()
      Result.success(inserted)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun loginUser(email: String, password: String): Result<UserEntity> {
    return try {
      val cleanEmail = email.trim().lowercase()
      val user = api.findUserByEmail("eq.$cleanEmail").firstOrNull()?.toEntity()
        ?: return Result.failure(IllegalArgumentException("No existe una cuenta registrada con ese correo electrónico."))

      if (user.passwordHash.isBlank() || !PasswordHasher.verify(password, user.passwordHash)) {
        return Result.failure(IllegalArgumentException("Correo electrónico o contraseña incorrectos."))
      }

      seedIfEmptyRemote(user.uid, user.nombre)
      refreshAll()
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun resetPasswordDirect(email: String, newPassword: String): Result<Unit> {
    return try {
      val cleanEmail = email.trim().lowercase()
      if (!PharmacyUtils.isValidEmail(cleanEmail)) {
        return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido."))
      }
      val analysis = PharmacyUtils.evaluatePasswordStrength(newPassword)
      if (!analysis.isAllowed) {
        return Result.failure(IllegalArgumentException(analysis.feedback))
      }
      val user = api.findUserByEmail("eq.$cleanEmail").firstOrNull()?.toEntity()
        ?: return Result.failure(IllegalArgumentException("No existe una cuenta registrada con ese correo electrónico."))

      val updated = api.updateUserPasswordHash(
        uidFilter = "eq.${user.uid}",
        patch = PasswordHashPatch(PasswordHasher.hash(newPassword)),
      )
      if (updated.isEmpty()) {
        return Result.failure(IllegalStateException("No se pudo actualizar la contraseña en Supabase."))
      }
      refreshAll()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun changePasswordLoggedIn(
    userId: String,
    currentPassword: String,
    newPassword: String,
  ): Result<Unit> {
    return try {
      val user = api.findUserById("eq.$userId").firstOrNull()?.toEntity()
        ?: return Result.failure(IllegalArgumentException("Usuario no encontrado."))
      if (user.passwordHash.isBlank() || !PasswordHasher.verify(currentPassword, user.passwordHash)) {
        return Result.failure(IllegalArgumentException("La contraseña actual es incorrecta."))
      }
      val analysis = PharmacyUtils.evaluatePasswordStrength(newPassword)
      if (!analysis.isAllowed) {
        return Result.failure(IllegalArgumentException(analysis.feedback))
      }
      api.updateUserPasswordHash(
        uidFilter = "eq.$userId",
        patch = PasswordHashPatch(PasswordHasher.hash(newPassword)),
      )
      refreshAll()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun saveMedicamento(med: MedicamentoEntity, currentCount: Int): Result<MedicamentoEntity> {
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
      val saved = api.upsertMedicamentos(listOf(SupabaseMedicamentoRow.fromEntity(entity)))
        .firstOrNull()?.toEntity() ?: entity
      refreshAll()
      Result.success(saved)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun deleteMedicamento(medId: String): Result<Unit> {
    return try {
      api.deleteMedicamento("eq.$medId")
      refreshAll()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun saveProveedor(prov: ProveedorEntity, currentCount: Int): Result<ProveedorEntity> {
    return try {
      val isNew = prov.id.isBlank()
      val finalId = if (isNew) {
        "PRV-${(currentCount + 1).toString().padStart(3, '0')}-${UUID.randomUUID().toString().take(6)}"
      } else {
        prov.id
      }
      val entity = prov.copy(id = finalId)
      val saved = api.upsertProveedores(listOf(SupabaseProveedorRow.fromEntity(entity)))
        .firstOrNull()?.toEntity() ?: entity
      refreshAll()
      Result.success(saved)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun deleteProveedor(provId: String): Result<Unit> {
    return try {
      api.deleteProveedor("eq.$provId")
      refreshAll()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun saveCliente(cliente: ClienteEntity, currentCount: Int): Result<ClienteEntity> {
    return try {
      val isNew = cliente.id.isBlank()
      val finalId = if (isNew) {
        "CLI-${(currentCount + 1).toString().padStart(3, '0')}-${UUID.randomUUID().toString().take(6)}"
      } else {
        cliente.id
      }
      val entity = cliente.copy(id = finalId)
      val saved = api.upsertClientes(listOf(SupabaseClienteRow.fromEntity(entity)))
        .firstOrNull()?.toEntity() ?: entity
      refreshAll()
      Result.success(saved)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun deleteCliente(clienteId: String): Result<Unit> {
    return try {
      api.deleteCliente("eq.$clienteId")
      refreshAll()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun addStaffMember(nombre: String, email: String, role: String): Result<UserEntity> {
    return try {
      val cleanName = nombre.trim().take(100)
      val cleanEmail = email.trim().lowercase().take(150)
      val cleanRole = if (role == "admin") "admin" else "empleado"
      if (api.findUserByEmail("eq.$cleanEmail").isNotEmpty()) {
        return Result.failure(IllegalArgumentException("Ya existe un usuario con ese correo electrónico."))
      }
      val staffUser = UserEntity(
        uid = "STF-${UUID.randomUUID().toString().take(8)}",
        email = cleanEmail,
        nombre = cleanName,
        role = cleanRole,
        passwordHash = PasswordHasher.hash(FarmaciaRepository.DEFAULT_STAFF_PASSWORD),
        createdAt = System.currentTimeMillis(),
      )
      val inserted = api.insertUser(SupabaseUserRow.fromEntity(staffUser)).firstOrNull()?.toEntity() ?: staffUser
      refreshAll()
      Result.success(inserted)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun registrarCompra(
    proveedorId: String,
    medId: String,
    cantidad: Int,
    costo: Double,
    estado: String,
    currentCount: Int,
    nota: String?,
  ): Result<CompraEntity> {
    return try {
      val seq = (currentCount + 1).toString().padStart(3, '0')
      val suffix = UUID.randomUUID().toString().take(4).uppercase()
      val code = "CMP-$seq-$suffix"
      val now = System.currentTimeMillis()
      val entity = CompraEntity(
        id = code,
        codigo = code,
        fecha = now,
        proveedorId = proveedorId,
        medId = medId,
        cantidad = cantidad,
        costo = costo,
        estado = estado,
        nota = nota,
      )
      api.registrarCompraRpc(
        RegistrarCompraRpcRequest(
          id = entity.id,
          codigo = entity.codigo,
          fecha = entity.fecha,
          proveedorId = entity.proveedorId,
          medId = entity.medId,
          cantidad = entity.cantidad,
          costo = entity.costo,
          estado = entity.estado,
          nota = entity.nota,
        )
      )
      refreshAll()
      Result.success(entity)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun recibirCompra(compraId: String): Result<Boolean> {
    return try {
      val ok = api.recibirCompraRpc(RecibirCompraRpcRequest(compraId))
      refreshAll()
      Result.success(ok)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  override suspend fun registrarVenta(
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
      val now = System.currentTimeMillis()

      val venta = VentaEntity(
        uuid = uuid,
        codigo = codigo,
        fecha = now,
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

      api.registrarVentaRpc(
        RegistrarVentaRpcRequest(
          uuid = uuid,
          codigo = codigo,
          fecha = now,
          subtotal = totals.subtotal,
          descuento = totals.descuento,
          descPct = descPct,
          iva = totals.iva,
          total = totals.total,
          vendedorNombre = vendedor.nombre,
          vendedorId = vendedor.uid,
          clienteId = clienteId,
          puntos = puntos,
          items = items.map { SaleItemRpcPayload(it.medId, it.nombre, it.cantidad, it.precio) },
        )
      )
      refreshAll()
      Result.success(VentaWithItems(venta, ventaItems))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(IllegalStateException(formatSupabaseError(e)))
    }
  }

  private fun formatSupabaseError(exc: Exception): String {
    return when (exc) {
      is IOException ->
        "Sin conexión con Supabase. Verifica tu internet e intenta de nuevo."
      is HttpException -> {
        val code = exc.code()
        val rawBody = runCatching { exc.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        val parsed = runCatching {
          if (rawBody.isNotBlank()) errorAdapter.fromJson(rawBody) else null
        }.getOrNull()
        val serverMsg = parsed?.message.orEmpty()
        when {
          code == 401 || code == 403 ->
            "Acceso denegado por Supabase. Verifica SUPABASE_URL, SUPABASE_ANON_KEY y los permisos del SQL."
          parsed?.code == "42P01" ||
            serverMsg.contains("Could not find the table", ignoreCase = true) ||
            serverMsg.contains("does not exist", ignoreCase = true) ->
            "Faltan tablas o funciones en Supabase. Ejecuta supabase_schema.sql en el SQL Editor de tu proyecto."
          parsed?.code == "23505" ->
            "Ya existe un registro duplicado con ese correo o código."
          serverMsg.isNotBlank() -> serverMsg
          else -> "Error del servidor Supabase ($code)."
        }
      }
      else -> exc.localizedMessage ?: "No se pudo completar la operación en Supabase."
    }
  }
}
