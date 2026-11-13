package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.FarmaciaLogo
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  isSigningIn: Boolean,
  authError: String?,
  onGoogleIdToken: (String) -> Unit,
  onAuthError: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isLaunchingCredentialSheet by remember { mutableStateOf(false) }

  fun launchGoogleSignIn() {
    val webClientIdResId = context.resources.getIdentifier(
      "default_web_client_id",
      "string",
      context.packageName,
    )
    val webClientId = if (webClientIdResId != 0) context.getString(webClientIdResId) else ""
    if (webClientId.isBlank()) {
      onAuthError("No se encontró default_web_client_id en la configuración de Google Services.")
      return
    }

    val activityContext = context.findActivity() ?: context
    val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    isLaunchingCredentialSheet = true
    scope.launch {
      try {
        val credentialManager = CredentialManager.create(activityContext)
        val result = credentialManager.getCredential(
          context = activityContext,
          request = request,
        )
        val credential = result.credential
        if (credential is CustomCredential &&
          credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
          val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
          onGoogleIdToken(googleIdTokenCredential.idToken)
        } else {
          onAuthError("Tipo de credencial incompatible recibido de Google.")
        }
      } catch (_: GetCredentialCancellationException) {
        onAuthError("Inicio de sesión cancelado.")
      } catch (_: NoCredentialException) {
        onAuthError("No hay cuentas de Google disponibles en este dispositivo. Agrega una cuenta en Ajustes.")
      } catch (e: Exception) {
        onAuthError(e.localizedMessage ?: "No se pudo completar el inicio de sesión con Google.")
      } finally {
        isLaunchingCredentialSheet = false
      }
    }
  }

  val busy = isSigningIn || isLaunchingCredentialSheet

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
          )
        )
      )
      .imePadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      ) {
        Column {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_pharmacy_banner_1794549812251),
              contentDescription = "Ilustración FarmaciaDB",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                  )
                ),
            )
            FarmaciaLogo(
              modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            )
          }

          Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Text(
              text = "Gestión Farmacéutica en la Nube",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Inicia sesión con tu cuenta de Google para sincronizar tu inventario, punto de venta, clientes y análisis clínico en tiempo real.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(16.dp),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                FeatureHighlightRow(
                  icon = Icons.Filled.CloudDone,
                  title = "Base de datos en tiempo real",
                  subtitle = "Respaldo seguro en Cloud Firestore por sucursal y usuario.",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                FeatureHighlightRow(
                  icon = Icons.Filled.PointOfSale,
                  title = "POS, Lotes, Compras y Reportes",
                  subtitle = "Control transaccional de stock, IVA, Puntos Salud y PDF/CSV.",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                FeatureHighlightRow(
                  icon = Icons.Filled.AutoAwesome,
                  title = "Asistente Clínico FarmaIA",
                  subtitle = "Consultas farmacológicas y métricas en vivo con Gemini.",
                )
              }
            }

            if (!authError.isNullOrBlank()) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
              ) {
                Text(
                  text = authError,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                )
              }
            }

            Button(
              onClick = { launchGoogleSignIn() },
              enabled = !busy,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("google_signin_button"),
            ) {
              if (busy) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                  text = "Conectando con Google...",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
              } else {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                  text = "Continuar con Google",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "Autenticación protegida por Google Identity y Firebase Auth",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer,
      shape = RoundedCornerShape(10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .padding(8.dp)
          .size(20.dp),
      )
    }
    Column {
      Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}
