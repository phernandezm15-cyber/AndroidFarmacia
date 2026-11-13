package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.content.res.ResourcesCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FarmaciaDatabase
import com.example.data.local.PasswordHasher
import com.example.data.remote.PasswordHashPatch
import com.example.data.remote.RecibirCompraRpcRequest
import com.example.data.remote.RegistrarCompraRpcRequest
import com.example.data.remote.RegistrarVentaRpcRequest
import com.example.data.remote.SupabaseApi
import com.example.data.remote.SupabaseClienteRow
import com.example.data.remote.SupabaseCompraRow
import com.example.data.remote.SupabaseMedicamentoRow
import com.example.data.remote.SupabaseProveedorRow
import com.example.data.remote.SupabaseUserRow
import com.example.data.remote.SupabaseVentaItemInsert
import com.example.data.remote.SupabaseVentaItemRow
import com.example.data.remote.SupabaseVentaRow
import com.example.data.repository.CartLineItem
import com.example.data.repository.RoomFarmaciaRepository
import com.example.data.repository.SupabaseFarmaciaRepository
import com.example.domain.PharmacyUtils
import com.example.ui.components.configureAppCheck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "xxxhdpi")
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FarmaciaDB", appName)
  }

  @Test
  fun `custom font resource loads cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
    assertNotNull(typeface)
  }

  @Test
  fun `configureAppCheck does not throw`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    configureAppCheck(context, Intent())
  }

  @Test
  fun `password hasher creates salted pbkdf2 hash and verifies`() {
    val hash = PasswordHasher.hash("Farmacia2026!")
    assertTrue(PasswordHasher.verify("Farmacia2026!", hash))
    assertFalse(PasswordHasher.verify("ClaveIncorrecta1", hash))
  }

  @Test
  fun `room repository registers login resets password directly without email`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, FarmaciaDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = RoomFarmaciaRepository(db.dao())

      val empResult = repo.registerUser("Ana Empleado", "ana@farmacia.com", "Farmacia123!", "empleado", null)
      assertTrue(empResult.isSuccess)
      assertFalse(empResult.getOrThrow().isAdmin)

      val badAdmin = repo.registerUser("Juan Admin", "admin@farmacia.com", "Farmacia123!", "admin", "wrong")
      assertTrue(badAdmin.isFailure)

      val goodAdmin = repo.registerUser(
        "Juan Admin",
        "admin@farmacia.com",
        "Farmacia123!",
        "admin",
        PharmacyUtils.ADMIN_MASTER_PASSWORD,
      )
      assertTrue(goodAdmin.isSuccess)
      assertTrue(goodAdmin.getOrThrow().isAdmin)

      val reset = repo.resetPasswordDirect("ana@farmacia.com", "NuevaClave99!")
      assertTrue(reset.isSuccess)

      assertTrue(repo.loginUser("ana@farmacia.com", "Farmacia123!").isFailure)
      assertTrue(repo.loginUser("ana@farmacia.com", "NuevaClave99!").isSuccess)
    } finally {
      db.close()
    }
  }

  @Test
  fun `supabase repository syncs shared database across devices and supports direct password reset`() = runTest {
    val fakeApi = InMemoryFakeSupabaseApi()
    val phoneARepo = SupabaseFarmaciaRepository(fakeApi)
    val phoneBRepo = SupabaseFarmaciaRepository(fakeApi)

    // Phone A creates Admin using farmaciadbadmin123
    val adminReg = phoneARepo.registerUser(
      "Pablo Admin",
      "pablo@farmacia.com",
      "Farmacia123!",
      "admin",
      PharmacyUtils.ADMIN_MASTER_PASSWORD,
    )
    assertTrue(adminReg.isSuccess)
    assertTrue(adminReg.getOrThrow().isAdmin)

    // Phone B creates Empleado normally and resets password directly
    val empReg = phoneBRepo.registerUser("Maria Caja", "maria@farmacia.com", "Caja2026!", "empleado", null)
    assertTrue(empReg.isSuccess)
    assertFalse(empReg.getOrThrow().isAdmin)

    assertTrue(phoneBRepo.resetPasswordDirect("maria@farmacia.com", "CajaNueva99!").isSuccess)
    assertTrue(phoneBRepo.loginUser("maria@farmacia.com", "CajaNueva99!").isSuccess)

    // Phone B sells 2 units of MED-001 (initial stock 120 -> 118)
    val sale = phoneBRepo.registrarVenta(
      items = listOf(CartLineItem("MED-001", "Panadol Extra", 14.0, 2)),
      descPct = 0.0,
      vendedor = empReg.getOrThrow(),
      clienteId = null,
      currentSalesCount = 24,
    )
    assertTrue(sale.isSuccess)

    // Phone A refreshes and sees updated stock (118) from Phone B
    assertTrue(phoneARepo.refreshAll().isSuccess)
    val updatedMed = phoneARepo.observeMedicamentos().first().first { it.id == "MED-001" }
    assertEquals(118, updatedMed.stock)
  }

  @Test
  fun `main activity launches and renders normal login screen without google`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("login_email_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_password_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_submit_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tab_register").assertIsDisplayed()
  }

  @Test
  fun `register tab requires admin code only when admin role is selected`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tab_register").performClick()
    composeTestRule.onNodeWithTag("register_name_input").performTextInput("Carlos Perez")
    composeTestRule.onNodeWithTag("register_email_input").performTextInput("carlos@farmacia.com")
    composeTestRule.onNodeWithTag("register_password_input").performTextInput("Farmacia123!")

    composeTestRule.onNodeWithTag("admin_code_input").assertDoesNotExist()
    composeTestRule.onNodeWithTag("role_admin_chip").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("admin_code_input").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("admin_code_input").performTextInput("clave_erronea")
    composeTestRule.onNodeWithTag("register_submit_button").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("auth_error_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Clave de autorización incorrecta.").assertIsDisplayed()
  }

  @Test
  fun `forgot password dialog opens direct password form without verification`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("forgot_password_button").performScrollTo().performClick()

    composeTestRule.onNodeWithTag("reset_email_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("reset_new_password_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("reset_confirm_password_input").assertIsDisplayed()

    composeTestRule.onNodeWithTag("reset_email_input").performTextInput("usuario@farmacia.com")
    composeTestRule.onNodeWithTag("reset_new_password_input").performTextInput("Farmacia2026!")
    composeTestRule.onNodeWithTag("reset_confirm_password_input").performTextInput("OtraClave99!")
    composeTestRule.onNodeWithTag("confirm_password_reset_button").performClick()

    composeTestRule.onNodeWithTag("reset_error_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Las contraseñas no coinciden.").assertIsDisplayed()
  }
}

private class InMemoryFakeSupabaseApi : SupabaseApi {
  private val users = mutableListOf<SupabaseUserRow>()
  private val meds = mutableListOf<SupabaseMedicamentoRow>()
  private val provs = mutableListOf<SupabaseProveedorRow>()
  private val clientes = mutableListOf<SupabaseClienteRow>()
  private val compras = mutableListOf<SupabaseCompraRow>()
  private val ventas = mutableListOf<SupabaseVentaRow>()
  private val ventaItems = mutableListOf<SupabaseVentaItemRow>()
  private var nextItemId = 1

  override suspend fun findUserByEmail(emailFilter: String, select: String, limit: Int): List<SupabaseUserRow> {
    val target = emailFilter.removePrefix("eq.")
    return users.filter { it.email.equals(target, ignoreCase = true) }.take(limit)
  }

  override suspend fun findUserById(uidFilter: String, select: String, limit: Int): List<SupabaseUserRow> {
    val target = uidFilter.removePrefix("eq.")
    return users.filter { it.uid == target }.take(limit)
  }

  override suspend fun getUsers(select: String, order: String): List<SupabaseUserRow> =
    users.sortedBy { it.nombre }

  override suspend fun insertUser(user: SupabaseUserRow, prefer: String): List<SupabaseUserRow> {
    users.add(user)
    return listOf(user)
  }

  override suspend fun updateUserPasswordHash(
    uidFilter: String,
    patch: PasswordHashPatch,
    prefer: String,
  ): List<SupabaseUserRow> {
    val target = uidFilter.removePrefix("eq.")
    val idx = users.indexOfFirst { it.uid == target }
    if (idx < 0) return emptyList()
    val updated = users[idx].copy(passwordHash = patch.passwordHash)
    users[idx] = updated
    return listOf(updated)
  }

  override suspend fun getMedicamentos(select: String, order: String, limit: Int?): List<SupabaseMedicamentoRow> {
    val sorted = meds.sortedBy { it.id }
    return if (limit != null) sorted.take(limit) else sorted
  }

  override suspend fun upsertMedicamentos(
    meds: List<SupabaseMedicamentoRow>,
    prefer: String,
  ): List<SupabaseMedicamentoRow> {
    meds.forEach { m ->
      val idx = this.meds.indexOfFirst { it.id == m.id }
      if (idx >= 0) this.meds[idx] = m else this.meds.add(m)
    }
    return meds
  }

  override suspend fun deleteMedicamento(idFilter: String) {
    val target = idFilter.removePrefix("eq.")
    meds.removeAll { it.id == target }
  }

  override suspend fun getProveedores(select: String, order: String): List<SupabaseProveedorRow> =
    provs.sortedBy { it.id }

  override suspend fun upsertProveedores(
    provs: List<SupabaseProveedorRow>,
    prefer: String,
  ): List<SupabaseProveedorRow> {
    provs.forEach { p ->
      val idx = this.provs.indexOfFirst { it.id == p.id }
      if (idx >= 0) this.provs[idx] = p else this.provs.add(p)
    }
    return provs
  }

  override suspend fun deleteProveedor(idFilter: String) {
    val target = idFilter.removePrefix("eq.")
    provs.removeAll { it.id == target }
  }

  override suspend fun getClientes(select: String, order: String): List<SupabaseClienteRow> =
    clientes.sortedBy { it.id }

  override suspend fun upsertClientes(
    clientes: List<SupabaseClienteRow>,
    prefer: String,
  ): List<SupabaseClienteRow> {
    clientes.forEach { c ->
      val idx = this.clientes.indexOfFirst { it.id == c.id }
      if (idx >= 0) this.clientes[idx] = c else this.clientes.add(c)
    }
    return clientes
  }

  override suspend fun deleteCliente(idFilter: String) {
    val target = idFilter.removePrefix("eq.")
    clientes.removeAll { it.id == target }
  }

  override suspend fun getCompras(select: String, order: String): List<SupabaseCompraRow> =
    compras.sortedByDescending { it.fecha }

  override suspend fun upsertCompras(
    compras: List<SupabaseCompraRow>,
    prefer: String,
  ): List<SupabaseCompraRow> {
    compras.forEach { c ->
      val idx = this.compras.indexOfFirst { it.id == c.id }
      if (idx >= 0) this.compras[idx] = c else this.compras.add(c)
    }
    return compras
  }

  override suspend fun getVentas(select: String, order: String): List<SupabaseVentaRow> =
    ventas.sortedByDescending { it.fecha }.map { v ->
      v.copy(ventaItems = ventaItems.filter { it.ventaId == v.uuid })
    }

  override suspend fun upsertVentas(ventas: List<SupabaseVentaRow>, prefer: String) {
    ventas.forEach { v ->
      if (this.ventas.none { it.uuid == v.uuid }) {
        this.ventas.add(v.copy(ventaItems = null))
      }
    }
  }

  override suspend fun insertVentaItems(items: List<SupabaseVentaItemInsert>, prefer: String) {
    items.forEach { item ->
      ventaItems.add(
        SupabaseVentaItemRow(
          itemId = nextItemId++,
          ventaId = item.ventaId,
          medId = item.medId,
          nombre = item.nombre,
          cantidad = item.cantidad,
          precio = item.precio,
        )
      )
    }
  }

  override suspend fun registrarCompraRpc(payload: RegistrarCompraRpcRequest) {
    if (payload.estado == "Completado") {
      val idx = meds.indexOfFirst { it.id == payload.medId }
      if (idx >= 0) meds[idx] = meds[idx].copy(stock = meds[idx].stock + payload.cantidad)
    }
    compras.add(
      SupabaseCompraRow(
        payload.id, payload.codigo, payload.fecha, payload.proveedorId,
        payload.medId, payload.cantidad, payload.costo, payload.estado, payload.nota,
      )
    )
  }

  override suspend fun recibirCompraRpc(payload: RecibirCompraRpcRequest): Boolean {
    val idx = compras.indexOfFirst { it.id == payload.compraId }
    if (idx < 0 || compras[idx].estado != "Pendiente") return false
    val medIdx = meds.indexOfFirst { it.id == compras[idx].medId }
    if (medIdx >= 0) meds[medIdx] = meds[medIdx].copy(stock = meds[medIdx].stock + compras[idx].cantidad)
    compras[idx] = compras[idx].copy(estado = "Completado")
    return true
  }

  override suspend fun registrarVentaRpc(payload: RegistrarVentaRpcRequest) {
    payload.items.forEach { item ->
      val idx = meds.indexOfFirst { it.id == item.medId }
      val current = meds[idx]
      meds[idx] = current.copy(stock = current.stock - item.cantidad)
    }
    ventas.add(
      SupabaseVentaRow(
        uuid = payload.uuid,
        codigo = payload.codigo,
        fecha = payload.fecha,
        subtotal = payload.subtotal,
        descuento = payload.descuento,
        descPct = payload.descPct,
        iva = payload.iva,
        total = payload.total,
        vendedorNombre = payload.vendedorNombre,
        vendedorId = payload.vendedorId,
        clienteId = payload.clienteId,
        puntos = payload.puntos,
      )
    )
    payload.items.forEach { item ->
      ventaItems.add(
        SupabaseVentaItemRow(
          itemId = nextItemId++,
          ventaId = payload.uuid,
          medId = item.medId,
          nombre = item.nombre,
          cantidad = item.cantidad,
          precio = item.precio,
        )
      )
    }
  }
}
