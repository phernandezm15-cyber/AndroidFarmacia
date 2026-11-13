package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.ClienteEntity
import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.data.local.VentaEntity
import com.example.data.local.VentaItemEntity
import com.example.data.local.VentaWithItems
import com.example.data.remote.ClienteDto
import com.example.data.remote.CompraDto
import com.example.data.remote.FirestoreSeedData
import com.example.data.remote.MedicamentoDto
import com.example.data.remote.OperationType
import com.example.data.remote.ProveedorDto
import com.example.data.remote.StaffMemberDto
import com.example.data.remote.UserProfileDto
import com.example.data.remote.VentaDto
import com.example.data.remote.handleFirestoreError
import com.example.domain.PharmacyUtils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class CartLineItem(
  val medId: String,
  val nombre: String,
  val precio: Double,
  val cantidad: Int,
)

class FarmaciaRepository(
  private val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun userDoc(userId: String) = db.collection("users").document(userId)
  private fun medsCol(userId: String) = userDoc(userId).collection("medicamentos")
  private fun provsCol(userId: String) = userDoc(userId).collection("proveedores")
  private fun clientesCol(userId: String) = userDoc(userId).collection("clientes")
  private fun comprasCol(userId: String) = userDoc(userId).collection("compras")
  private fun ventasCol(userId: String) = userDoc(userId).collection("ventas")
  private fun staffCol(userId: String) = userDoc(userId).collection("staff")

  fun observeUserProfile(userId: String): Flow<UserEntity?> =
    userDoc(userId).snapshots().map { snapshot ->
      if (!snapshot.exists()) null
      else snapshot.toObject(UserProfileDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toEntity()
    }

  fun observeMedicamentos(userId: String): Flow<List<MedicamentoEntity>> =
    medsCol(userId).snapshots().map { snapshot ->
      snapshot.documents
        .mapNotNull { it.toObject(MedicamentoDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toEntity() }
        .sortedBy { it.id }
    }

  fun observeProveedores(userId: String): Flow<List<ProveedorEntity>> =
    provsCol(userId).snapshots().map { snapshot ->
      snapshot.documents
        .mapNotNull { it.toObject(ProveedorDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toEntity() }
        .sortedBy { it.id }
    }

  fun observeClientes(userId: String): Flow<List<ClienteEntity>> =
    clientesCol(userId).snapshots().map { snapshot ->
      snapshot.documents
        .mapNotNull { it.toObject(ClienteDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toEntity() }
        .sortedBy { it.id }
    }

  fun observeCompras(userId: String): Flow<List<CompraEntity>> =
    comprasCol(userId).snapshots().map { snapshot ->
      snapshot.documents
        .mapNotNull { it.toObject(CompraDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toEntity() }
        .sortedByDescending { it.fecha }
    }

  fun observeVentas(userId: String): Flow<List<VentaWithItems>> =
    ventasCol(userId).snapshots().map { snapshot ->
      snapshot.documents
        .mapNotNull { it.toObject(VentaDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toEntity() }
        .sortedByDescending { it.venta.fecha }
    }

  fun observeStaff(userId: String): Flow<List<UserEntity>> =
    staffCol(userId).snapshots().map { snapshot ->
      snapshot.documents
        .mapNotNull { it.toObject(StaffMemberDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toEntity() }
        .sortedBy { it.nombre }
    }

  suspend fun ensureUserWorkspaceInitialized(
    userId: String,
    email: String,
    displayName: String,
    isEmailVerified: Boolean,
  ): Result<UserEntity> {
    val profileRef = userDoc(userId)
    return try {
      val existingSnap = profileRef.get().await()
      val cleanEmail = email.ifBlank { "usuario@farmaciadb.app" }.take(150)
      val cleanName = displayName.ifBlank { cleanEmail.substringBefore("@") }.ifBlank { "Usuario" }.take(100)
      val isBootstrapAdmin = cleanEmail.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) && isEmailVerified

      val profile: UserEntity = if (!existingSnap.exists()) {
        val role = if (isBootstrapAdmin) "admin" else "empleado"
        val profileData = mapOf(
          "userId" to userId,
          "nombre" to cleanName,
          "email" to cleanEmail,
          "role" to role,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        profileRef.set(profileData).await()
        UserEntity(
          uid = userId,
          email = cleanEmail,
          nombre = cleanName,
          role = role,
        )
      } else {
        existingSnap.toObject(UserProfileDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toEntity()
          ?: UserEntity(uid = userId, email = cleanEmail, nombre = cleanName, role = if (isBootstrapAdmin) "admin" else "empleado")
      }

      // Seed initial pharmacy workspace if medicamentos collection is empty
      val existingMeds = medsCol(userId).limit(1).get().await()
      if (existingMeds.isEmpty) {
        val seed = FirestoreSeedData.buildInitialSeed(userId, profile.nombre)

        // Batch 1: Core master catalogs (medicamentos must exist before compras/ventas)
        val batch1 = db.batch()
        seed.medicamentos.forEach { med ->
          batch1.set(medsCol(userId).document(med.id), MedicamentoDto.toCreateMap(med))
        }
        seed.proveedores.forEach { prov ->
          batch1.set(provsCol(userId).document(prov.id), ProveedorDto.toCreateMap(prov))
        }
        seed.clientes.forEach { cli ->
          batch1.set(clientesCol(userId).document(cli.id), ClienteDto.toCreateMap(cli))
        }
        batch1.commit().await()

        // Batch 2: Initial purchase orders, sales history, and owner staff card
        val batch2 = db.batch()
        seed.compras.forEach { cmp ->
          batch2.set(comprasCol(userId).document(cmp.id), CompraDto.toCreateMap(cmp))
        }
        seed.ventas.forEach { v ->
          batch2.set(ventasCol(userId).document(v.venta.uuid), VentaDto.toCreateMap(v.venta, v.items))
        }
        batch2.set(
          staffCol(userId).document("STF-OWNER"),
          mapOf(
            "nombre" to profile.nombre,
            "email" to profile.email,
            "role" to profile.role,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        batch2.commit().await()
      }

      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, profileRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveMedicamento(userId: String, med: MedicamentoEntity, currentCount: Int): Result<MedicamentoEntity> {
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
    val docRef = medsCol(userId).document(finalId)
    return try {
      if (isNew) {
        docRef.set(MedicamentoDto.toCreateMap(entity)).await()
      } else {
        docRef.update(MedicamentoDto.toUpdateMap(entity)).await()
      }
      Result.success(entity)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteMedicamento(userId: String, medId: String): Result<Unit> {
    val docRef = medsCol(userId).document(medId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveProveedor(userId: String, prov: ProveedorEntity, currentCount: Int): Result<ProveedorEntity> {
    val isNew = prov.id.isBlank()
    val finalId = if (isNew) {
      "PRV-${(currentCount + 1).toString().padStart(3, '0')}-${UUID.randomUUID().toString().take(6)}"
    } else {
      prov.id
    }
    val entity = prov.copy(id = finalId)
    val docRef = provsCol(userId).document(finalId)
    return try {
      if (isNew) {
        docRef.set(ProveedorDto.toCreateMap(entity)).await()
      } else {
        docRef.update(ProveedorDto.toUpdateMap(entity)).await()
      }
      Result.success(entity)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteProveedor(userId: String, provId: String): Result<Unit> {
    val docRef = provsCol(userId).document(provId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveCliente(userId: String, cliente: ClienteEntity, currentCount: Int): Result<ClienteEntity> {
    val isNew = cliente.id.isBlank()
    val finalId = if (isNew) {
      "CLI-${(currentCount + 1).toString().padStart(3, '0')}-${UUID.randomUUID().toString().take(6)}"
    } else {
      cliente.id
    }
    val entity = cliente.copy(id = finalId)
    val docRef = clientesCol(userId).document(finalId)
    return try {
      if (isNew) {
        docRef.set(ClienteDto.toCreateMap(entity)).await()
      } else {
        docRef.update(ClienteDto.toUpdateMap(entity)).await()
      }
      Result.success(entity)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteCliente(userId: String, clienteId: String): Result<Unit> {
    val docRef = clientesCol(userId).document(clienteId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addStaffMember(userId: String, nombre: String, email: String, role: String): Result<UserEntity> {
    val staffId = "STF-${UUID.randomUUID().toString().take(8)}"
    val docRef = staffCol(userId).document(staffId)
    return try {
      val cleanName = nombre.trim().take(100)
      val cleanEmail = email.trim().take(150)
      val cleanRole = if (role == "admin") "admin" else "empleado"
      docRef.set(
        mapOf(
          "nombre" to cleanName,
          "email" to cleanEmail,
          "role" to cleanRole,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(UserEntity(uid = staffId, email = cleanEmail, nombre = cleanName, role = cleanRole))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun registrarCompra(
    userId: String,
    proveedorId: String,
    medId: String,
    cantidad: Int,
    costo: Double,
    estado: String,
    currentCount: Int,
    nota: String? = null,
  ): Result<CompraEntity> {
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
    val compraRef = comprasCol(userId).document(code)
    val medRef = medsCol(userId).document(medId)
    return try {
      db.runTransaction { transaction ->
        val medSnap = transaction.get(medRef)
        if (!medSnap.exists()) {
          throw IllegalStateException("Medicamento no encontrado: $medId")
        }
        if (estado == "Completado") {
          val currentStock = medSnap.getLong("stock") ?: 0L
          transaction.update(
            medRef,
            mapOf(
              "stock" to (currentStock + cantidad.toLong()),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        transaction.set(compraRef, CompraDto.toCreateMap(entity))
      }.await()
      Result.success(entity)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, compraRef.path)
      Result.failure(e)
    }
  }

  suspend fun recibirCompra(userId: String, compraId: String): Result<Boolean> {
    val compraRef = comprasCol(userId).document(compraId)
    return try {
      val updated = db.runTransaction { transaction ->
        val compraSnap = transaction.get(compraRef)
        if (!compraSnap.exists()) return@runTransaction false
        val estadoActual = compraSnap.getString("estado") ?: return@runTransaction false
        if (estadoActual != "Pendiente") return@runTransaction false

        val medId = compraSnap.getString("medId") ?: return@runTransaction false
        val cantidad = (compraSnap.getLong("cantidad") ?: 0L)
        val medRef = medsCol(userId).document(medId)
        val medSnap = transaction.get(medRef)
        if (!medSnap.exists()) {
          throw IllegalStateException("Medicamento asociado no encontrado: $medId")
        }

        val currentStock = medSnap.getLong("stock") ?: 0L
        transaction.update(
          medRef,
          mapOf(
            "stock" to (currentStock + cantidad),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        transaction.update(
          compraRef,
          mapOf(
            "estado" to "Completado",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        true
      }.await()
      Result.success(updated)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, compraRef.path)
      Result.failure(e)
    }
  }

  suspend fun registrarVenta(
    userId: String,
    items: List<CartLineItem>,
    descPct: Double,
    vendedor: UserEntity,
    clienteId: String?,
    currentSalesCount: Int,
  ): Result<VentaWithItems> {
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
    val ventaItems = items.mapIndexed { idx, item ->
      VentaItemEntity(
        itemId = idx + 1,
        ventaId = uuid,
        medId = item.medId,
        nombre = item.nombre,
        cantidad = item.cantidad,
        precio = item.precio,
      )
    }

    val ventaRef = ventasCol(userId).document(uuid)
    return try {
      db.runTransaction { transaction ->
        // Phase 1: All Firestore reads BEFORE any writes
        val medSnapshots = items.map { item ->
          val ref = medsCol(userId).document(item.medId)
          item to (ref to transaction.get(ref))
        }
        val clienteRef = if (!clienteId.isNullOrBlank()) clientesCol(userId).document(clienteId) else null
        val clienteSnap = if (clienteRef != null) transaction.get(clienteRef) else null

        // Phase 2: Stock validation
        val stockUpdates = medSnapshots.map { (item, pair) ->
          val (ref, snap) = pair
          if (!snap.exists()) {
            throw IllegalStateException("Producto no encontrado: ${item.nombre}")
          }
          val stockActual = snap.getLong("stock") ?: 0L
          if (stockActual < item.cantidad.toLong()) {
            throw IllegalStateException("Stock insuficiente para ${item.nombre} (disp: $stockActual)")
          }
          ref to (stockActual - item.cantidad.toLong())
        }

        // Phase 3: Atomic writes
        stockUpdates.forEach { (ref, newStock) ->
          transaction.update(
            ref,
            mapOf(
              "stock" to newStock,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        if (clienteRef != null && clienteSnap != null && clienteSnap.exists() && puntos > 0) {
          val puntosActuales = clienteSnap.getLong("puntos") ?: 0L
          transaction.update(
            clienteRef,
            mapOf(
              "puntos" to (puntosActuales + puntos.toLong()),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        transaction.set(ventaRef, VentaDto.toCreateMap(venta, ventaItems))
      }.await()
      Result.success(VentaWithItems(venta, ventaItems))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, ventaRef.path)
      Result.failure(e)
    }
  }

  private companion object {
    const val BOOTSTRAP_ADMIN_EMAIL = "pabloalexander361@gmail.com"
  }
}
