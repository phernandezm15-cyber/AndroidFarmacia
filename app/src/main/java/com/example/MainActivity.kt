package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.UserEntity
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.FarmaciaRepository
import com.example.data.repository.FarmaciaRepositoryProvider
import com.example.domain.FirebaseAiRepository
import com.example.ui.FarmaciaViewModel
import com.example.ui.components.ChatbotSheet
import com.example.ui.components.FarmaciaLogo
import com.example.ui.components.NotificationsSheet
import com.example.ui.components.RoleDetailsDialog
import com.example.ui.components.UsersManagementDialog
import com.example.ui.components.configureAppCheck
import com.example.ui.screens.ClientesScreen
import com.example.ui.screens.ComprasScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventarioScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ManualesScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ProveedoresScreen
import com.example.ui.screens.ReportesScreen
import com.example.ui.theme.FarmaciaTheme
import kotlinx.coroutines.launch

sealed class FarmaciaRoute(val route: String, val label: String, val icon: ImageVector) {
  data object Dashboard : FarmaciaRoute("dashboard", "Inicio", Icons.Filled.Dashboard)
  data object Pos : FarmaciaRoute("pos", "POS", Icons.Filled.PointOfSale)
  data object Inventario : FarmaciaRoute("inventario", "Inventario", Icons.Filled.Inventory2)
  data object Clientes : FarmaciaRoute("clientes", "Clientes", Icons.Filled.People)
  data object Proveedores : FarmaciaRoute("proveedores", "Proveedores", Icons.Filled.LocalShipping)
  data object Compras : FarmaciaRoute("compras", "Compras", Icons.Filled.ShoppingBag)
  data object Reportes : FarmaciaRoute("reportes", "Reportes", Icons.Filled.Assessment)
  data object Manuales : FarmaciaRoute("manuales", "Manuales", Icons.AutoMirrored.Filled.MenuBook)
}

private val ALL_ROUTES = listOf(
  FarmaciaRoute.Dashboard,
  FarmaciaRoute.Pos,
  FarmaciaRoute.Inventario,
  FarmaciaRoute.Clientes,
  FarmaciaRoute.Proveedores,
  FarmaciaRoute.Compras,
  FarmaciaRoute.Reportes,
  FarmaciaRoute.Manuales,
)

private val COMPACT_PRIMARY_ROUTES = listOf(
  FarmaciaRoute.Dashboard,
  FarmaciaRoute.Pos,
  FarmaciaRoute.Inventario,
  FarmaciaRoute.Compras,
)

private val COMPACT_OVERFLOW_ROUTES = listOf(
  FarmaciaRoute.Clientes,
  FarmaciaRoute.Proveedores,
  FarmaciaRoute.Reportes,
  FarmaciaRoute.Manuales,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    runCatching { enableEdgeToEdge() }
      .onFailure { Log.w("MainActivity", "enableEdgeToEdge failed", it) }
    setContent {
      FarmaciaAppRoot()
    }
  }
}

