package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FarmaciaDatabase
import com.example.data.local.MedicamentoEntity
import com.example.data.local.UserEntity
import com.example.data.repository.CartLineItem
import com.example.data.repository.RoomFarmaciaRepository
import com.example.domain.PharmacyUtils
import com.example.ui.screens.PosScreen
import com.example.ui.theme.FarmaciaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "xxxhdpi")
class PosCheckoutTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `compact phone pos shows sticky checkout bar and unclipped pay button`() {
    val sampleMed = MedicamentoEntity(
      id = "MED-001",
      codigo = "7501001",
      nombre = "Panadol Extra",
      principio = "Paracetamol 500mg",
      categoria = "Analgésicos",
      caducidad = "2028-06-01",
      precioCompra = 8.0,
      precioVenta = 14.0,
      stock = 120,
    )
    var submittedCash: String? = null

    composeRule.setContent {
      FarmaciaTheme {
        var cart by remember { mutableStateOf<List<CartLineItem>>(emptyList()) }
        Box(modifier = Modifier.size(360.dp, 640.dp)) {
          PosScreen(
            user = UserEntity("u1", "caja@farmacia.com", "Cajero", "empleado"),
            meds = listOf(sampleMed),
            clientes = emptyList(),
            cart = cart,
            discountPct = 0.0,
            selectedClientId = null,
            focusMode = false,
            isProcessing = false,
            saleError = null,
            lastReceipt = null,
            onToggleFocus = {},
            onAddToCart = {
              cart = listOf(CartLineItem(sampleMed.id, sampleMed.nombre, sampleMed.precioVenta, 1))
            },
            onDecrementCart = { cart = emptyList() },
            onRemoveCart = { cart = emptyList() },
            onClearCart = { cart = emptyList() },
            onDiscountChange = {},
            onClientChange = {},
            onScanCodeSubmit = { false },
            onCompleteSale = { cash -> submittedCash = cash },
            onClearSaleError = {},
            onClearReceipt = {},
          )
        }
      }
    }

    composeRule.onNodeWithTag("pos_add_MED-001").performClick()
    composeRule.onNodeWithTag("pos_checkout_bar").assertIsDisplayed()
    composeRule.onNodeWithTag("pos_open_checkout_button").assertIsDisplayed().performClick()

    composeRule.onNodeWithTag("pos_cash_input").assertIsDisplayed()
    composeRule.onNodeWithTag("pos_exact_cash_button").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("pos_complete_sale_button").assertIsDisplayed().performClick()

    val expectedTotal = String.format(
      java.util.Locale.US,
      "%.2f",
      PharmacyUtils.calcTotals(listOf(CartLineItem("MED-001", "Panadol Extra", 14.0, 1)), 0.0).total,
    )
    assertNotNull(submittedCash)
    assertEquals(expectedTotal, submittedCash)
  }

  @Test
  fun `room repository registrarVenta deducts stock atomically`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, FarmaciaDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = RoomFarmaciaRepository(db.dao())
      val seller = repo.registerUser("Ana Caja", "ana@caja.com", "Farmacia123!", "empleado", null).getOrThrow()
      val initialMed = repo.observeMedicamentos().first().first { it.id == "MED-001" }

      val result = repo.registrarVenta(
        items = listOf(CartLineItem(initialMed.id, initialMed.nombre, initialMed.precioVenta, 3)),
        descPct = 0.0,
        vendedor = seller,
        clienteId = null,
        currentSalesCount = 24,
      )
      assertTrue(result.isSuccess)

      val afterMed = repo.observeMedicamentos().first().first { it.id == "MED-001" }
      assertEquals(initialMed.stock - 3, afterMed.stock)
    } finally {
      db.close()
    }
  }
}
