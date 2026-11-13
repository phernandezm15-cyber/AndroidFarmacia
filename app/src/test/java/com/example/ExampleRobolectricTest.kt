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
import com.example.data.repository.FarmaciaRepository
import com.example.domain.PharmacyUtils
import com.example.ui.components.configureAppCheck
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
      val repo = FarmaciaRepository(db.dao())

      // Empleado registers normally without admin key
      val empResult = repo.registerUser("Ana Empleado", "ana@farmacia.com", "Farmacia123!", "empleado", null)
      assertTrue(empResult.isSuccess)
      assertFalse(empResult.getOrThrow().isAdmin)

      // Admin rejects wrong key and accepts farmaciadbadmin123
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

      // Direct password reset without email or verification code
      val reset = repo.resetPasswordDirect("ana@farmacia.com", "NuevaClave99!")
      assertTrue(reset.isSuccess)

      assertTrue(repo.loginUser("ana@farmacia.com", "Farmacia123!").isFailure)
      assertTrue(repo.loginUser("ana@farmacia.com", "NuevaClave99!").isSuccess)
    } finally {
      db.close()
    }
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