@Composable
fun FarmaciaAppRoot(repositoryOverride: FarmaciaRepository? = null) {
  val context = LocalContext.current
  val appContext = context.applicationContext ?: context
  val prefs = remember(appContext) { SessionPreferences(appContext) }
  val repo = remember(appContext, repositoryOverride) {
    repositoryOverride ?: FarmaciaRepositoryProvider.get(appContext)
  }
  val darkMode by prefs.darkModeFlow.collectAsStateWithLifecycle(initialValue = false)
  val loggedUid by prefs.loggedUserIdFlow.collectAsStateWithLifecycle(initialValue = null)
  val scope = rememberCoroutineScope()

  var activeUser by remember { mutableStateOf<UserEntity?>(null) }
  var isSigningIn by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }
  var authNotice by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(loggedUid) {
    val uid = loggedUid
    if (uid.isNullOrBlank()) {
      activeUser = null
    } else {
      val found = repo.findUserById(uid)
      if (found == null) {
        prefs.setLoggedUserId(null)
        activeUser = null
      } else {
        activeUser = found
      }
    }
  }

  FarmaciaTheme(darkTheme = darkMode) {
    val user = activeUser
    if (user == null) {
      val snackbarHostState = remember { SnackbarHostState() }
      Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
      ) { innerPadding ->
        LoginScreen(
          isSigningIn = isSigningIn,
          isCloudSupabase = repo.isCloudBackend,
          authError = authError,
          authNotice = authNotice,
          onClearMessages = {
            authError = null
            authNotice = null
          },
          onSignIn = { email, password ->
            scope.launch {
              isSigningIn = true
              authError = null
              authNotice = null
              repo.loginUser(email, password).fold(
                onSuccess = { logged ->
                  prefs.setLoggedUserId(logged.uid)
                  activeUser = logged
                },
                onFailure = { err ->
                  authError = err.localizedMessage ?: "No se pudo iniciar sesión."
                },
              )
              isSigningIn = false
            }
          },
          onRegister = { nombre, email, password, role, adminCode ->
            scope.launch {
              isSigningIn = true
              authError = null
              authNotice = null
              repo.registerUser(nombre, email, password, role, adminCode).fold(
                onSuccess = { created ->
                  prefs.setLoggedUserId(created.uid)
                  activeUser = created
                },
                onFailure = { err ->
                  authError = err.localizedMessage ?: "No se pudo crear la cuenta."
                },
              )
              isSigningIn = false
            }
          },
          onResetPasswordDirect = { email, newPassword, onSuccess, onError ->
            scope.launch {
              repo.resetPasswordDirect(email, newPassword).fold(
                onSuccess = {
                  authError = null
                  authNotice = "Contraseña actualizada en la base de datos. Ya puedes iniciar sesión."
                  onSuccess()
                },
                onFailure = { err ->
                  onError(err.localizedMessage ?: "No se pudo actualizar la contraseña.")
                },
              )
            }
          },
          modifier = Modifier.padding(innerPadding),
        )
      }
    } else {
      val vm: FarmaciaViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) {
              "APPLICATION_KEY missing from CreationExtras"
            }
            val vmRepo = repositoryOverride ?: FarmaciaRepositoryProvider.get(app)
            FarmaciaViewModel(
              repo = vmRepo,
              aiRepo = FirebaseAiRepository(),
              currentUserId = user.uid,
              initialDisplayName = user.nombre,
            )
          }
        },
      )

      AuthenticatedPharmacyShell(
        vm = vm,
        darkMode = darkMode,
        hasPasswordAuth = true,
        onChangePassword = { currentPassword, newPassword, onSuccess, onError ->
          scope.launch {
            repo.changePasswordLoggedIn(user.uid, currentPassword, newPassword).fold(
              onSuccess = { onSuccess() },
              onFailure = { err -> onError(err.localizedMessage ?: "No se pudo cambiar la contraseña.") },
            )
          }
        },
        onToggleDarkMode = {
          scope.launch { prefs.setDarkMode(!darkMode) }
        },
        onLogout = {
          scope.launch {
            prefs.setLoggedUserId(null)
            activeUser = null
          }
        },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedPharmacyShell(
  vm: FarmaciaViewModel,
  darkMode: Boolean,
  hasPasswordAuth: Boolean,
  onChangePassword: (
    currentPassword: String,
    newPassword: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) -> Unit,
  onToggleDarkMode: () -> Unit,
  onLogout: () -> Unit,
) {
  val isInitializing by vm.isInitializing.collectAsStateWithLifecycle()
  val workspaceError by vm.workspaceError.collectAsStateWithLifecycle()
  val currentUser by vm.currentUser.collectAsStateWithLifecycle()
  val meds by vm.medicamentos.collectAsStateWithLifecycle()
  val proveedores by vm.proveedores.collectAsStateWithLifecycle()
  val clientes by vm.clientes.collectAsStateWithLifecycle()
  val compras by vm.compras.collectAsStateWithLifecycle()
  val ventas by vm.ventas.collectAsStateWithLifecycle()
  val users by vm.users.collectAsStateWithLifecycle()
  val alertCount by vm.alertCount.collectAsStateWithLifecycle()
  val focusMode by vm.focusMode.collectAsStateWithLifecycle()
  val cart by vm.cart.collectAsStateWithLifecycle()
  val discountPct by vm.discountPct.collectAsStateWithLifecycle()
  val selectedClientId by vm.selectedClientId.collectAsStateWithLifecycle()
  val lastReceipt by vm.lastReceipt.collectAsStateWithLifecycle()
  val isProcessingSale by vm.isProcessingSale.collectAsStateWithLifecycle()
  val chatMessages by vm.chatMessages.collectAsStateWithLifecycle()
  val isBotThinking by vm.isBotThinking.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    vm.toasts.collect { toast ->
      snackbarHostState.showSnackbar(toast.message)
    }
  }

  if (isInitializing && currentUser == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FarmaciaLogo()
        CircularProgressIndicator()
        Text("Cargando base de datos local...", style = MaterialTheme.typography.bodySmall)
      }
    }
    return
  }

  if (workspaceError != null && currentUser == null) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FarmaciaLogo()
        Text(
          text = workspaceError ?: "Error de acceso a la base de datos",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
          textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(onClick = { vm.initializeWorkspace() }) {
            Text("Reintentar")
          }
          OutlinedButton(onClick = onLogout) {
            Text("Cerrar sesión")
          }
        }
      }
    }
    return
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: FarmaciaRoute.Dashboard.route

  var showMoreMenu by remember { mutableStateOf(false) }
  var showRoleDialog by remember { mutableStateOf(false) }
  var showUsersDialog by remember { mutableStateOf(false) }
  var showNotificationsSheet by remember { mutableStateOf(false) }
  var showChatbotSheet by remember { mutableStateOf(false) }

  fun navigateTo(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 720.dp

    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        if (!focusMode) {
          TopAppBar(
            title = { FarmaciaLogo(compact = true) },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
            actions = {
              val user = currentUser
              if (user != null) {
                Surface(
                  onClick = { showRoleDialog = true },
                  shape = RoundedCornerShape(999.dp),
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                  modifier = Modifier.testTag("topbar_role_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = if (user.isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.Person,
                      contentDescription = "Ver rol y permisos",
                      modifier = Modifier.size(16.dp),
                      tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      text = if (user.isAdmin) "Admin" else "Empleado",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
                if (user.isAdmin) {
                  IconButton(
                    onClick = { showUsersDialog = true },
                    modifier = Modifier.testTag("topbar_users_button"),
                  ) {
                    Icon(Icons.Filled.Group, contentDescription = "Personal y Roles")
                  }
                }
              }
              IconButton(
                onClick = { showNotificationsSheet = true },
                modifier = Modifier.testTag("topbar_notifications_button"),
              ) {
                BadgedBox(
                  badge = {
                    if (alertCount > 0) {
                      Badge { Text(alertCount.toString()) }
                    }
                  }
                ) {
                  Icon(Icons.Filled.Notifications, contentDescription = "Alertas del sistema")
                }
              }
              if (vm.isCloudBackend) {
                IconButton(
                  onClick = { vm.refreshCloudData() },
                  modifier = Modifier.testTag("topbar_sync_button"),
                ) {
                  Icon(Icons.Filled.Sync, contentDescription = "Sincronizar con Supabase")
                }
              }
              IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier.testTag("topbar_theme_button"),
              ) {
                Icon(
                  imageVector = if (darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                  contentDescription = if (darkMode) "Modo claro" else "Modo oscuro",
                )
              }
              IconButton(
                onClick = onLogout,
                modifier = Modifier.testTag("topbar_logout_button"),
              ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
              }
            },
          )
        }
      },
      bottomBar = {
        if (!useRail && !focusMode) {
          NavigationBar {
            COMPACT_PRIMARY_ROUTES.forEach { dest ->
              NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = { navigateTo(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label, maxLines = 1) },
                modifier = Modifier.testTag("nav_${dest.route}"),
              )
            }
            val isOverflowSelected = COMPACT_OVERFLOW_ROUTES.any { it.route == currentRoute }
            NavigationBarItem(
              selected = isOverflowSelected,
              onClick = { showMoreMenu = true },
              icon = {
                Box {
                  Icon(Icons.Filled.MoreHoriz, contentDescription = "Más módulos")
                  DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                  ) {
                    COMPACT_OVERFLOW_ROUTES.forEach { extra ->
                      DropdownMenuItem(
                        text = { Text(extra.label) },
                        leadingIcon = { Icon(extra.icon, contentDescription = null) },
                        onClick = {
                          showMoreMenu = false
                          navigateTo(extra.route)
                        },
                      )
                    }
                  }
                }
              },
              label = { Text("Más", maxLines = 1) },
              modifier = Modifier.testTag("nav_more"),
            )
          }
        }
      },
      floatingActionButton = {
        if (!focusMode) {
          ExtendedFloatingActionButton(
            onClick = { showChatbotSheet = true },
            icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "Abrir FarmaIA") },
            text = { Text("FarmaIA") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("chatbot_fab"),
          )
        } else {
          FloatingActionButton(
            onClick = { showChatbotSheet = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = "FarmaIA")
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useRail && !focusMode) {
          NavigationRail(
            modifier = Modifier
              .fillMaxHeight()
              .verticalScroll(rememberScrollState()),
          ) {
            ALL_ROUTES.forEach { dest ->
              NavigationRailItem(
                selected = currentRoute == dest.route,
                onClick = { navigateTo(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
                modifier = Modifier.testTag("rail_${dest.route}"),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = FarmaciaRoute.Dashboard.route,
          modifier = Modifier.weight(1f),
        ) {
          composable(FarmaciaRoute.Dashboard.route) {
            DashboardScreen(
              user = currentUser,
              meds = meds,
              ventas = ventas,
              proveedores = proveedores,
              compras = compras,
              onOpenManuales = { navigateTo(FarmaciaRoute.Manuales.route) },
              onGenerateRestockOrder = { medId, supplierId, cost ->
                vm.registrarCompra(
                  proveedorId = supplierId,
                  medId = medId,
                  cantidad = 50,
                  costo = cost,
                  estado = "Pendiente",
                  nota = "Generada por predicción IA",
                )
              },
            )
          }
          composable(FarmaciaRoute.Pos.route) {
            PosScreen(
              user = currentUser,
              meds = meds,
              clientes = clientes,
              cart = cart,
              discountPct = discountPct,
              selectedClientId = selectedClientId,
              focusMode = focusMode,
              isProcessing = isProcessingSale,
              lastReceipt = lastReceipt,
              onToggleFocus = { vm.setFocusMode(!focusMode) },
              onAddToCart = { vm.addToCart(it) },
              onDecrementCart = { vm.decrementFromCart(it) },
              onRemoveCart = { vm.removeFromCart(it) },
              onDiscountChange = { vm.setDiscountPct(it) },
              onClientChange = { vm.setSelectedClientId(it) },
              onScanCodeSubmit = { vm.addByBarcodeOrId(it) },
              onCompleteSale = { vm.completeSale(it) },
              onClearReceipt = { vm.clearReceipt() },
            )
          }
          composable(FarmaciaRoute.Inventario.route) {
            InventarioScreen(
              user = currentUser,
              meds = meds,
              onSaveMed = { med, onDone -> vm.saveMedicamento(med, onDone) },
              onDeleteMed = { vm.deleteMedicamento(it) },
              onRestrictedNotice = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            )
          }
          composable(FarmaciaRoute.Clientes.route) {
            ClientesScreen(
              clientes = clientes,
              onSaveCliente = { c, onDone -> vm.saveCliente(c, onDone) },
              onDeleteCliente = { vm.deleteCliente(it) },
            )
          }
          composable(FarmaciaRoute.Proveedores.route) {
            ProveedoresScreen(
              user = currentUser,
              proveedores = proveedores,
              onSaveProveedor = { p, onDone -> vm.saveProveedor(p, onDone) },
              onDeleteProveedor = { vm.deleteProveedor(it) },
              onRestrictedNotice = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            )
          }
          composable(FarmaciaRoute.Compras.route) {
            ComprasScreen(
              user = currentUser,
              meds = meds,
              proveedores = proveedores,
              compras = compras,
              onRegisterPurchase = { provId, medId, cant, cost, est, onDone ->
                vm.registrarCompra(provId, medId, cant, cost, est, null, onDone)
              },
              onReceivePurchase = { vm.recibirCompra(it) },
              onRestrictedNotice = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            )
          }
          composable(FarmaciaRoute.Reportes.route) {
            ReportesScreen(
              user = currentUser,
              ventas = ventas,
              meds = meds,
              onNotify = { msg, _ -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            )
          }
          composable(FarmaciaRoute.Manuales.route) {
            ManualesScreen(
              onNotify = { msg, _ -> scope.launch { snackbarHostState.showSnackbar(msg) } },
            )
          }
        }
      }
    }
  }

  if (showRoleDialog && currentUser != null) {
    RoleDetailsDialog(
      user = currentUser!!,
      hasPasswordAuth = hasPasswordAuth,
      onChangePassword = onChangePassword,
      onDismiss = { showRoleDialog = false },
    )
  }

  if (showUsersDialog && currentUser?.isAdmin == true) {
    UsersManagementDialog(
      users = users,
      onRegisterUser = { nombre, email, isAdminRole, key, onSuccess, onError ->
        vm.addStaffMember(nombre, email, isAdminRole, key, onSuccess, onError)
      },
      onDismiss = { showUsersDialog = false },
    )
  }

  if (showNotificationsSheet) {
    NotificationsSheet(
      meds = meds,
      compras = compras,
      onDismiss = { showNotificationsSheet = false },
      onNavigateInventario = { navigateTo(FarmaciaRoute.Inventario.route) },
      onNavigateCompras = { navigateTo(FarmaciaRoute.Compras.route) },
    )
  }

  if (showChatbotSheet) {
    ChatbotSheet(
      messages = chatMessages,
      isThinking = isBotThinking,
      isPlaceholderMode = vm.isAiPlaceholderMode,
      onSend = { vm.sendChatMessage(it) },
      onClear = { vm.clearChat() },
      onDismiss = { showChatbotSheet = false },
    )
  }
}
