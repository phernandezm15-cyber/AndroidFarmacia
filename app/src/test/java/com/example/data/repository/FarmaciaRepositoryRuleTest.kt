package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.MedicamentoEntity
import com.example.data.local.UserEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FarmaciaRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserWorkspaceInitialized_authenticatedUser_createsProfileAndSeedsWorkspace() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = FarmaciaRepository(firestore)

    val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserWorkspaceInitialized(
        userId = uid,
        email = ALICE_EMAIL,
        displayName = "Alice Farmacia",
        isEmailVerified = true,
      )
    }
    assertTrue(profileResult.isSuccess)
    val meds = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMedicamentos(uid).first { it.isNotEmpty() }
    }
    assertTrue(meds.size >= 20)
  }

  @Test
  fun saveAndObserveMedicamentos_ownerCanCrudAndReceiveRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = FarmaciaRepository(firestore)
    val uniqueId = "MED-TEST-${UUID.randomUUID().toString().take(8)}"

    val saved = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveMedicamento(
        userId = uid,
        med = MedicamentoEntity(
          id = "",
          codigo = "75099988877",
          nombre = "TestMed $uniqueId",
          principio = "Paracetamol 500mg",
          categoria = "Analgésicos",
          caducidad = "2028-12-31",
          precioCompra = 10.0,
          precioVenta = 18.0,
          stock = 40,
        ),
        currentCount = 20,
      ).getOrThrow()
    }

    val emitted = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMedicamentos(uid).first { list -> list.any { it.id == saved.id } }
    }
    assertTrue(emitted.any { it.id == saved.id && it.stock == 40 })

    val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveMedicamento(uid, saved.copy(stock = 55), 21)
    }
    assertTrue(updated.isSuccess)

    val deleted = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteMedicamento(uid, saved.id)
    }
    assertTrue(deleted.isSuccess)
  }

  @Test
  fun registrarVenta_decrementsStockAndAwardsClientePointsTransactionally() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = FarmaciaRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserWorkspaceInitialized(uid, ALICE_EMAIL, "Alice", true).getOrThrow()
    }

    val initialMed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMedicamentos(uid).first { list -> list.any { it.id == "MED-001" } }
    }.first { it.id == "MED-001" }
    val initialCli = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeClientes(uid).first { list -> list.any { it.id == "CLI-001" } }
    }.first { it.id == "CLI-001" }

    val saleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.registrarVenta(
        userId = uid,
        items = listOf(CartLineItem("MED-001", initialMed.nombre, 100.0, 2)),
        descPct = 0.0,
        vendedor = UserEntity(uid = uid, email = ALICE_EMAIL, nombre = "Alice", role = "empleado"),
        clienteId = "CLI-001",
        currentSalesCount = 24,
      )
    }
    assertTrue(saleResult.isSuccess)

    val updatedMed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMedicamentos(uid).first { list ->
        list.any { it.id == "MED-001" && it.stock == initialMed.stock - 2 }
      }
    }.first { it.id == "MED-001" }
    assertEquals(initialMed.stock - 2, updatedMed.stock)

    val updatedCli = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeClientes(uid).first { list ->
        list.any { it.id == "CLI-001" && it.puntos > initialCli.puntos }
      }
    }.first { it.id == "CLI-001" }
    assertTrue(updatedCli.puntos >= initialCli.puntos + 10)
  }

  @Test
  fun registrarAndRecibirCompra_incrementsStockAtomically() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = FarmaciaRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserWorkspaceInitialized(uid, ALICE_EMAIL, "Alice", true).getOrThrow()
    }

    val beforeMed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMedicamentos(uid).first { list -> list.any { it.id == "MED-002" } }
    }.first { it.id == "MED-002" }

    val purchase = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.registrarCompra(
        userId = uid,
        proveedorId = "PRV-001",
        medId = "MED-002",
        cantidad = 15,
        costo = 12.0,
        estado = "Pendiente",
        currentCount = 6,
      ).getOrThrow()
    }

    val received = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.recibirCompra(uid, purchase.id).getOrThrow()
    }
    assertTrue(received)

    val afterMed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMedicamentos(uid).first { list ->
        list.any { it.id == "MED-002" && it.stock == beforeMed.stock + 15 }
      }
    }.first { it.id == "MED-002" }
    assertEquals(beforeMed.stock + 15, afterMed.stock)
  }

  @Test
  fun crossUserAccess_bobCannotReadOrWriteAliceWorkspace_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FarmaciaRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureUserWorkspaceInitialized(aliceUid, ALICE_EMAIL, "Alice", true).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = FarmaciaRepository(firestore)
    val writeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.deleteMedicamento(aliceUid, "MED-001")
    }
    assertTrue(writeResult.isFailure)
    val fsErr = writeResult.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)

    var flowException: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeMedicamentos(aliceUid).first()
      }
    } catch (e: Exception) {
      flowException = e
    }
    val readErr = checkNotNull(flowException?.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $flowException"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, readErr.code)
  }

  @Test
  fun unauthenticatedCaller_observeMedicamentos_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = FarmaciaRepository(firestore)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeMedicamentos("some_uid_123").first()
      }
    } catch (e: Exception) {
      caught = e
    }
    val fsErr = checkNotNull(caught?.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $caught"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice.farmacia@test.com"
    const val BOB_EMAIL = "bob.farmacia@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
