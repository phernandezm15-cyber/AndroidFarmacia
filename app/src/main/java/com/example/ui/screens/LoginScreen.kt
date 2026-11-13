package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.PasswordStrengthLevel
import com.example.domain.PharmacyUtils
import com.example.ui.components.FarmaciaLogo
import com.example.ui.theme.FarmaciaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  isSigningIn: Boolean,
  authError: String?,
  authNotice: String?,
  onSignIn: (email: String, password: String) -> Unit,
  onRegister: (nombre: String, email: String, password: String, role: String, adminCode: String) -> Unit,
  onResetPasswordDirect: (email: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
  onClearMessages: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val invalidAdminCodeMsg = stringResource(R.string.error_admin_key)

  // 0 = Iniciar Sesión, 1 = Crear Cuenta
  var selectedTab by remember { mutableIntStateOf(0) }

  var loginEmail by remember { mutableStateOf("") }
  var loginPassword by remember { mutableStateOf("") }
  var showLoginPassword by remember { mutableStateOf(false) }

  var regNombre by remember { mutableStateOf("") }
  var regEmail by remember { mutableStateOf("") }
  var regPassword by remember { mutableStateOf("") }
  var showRegPassword by remember { mutableStateOf(false) }
  var selectedRole by remember { mutableStateOf("empleado") }
  var adminCode by remember { mutableStateOf("") }
  var showAdminCode by remember { mutableStateOf(false) }

  var localError by remember { mutableStateOf<String?>(null) }
  var showResetDialog by remember { mutableStateOf(false) }

  val passwordAnalysis = remember(regPassword) {
    PharmacyUtils.evaluatePasswordStrength(regPassword)
  }

  fun submitLogin() {
    focusManager.clearFocus()
    localError = null
    onClearMessages()
    val cleanEmail = loginEmail.trim()
    if (!PharmacyUtils.isValidEmail(cleanEmail)) {
      localError = "Ingresa un correo electrónico válido."
      return
    }
    if (loginPassword.isBlank()) {
      localError = "Ingresa tu contraseña."
      return
    }
    onSignIn(cleanEmail, loginPassword)
  }

  fun submitRegister() {
    focusManager.clearFocus()
    localError = null
    onClearMessages()
    val cleanName = regNombre.trim()
    val cleanEmail = regEmail.trim()
    if (cleanName.length < 3) {
      localError = "Ingresa tu nombre completo (mínimo 3 caracteres)."
      return
    }
    if (!PharmacyUtils.isValidEmail(cleanEmail)) {
      localError = "Ingresa un correo electrónico válido."
      return
    }
    if (!passwordAnalysis.isAllowed) {
      localError = passwordAnalysis.feedback
      return
    }
    if (selectedRole == "admin" && adminCode != PharmacyUtils.ADMIN_MASTER_PASSWORD) {
      localError = invalidAdminCodeMsg
      return
    }
    onRegister(cleanName, cleanEmail, regPassword, selectedRole, adminCode)
  }

  val displayedError = localError ?: authError

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
        .padding(horizontal = 20.dp, vertical = 24.dp),
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
              .height(148.dp),
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
                    listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
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
              text = stringResource(R.string.login_title),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              SegmentedButton(
                selected = selectedTab == 0,
                onClick = {
                  selectedTab = 0
                  localError = null
                  onClearMessages()
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.testTag("tab_login"),
              ) {
                Text("Iniciar Sesión", fontWeight = FontWeight.SemiBold)
              }
              SegmentedButton(
                selected = selectedTab == 1,
                onClick = {
                  selectedTab = 1
                  localError = null
                  onClearMessages()
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.testTag("tab_register"),
              ) {
                Text("Crear Cuenta", fontWeight = FontWeight.SemiBold)
              }
            }

            if (selectedTab == 0) {
              OutlinedTextField(
                value = loginEmail,
                onValueChange = {
                  loginEmail = it
                  localError = null
                },
                label = { Text("Correo electrónico") },
                placeholder = { Text("usuario@farmacia.com") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                singleLine = true,
                enabled = !isSigningIn,
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Email,
                  imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("login_email_input"),
              )

              OutlinedTextField(
                value = loginPassword,
                onValueChange = {
                  loginPassword = it
                  localError = null
                },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                  IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                    Icon(
                      imageVector = if (showLoginPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                      contentDescription = if (showLoginPassword) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                  }
                },
                visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isSigningIn,
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Password,
                  imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("login_password_input"),
              )
            } else {
              OutlinedTextField(
                value = regNombre,
                onValueChange = {
                  regNombre = it
                  localError = null
                },
                label = { Text("Nombre completo") },
                placeholder = { Text("Ej. María López") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                enabled = !isSigningIn,
                keyboardOptions = KeyboardOptions(
                  capitalization = KeyboardCapitalization.Words,
                  imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("register_name_input"),
              )

              OutlinedTextField(
                value = regEmail,
                onValueChange = {
                  regEmail = it
                  localError = null
                },
                label = { Text("Correo electrónico") },
                placeholder = { Text("usuario@farmacia.com") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                singleLine = true,
                enabled = !isSigningIn,
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Email,
                  imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("register_email_input"),
              )

              OutlinedTextField(
                value = regPassword,
                onValueChange = {
                  regPassword = it
                  localError = null
                },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                  IconButton(onClick = { showRegPassword = !showRegPassword }) {
                    Icon(
                      imageVector = if (showRegPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                      contentDescription = if (showRegPassword) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                  }
                },
                visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isSigningIn,
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Password,
                  imeAction = if (selectedRole == "admin") ImeAction.Next else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                  onDone = {
                    if (selectedRole != "admin") submitRegister()
                  }
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("register_password_input"),
              )

              if (regPassword.isNotEmpty()) {
                val strengthColor = when (passwordAnalysis.level) {
                  PasswordStrengthLevel.VACIA -> MaterialTheme.colorScheme.outline
                  PasswordStrengthLevel.BAJA -> MaterialTheme.colorScheme.error
                  PasswordStrengthLevel.MEDIA -> FarmaciaColors.extended.warning
                  PasswordStrengthLevel.FUERTE -> FarmaciaColors.extended.success
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  LinearProgressIndicator(
                    progress = { (passwordAnalysis.score / 3f).coerceIn(0f, 1f) },
                    color = strengthColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp),
                  )
                  Text(
                    text = "${passwordAnalysis.label} · ${passwordAnalysis.feedback}",
                    style = MaterialTheme.typography.labelSmall,
                    color = strengthColor,
                  )
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  text = "Tipo de cuenta",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  RoleOptionCard(
                    title = stringResource(R.string.role_empleado),
                    subtitle = "POS y ventas",
                    icon = Icons.Filled.Person,
                    selected = selectedRole == "empleado",
                    onClick = {
                      selectedRole = "empleado"
                      localError = null
                    },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("role_empleado_chip"),
                  )
                  RoleOptionCard(
                    title = stringResource(R.string.role_admin),
                    subtitle = "Requiere código",
                    icon = Icons.Filled.AdminPanelSettings,
                    selected = selectedRole == "admin",
                    onClick = {
                      selectedRole = "admin"
                      localError = null
                    },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("role_admin_chip"),
                  )
                }
              }

              AnimatedVisibility(visible = selectedRole == "admin") {
                OutlinedTextField(
                  value = adminCode,
                  onValueChange = {
                    adminCode = it
                    localError = null
                  },
                  label = { Text("Código de autorización Admin") },
                  placeholder = { Text("Ingresa la clave maestra") },
                  supportingText = {
                    Text("Necesario para crear cuentas con privilegios de Administrador.")
                  },
                  leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                  trailingIcon = {
                    IconButton(onClick = { showAdminCode = !showAdminCode }) {
                      Icon(
                        imageVector = if (showAdminCode) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showAdminCode) "Ocultar código" else "Mostrar código",
                      )
                    }
                  },
                  visualTransformation = if (showAdminCode) VisualTransformation.None else PasswordVisualTransformation(),
                  singleLine = true,
                  enabled = !isSigningIn,
                  keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                  ),
                  keyboardActions = KeyboardActions(onDone = { submitRegister() }),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_code_input"),
                )
              }
            }

            if (!displayedError.isNullOrBlank()) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("auth_error_banner"),
              ) {
                Text(
                  text = displayedError,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                )
              }
            }

            if (!authNotice.isNullOrBlank()) {
              Surface(
                color = FarmaciaColors.extended.successContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("auth_notice_banner"),
              ) {
                Text(
                  text = authNotice,
                  color = FarmaciaColors.extended.success,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                )
              }
            }

            Button(
              onClick = {
                if (selectedTab == 0) submitLogin() else submitRegister()
              },
              enabled = !isSigningIn,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag(if (selectedTab == 0) "login_submit_button" else "register_submit_button"),
            ) {
              if (isSigningIn) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                  text = if (selectedTab == 0) "Iniciando sesión..." else "Creando cuenta...",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
              } else {
                Text(
                  text = if (selectedTab == 0) "Iniciar Sesión" else "Crear Cuenta",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            if (selectedTab == 0) {
              TextButton(
                onClick = {
                  focusManager.clearFocus()
                  localError = null
                  onClearMessages()
                  showResetDialog = true
                },
                enabled = !isSigningIn,
                modifier = Modifier
                  .align(Alignment.CenterHorizontally)
                  .testTag("forgot_password_button"),
              ) {
                Text("¿Olvidaste tu contraseña?")
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Storage,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Conectado a base de datos SQLite local (Room)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  if (showResetDialog) {
    DirectPasswordResetDialog(
      initialEmail = loginEmail,
      onConfirmReset = onResetPasswordDirect,
      onCompleted = { updatedEmail ->
        if (updatedEmail.isNotBlank()) {
          loginEmail = updatedEmail
        }
        loginPassword = ""
        showResetDialog = false
      },
      onDismiss = { showResetDialog = false },
    )
  }
}

@Composable
private fun DirectPasswordResetDialog(
  initialEmail: String,
  onConfirmReset: (email: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
  onCompleted: (email: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var email by remember { mutableStateOf(initialEmail) }
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var showNewPass by remember { mutableStateOf(false) }
  var showConfirmPass by remember { mutableStateOf(false) }
  var isBusy by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  val strength = remember(newPassword) {
    PharmacyUtils.evaluatePasswordStrength(newPassword)
  }

  AlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Filled.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Cambiar Contraseña", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Ingresa el correo de tu cuenta y escribe tu nueva contraseña para actualizarla directamente en la base de datos.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = email,
          onValueChange = {
            email = it
            errorMsg = null
          },
          label = { Text("Correo electrónico") },
          placeholder = { Text("usuario@farmacia.com") },
          leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
          singleLine = true,
          enabled = !isBusy,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reset_email_input"),
        )
        OutlinedTextField(
          value = newPassword,
          onValueChange = {
            newPassword = it
            errorMsg = null
          },
          label = { Text("Nueva contraseña") },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { showNewPass = !showNewPass }) {
              Icon(
                imageVector = if (showNewPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (showNewPass) "Ocultar" else "Mostrar",
              )
            }
          },
          visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          enabled = !isBusy,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reset_new_password_input"),
        )
        if (newPassword.isNotEmpty()) {
          val strengthColor = when (strength.level) {
            PasswordStrengthLevel.VACIA -> MaterialTheme.colorScheme.outline
            PasswordStrengthLevel.BAJA -> MaterialTheme.colorScheme.error
            PasswordStrengthLevel.MEDIA -> FarmaciaColors.extended.warning
            PasswordStrengthLevel.FUERTE -> FarmaciaColors.extended.success
          }
          Text(
            text = "${strength.label} · ${strength.feedback}",
            style = MaterialTheme.typography.labelSmall,
            color = strengthColor,
          )
        }
        OutlinedTextField(
          value = confirmPassword,
          onValueChange = {
            confirmPassword = it
            errorMsg = null
          },
          label = { Text("Confirmar nueva contraseña") },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { showConfirmPass = !showConfirmPass }) {
              Icon(
                imageVector = if (showConfirmPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (showConfirmPass) "Ocultar" else "Mostrar",
              )
            }
          },
          visualTransformation = if (showConfirmPass) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          enabled = !isBusy,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reset_confirm_password_input"),
        )

        if (!errorMsg.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("reset_error_banner"),
          ) {
            Text(
              text = errorMsg!!,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanEmail = email.trim()
          if (!PharmacyUtils.isValidEmail(cleanEmail)) {
            errorMsg = "Ingresa un correo electrónico válido."
            return@Button
          }
          if (!strength.isAllowed) {
            errorMsg = strength.feedback
            return@Button
          }
          if (newPassword != confirmPassword) {
            errorMsg = "Las contraseñas no coinciden."
            return@Button
          }
          isBusy = true
          errorMsg = null
          onConfirmReset(
            cleanEmail,
            newPassword,
            {
              isBusy = false
              onCompleted(cleanEmail)
            },
            { err ->
              isBusy = false
              errorMsg = err
            },
          )
        },
        enabled = !isBusy,
        modifier = Modifier.testTag("confirm_password_reset_button"),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Spacer(Modifier.width(8.dp))
        }
        Text("Cambiar contraseña")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isBusy,
      ) {
        Text("Cancelar")
      }
    },
  )
}

@Composable
private fun RoleOptionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  val containerColor = if (selected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
  }
  val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = containerColor,
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(20.dp),
      )
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
