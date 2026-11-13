package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.UserEntity
import com.example.domain.EstadoMed
import com.example.domain.PharmacyUtils
import com.example.domain.PharmacyVoiceMatcher
import com.example.ui.theme.FarmaciaColors
import java.util.Locale

@Composable
fun FarmaciaLogo(
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_farmacia_logo_1794549812075),
      contentDescription = "Logo FarmaciaDB",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(if (compact) 36.dp else 44.dp)
        .clip(RoundedCornerShape(12.dp)),
    )
    Column {
      Text(
        text = "FarmaciaDB",
        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (!compact) {
        Text(
          text = "Sistema Farmacéutico",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun BarcodeView(
  code: String,
  modifier: Modifier = Modifier,
) {
  val barColor = MaterialTheme.colorScheme.onSurface
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Canvas(
      modifier = Modifier
        .width(96.dp)
        .height(24.dp)
    ) {
      val bars = 32
      val slot = size.width / bars
      val seed = code.ifBlank { "75010000000" }
      for (i in 0 until bars) {
        val ch = seed[i % seed.length].code
        val thick = ((ch + i * 7) % 3) != 0
        if (thick || i % 2 == 0) {
          val w = if ((ch + i) % 4 == 0) slot * 0.75f else slot * 0.45f
          drawRect(
            color = barColor,
            topLeft = Offset(i * slot, 0f),
            size = Size(w, size.height),
          )
        }
      }
    }
    Text(
      text = code,
      style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun StatusBadge(estado: EstadoMed, modifier: Modifier = Modifier) {
  val ext = FarmaciaColors.extended
  val (bg, fg) = when (estado) {
    EstadoMed.OPTIMO -> ext.successContainer to ext.success
    EstadoMed.POR_VENCER -> ext.warningContainer to ext.warning
    EstadoMed.CRITICO -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
  }
  Surface(
    modifier = modifier,
    color = bg,
    shape = RoundedCornerShape(999.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(fg),
      )
      Text(
        text = estado.label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun RoleBannerCard(isAdmin: Boolean, adminText: String, employeeText: String) {
  val ext = FarmaciaColors.extended
  val borderColor = if (isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else ext.success.copy(alpha = 0.35f)
  val bgColor = if (isAdmin) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else ext.successContainer.copy(alpha = 0.45f)
  val iconTint = if (isAdmin) MaterialTheme.colorScheme.primary else ext.success

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = if (isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.Person,
        contentDescription = if (isAdmin) "Administrador" else "Empleado",
        tint = iconTint,
      )
      Text(
        text = if (isAdmin) adminText else employeeText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
fun ConfirmDeleteDialog(
  title: String,
  description: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold) },
    text = { Text(description, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_delete_button"),
      ) {
        Text("Eliminar")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSearchButton(
  meds: List<MedicamentoEntity>,
  onResolvedQuery: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showDialog by remember { mutableStateOf(false) }
  var spokenInput by remember { mutableStateOf("") }
  var feedbackText by remember { mutableStateOf<String?>(null) }

  val speechLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
      val raw = matches?.firstOrNull().orEmpty()
      if (raw.isNotBlank()) {
        val resolved = PharmacyVoiceMatcher.resolvePharmaceuticalVoiceQuery(raw, meds)
        feedbackText = resolved.reason ?: "Buscando: ${resolved.searchTerm}"
        onResolvedQuery(resolved.searchTerm)
      }
    }
  }

  FilledTonalIconButton(
    onClick = { showDialog = true },
    modifier = modifier
      .size(48.dp)
      .testTag("voice_search_button"),
  ) {
    Icon(
      imageVector = Icons.Filled.Mic,
      contentDescription = "Búsqueda por voz farmacéutica",
    )
  }

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("Búsqueda Inteligente por Voz", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Dicta un medicamento o síntoma. El motor fonético corrige términos como \"amoxicilina\", \"para dolor\" o \"dolor de cabeza\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = {
              val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "GT").toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el nombre del medicamento o síntoma...")
              }
              try {
                speechLauncher.launch(intent)
              } catch (_: Exception) {
                feedbackText = "Dictado del sistema no disponible en este dispositivo; escribe o elige una sugerencia rápida."
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("launch_system_speech_button"),
          ) {
            Icon(Icons.Filled.Mic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Hablar con Micrófono")
          }
          OutlinedTextField(
            value = spokenInput,
            onValueChange = { spokenInput = it },
            label = { Text("O escribe frase dictada / síntoma") },
            placeholder = { Text("Ej: dolor de cabeza, para dolor, omeprazol") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Text("Ejemplos rápidos:", style = MaterialTheme.typography.labelMedium)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            listOf("Panadol", "Dolor de cabeza", "Gastritis", "Antibiótico", "Alergia").forEach { chip ->
              AssistChip(
                onClick = {
                  val resolved = PharmacyVoiceMatcher.resolvePharmaceuticalVoiceQuery(chip, meds)
                  onResolvedQuery(resolved.searchTerm)
                  showDialog = false
                },
                label = { Text(chip) },
              )
            }
          }
          if (!feedbackText.isNullOrBlank()) {
            Text(
              text = feedbackText!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (spokenInput.isNotBlank()) {
              val resolved = PharmacyVoiceMatcher.resolvePharmaceuticalVoiceQuery(spokenInput, meds)
              onResolvedQuery(resolved.searchTerm)
            }
            showDialog = false
          },
          modifier = Modifier.testTag("apply_voice_query_button"),
        ) {
          Text("Buscar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDialog = false }) { Text("Cerrar") }
      },
    )
  }
}

@Composable
fun RoleDetailsDialog(
  user: UserEntity,
  hasPasswordAuth: Boolean = false,
  onChangePassword: (
    currentPassword: String,
    newPassword: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) -> Unit = { _, _, _, _ -> },
  onDismiss: () -> Unit,
) {
  val ext = FarmaciaColors.extended
  var showPasswordForm by remember { mutableStateOf(false) }
  var currentPass by remember { mutableStateOf("") }
  var newPass by remember { mutableStateOf("") }
  var confirmPass by remember { mutableStateOf("") }
  var showCurrent by remember { mutableStateOf(false) }
  var showNew by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }
  var successMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = { if (!isSaving) onDismiss() },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
          imageVector = if (user.isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.Person,
          contentDescription = null,
          tint = if (user.isAdmin) MaterialTheme.colorScheme.primary else ext.success,
        )
        Text("Perfil y Permisos", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("Usuario: ${user.nombre}", fontWeight = FontWeight.SemiBold)
        Text("Correo: ${user.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          text = "Rol: ${user.rolDisplay}",
          color = if (user.isAdmin) MaterialTheme.colorScheme.primary else ext.success,
          fontWeight = FontWeight.Bold,
        )
        HorizontalDivider()
        if (user.isAdmin) {
          Text("Privilegios de Administrador:", style = MaterialTheme.typography.labelLarge)
          Text("• Crear, editar y eliminar medicamentos y proveedores.", style = MaterialTheme.typography.bodySmall)
          Text("• Emitir órdenes de compra y ver costos al mayoreo.", style = MaterialTheme.typography.bodySmall)
          Text("• Acceso completo a Reportes Financieros y Personal.", style = MaterialTheme.typography.bodySmall)
        } else {
          Text("Privilegios de Empleado:", style = MaterialTheme.typography.labelLarge)
          Text("• Punto de Venta (POS), cobro y recibos.", style = MaterialTheme.typography.bodySmall)
          Text("• Consulta de catálogo, clientes Puntos Salud y FarmaIA.", style = MaterialTheme.typography.bodySmall)
          Text("• Corte de turno de ventas (sin edición de costos).", style = MaterialTheme.typography.bodySmall)
        }

        if (hasPasswordAuth) {
          HorizontalDivider()
          TextButton(
            onClick = {
              showPasswordForm = !showPasswordForm
              errorMsg = null
              successMsg = null
            },
            modifier = Modifier.testTag("toggle_change_password_button"),
          ) {
            Icon(Icons.Filled.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (showPasswordForm) "Ocultar cambio de contraseña" else "Cambiar contraseña directamente")
          }

          if (showPasswordForm) {
            OutlinedTextField(
              value = currentPass,
              onValueChange = { currentPass = it; errorMsg = null },
              label = { Text("Contraseña actual") },
              visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                IconButton(onClick = { showCurrent = !showCurrent }) {
                  Icon(if (showCurrent) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                }
              },
              singleLine = true,
              enabled = !isSaving,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_current_password_input"),
            )
            OutlinedTextField(
              value = newPass,
              onValueChange = { newPass = it; errorMsg = null },
              label = { Text("Nueva contraseña") },
              visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                IconButton(onClick = { showNew = !showNew }) {
                  Icon(if (showNew) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                }
              },
              singleLine = true,
              enabled = !isSaving,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_new_password_input"),
            )
            OutlinedTextField(
              value = confirmPass,
              onValueChange = { confirmPass = it; errorMsg = null },
              label = { Text("Confirmar nueva contraseña") },
              visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              enabled = !isSaving,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_confirm_password_input"),
            )
            if (!errorMsg.isNullOrBlank()) {
              Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (!successMsg.isNullOrBlank()) {
              Text(successMsg!!, color = ext.success, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            Button(
              onClick = {
                val analysis = PharmacyUtils.evaluatePasswordStrength(newPass)
                if (currentPass.isBlank()) {
                  errorMsg = "Ingresa tu contraseña actual."
                  return@Button
                }
                if (!analysis.isAllowed) {
                  errorMsg = analysis.feedback
                  return@Button
                }
                if (newPass != confirmPass) {
                  errorMsg = "Las contraseñas nuevas no coinciden."
                  return@Button
                }
                isSaving = true
                errorMsg = null
                onChangePassword(
                  currentPass,
                  newPass,
                  {
                    isSaving = false
                    currentPass = ""
                    newPass = ""
                    confirmPass = ""
                    successMsg = "Contraseña actualizada correctamente."
                  },
                  { err ->
                    isSaving = false
                    errorMsg = err
                  },
                )
              },
              enabled = !isSaving,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_save_password_button"),
            ) {
              Text(if (isSaving) "Guardando..." else "Guardar nueva contraseña")
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, enabled = !isSaving) { Text("Entendido") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
  meds: List<MedicamentoEntity>,
  compras: List<CompraEntity>,
  onDismiss: () -> Unit,
  onNavigateInventario: () -> Unit,
  onNavigateCompras: () -> Unit,
) {
  val lowStock = meds.filter { it.stock < 10 }
  val expiring = meds.filter { PharmacyUtils.diasParaVencer(it.caducidad) in 0..30 }
  val pending = compras.filter { it.estado == "Pendiente" }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("Centro de Alertas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Cerrar alertas")
        }
      }

      if (lowStock.isEmpty() && expiring.isEmpty() && pending.isEmpty()) {
        Text("No hay alertas activas en la farmacia.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      } else {
        LazyColumn(
          modifier = Modifier.height(300.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(lowStock) { m ->
            Card(
              onClick = {
                onDismiss()
                onNavigateInventario()
              },
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Column {
                  Text("Stock crítico: ${m.nombre}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                  Text("Solo quedan ${m.stock} unidades", style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
          items(expiring) { m ->
            val dias = PharmacyUtils.diasParaVencer(m.caducidad)
            Card(
              onClick = {
                onDismiss()
                onNavigateInventario()
              },
              colors = CardDefaults.cardColors(containerColor = FarmaciaColors.extended.warningContainer.copy(alpha = 0.55f)),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = FarmaciaColors.extended.warning)
                Column {
                  Text("Próximo a vencer: ${m.nombre}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                  Text("Vence en $dias días (${m.caducidad})", style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
          items(pending) { c ->
            val medName = meds.find { it.id == c.medId }?.nombre ?: c.medId
            Card(
              onClick = {
                onDismiss()
                onNavigateCompras()
              },
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                  Text("Compra pendiente: ${c.codigo}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                  Text("$medName · ${c.cantidad} uds.", style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun UsersManagementDialog(
  users: List<UserEntity>,
  onRegisterUser: (
    nombre: String,
    email: String,
    isAdminRole: Boolean,
    adminAuthKey: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  var showAdd by remember { mutableStateOf(false) }
  var nombre by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var isAdminRole by remember { mutableStateOf(false) }
  var adminKey by remember { mutableStateOf("") }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Personal y Roles", fontWeight = FontWeight.Bold)
        TextButton(onClick = { showAdd = !showAdd }) {
          Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text(if (showAdd) "Ver Lista" else "Nuevo")
        }
      }
    },
    text = {
      if (showAdd) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Cuenta Administrador", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = isAdminRole, onCheckedChange = { isAdminRole = it })
          }
          if (isAdminRole) {
            OutlinedTextField(
              value = adminKey,
              onValueChange = { adminKey = it },
              label = { Text("Clave maestra admin") },
              leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
          if (!errorMsg.isNullOrBlank()) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }
          Button(
            onClick = {
              onRegisterUser(
                nombre,
                email,
                isAdminRole,
                adminKey,
                {
                  nombre = ""
                  email = ""
                  adminKey = ""
                  errorMsg = null
                  showAdd = false
                },
                { err -> errorMsg = err },
              )
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Registrar Colaborador")
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.height(260.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(users) { u ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(u.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                  Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge(
                  containerColor = if (u.isAdmin) MaterialTheme.colorScheme.primary else FarmaciaColors.extended.success,
                ) {
                  Text(u.rolDisplay, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Cerrar") }
    },
  )
}
