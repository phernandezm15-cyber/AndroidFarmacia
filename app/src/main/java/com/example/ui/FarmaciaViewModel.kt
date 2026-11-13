package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClienteEntity
import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.data.local.VentaWithItems
import com.example.data.repository.CartLineItem
import com.example.data.repository.FarmaciaRepository
import com.example.domain.AiRepository
import com.example.domain.FarmaciaAiContext
import com.example.domain.PharmacyUtils
import com.example.domain.SaleReceiptData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
)

data class AppToast(
  val message: String,
  val isError: Boolean = false,
)

class FarmaciaViewModel(
  private val repo: FarmaciaRepository,
  private val aiRepo: AiRepository,
  private val currentUserId: String,
  private val initialDisplayName: String,
) : ViewModel() {

  private val _isInitializing = MutableStateFlow(true)
  val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

  private val _workspaceError = MutableStateFlow<String?>(null)
  val workspaceError: StateFlow<String?> = _workspaceError.asStateFlow()

  val currentUser: StateFlow<UserEntity?> = repo.observeUserProfile(currentUserId)
    .safeCatch(null, "Error observando perfil")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null,
    )

  val medicamentos: StateFlow<List<MedicamentoEntity>> = repo.observeMedicamentos()
    .safeCatch(emptyList(), "Error observando inventario")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val proveedores: StateFlow<List<ProveedorEntity>> = repo.observeProveedores()
    .safeCatch(emptyList(), "Error observando proveedores")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val clientes: StateFlow<List<ClienteEntity>> = repo.observeClientes()
    .safeCatch(emptyList(), "Error observando clientes")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val compras: StateFlow<List<CompraEntity>> = repo.observeCompras()
    .safeCatch(emptyList(), "Error observando compras")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val ventas: StateFlow<List<VentaWithItems>> = repo.observeVentas()
    .safeCatch(emptyList(), "Error observando ventas")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val users: StateFlow<List<UserEntity>> = repo.observeStaff()
    .safeCatch(emptyList(), "Error observando personal")
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val alertCount: StateFlow<Int> = combine(medicamentos, compras) { meds, comp ->
    val stockOrExpiry = meds.count { it.stock < 10 || PharmacyUtils.diasParaVencer(it.caducidad) <= 30 }
    val pendingOrders = comp.count { it.estado == "Pendiente" }
    stockOrExpiry + pendingOrders
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

  private val _focusMode = MutableStateFlow(false)
  val focusMode: StateFlow<Boolean> = _focusMode.asStateFlow()

  private val _cart = MutableStateFlow<List<CartLineItem>>(emptyList())
  val cart: StateFlow<List<CartLineItem>> = _cart.asStateFlow()

  private val _discountPct = MutableStateFlow(0.0)
  val discountPct: StateFlow<Double> = _discountPct.asStateFlow()

  private val _selectedClientId = MutableStateFlow<String?>(null)
  val selectedClientId: StateFlow<String?> = _selectedClientId.asStateFlow()

  private val _lastReceipt = MutableStateFlow<SaleReceiptData?>(null)
  val lastReceipt: StateFlow<SaleReceiptData?> = _lastReceipt.asStateFlow()

  private val _isProcessingSale = MutableStateFlow(false)
  val isProcessingSale: StateFlow<Boolean> = _isProcessingSale.asStateFlow()

  private val _chatMessages = MutableStateFlow(
    listOf(
      ChatMessage(
        id = "welcome",
        isUser = false,
        text = "👋 ¡Hola! Soy **FarmaIA**, tu asistente farmacéutico impulsado por **Gemini**.\n\nPregúntame sobre síntomas, monografías de medicamentos, interacciones o el estado en vivo de tu inventario y ventas en la base de datos.",
      )
    )
  )
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  private val _isBotThinking = MutableStateFlow(false)
  val isBotThinking: StateFlow<Boolean> = _isBotThinking.asStateFlow()

  val isAiPlaceholderMode: Boolean = aiRepo.isPlaceholderLocalMode

  private val _toasts = MutableSharedFlow<AppToast>(extraBufferCapacity = 8)
  val toasts = _toasts.asSharedFlow()

  init {
    initializeWorkspace()
  }

  fun initializeWorkspace() {
    viewModelScope.launch {
      _isInitializing.value = true
      _workspaceError.value = null
      repo.ensureWorkspaceSeeded(currentUserId, initialDisplayName).onFailure { err ->
        val msg = err.localizedMessage ?: "No se pudo inicializar la base de datos local"
        _workspaceError.value = msg
        notify(msg, isError = true)
      }
      _isInitializing.value = false
    }
  }

  private fun <T> Flow<T>.safeCatch(fallback: T, logMsg: String): Flow<T> = catch { error ->
    if (error is CancellationException) throw error
    Log.w(TAG, logMsg, error)
    notify(error.localizedMessage ?: logMsg, isError = true)
    emit(fallback)
  }

  private fun notify(message: String, isError: Boolean = false) {
    _toasts.tryEmit(AppToast(message, isError))
  }

  fun setFocusMode(enabled: Boolean) {
    _focusMode.value = enabled
  }

  fun addStaffMember(
    nombre: String,
    email: String,
    isAdminRole: Boolean,
    adminAuthKey: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
  ) {
    if (nombre.trim().length < 3) {
      onFailure("Mínimo 3 caracteres para el nombre")
      return
    }
    if (!PharmacyUtils.isValidEmail(email)) {
      onFailure("Correo electrónico inválido")
      return
    }
    if (isAdminRole && adminAuthKey != PharmacyUtils.ADMIN_MASTER_PASSWORD) {
      onFailure("Clave de autorización incorrecta para asignar rol Administrador")
      return
    }
    viewModelScope.launch {
      val role = if (isAdminRole) "admin" else "empleado"
      repo.addStaffMember(nombre, email, role).fold(
        onSuccess = { member ->
          notify("Colaborador guardado: ${member.nombre} (clave inicial: ${FarmaciaRepository.DEFAULT_STAFF_PASSWORD})")
          onSuccess()
        },
        onFailure = { err ->
          onFailure(err.localizedMessage ?: "No se pudo registrar colaborador")
        },
      )
    }
  }

  fun addToCart(medId: String): Boolean {
    val med = medicamentos.value.find { it.id == medId } ?: return false
    val current = _cart.value.find { it.medId == medId }?.cantidad ?: 0
    if (current + 1 > med.stock) {
      notify("Stock insuficiente para ${med.nombre} (${med.stock} disp.)", isError = true)
      return false
    }
    _cart.value = if (current > 0) {
      _cart.value.map { if (it.medId == medId) it.copy(cantidad = it.cantidad + 1) else it }
    } else {
      _cart.value + CartLineItem(med.id, med.nombre, med.precioVenta, 1)
    }
    return true
  }

  fun decrementFromCart(medId: String) {
    _cart.value = _cart.value
      .map { if (it.medId == medId) it.copy(cantidad = it.cantidad - 1) else it }
      .filter { it.cantidad > 0 }
  }

  fun removeFromCart(medId: String) {
    _cart.value = _cart.value.filterNot { it.medId == medId }
  }

  fun setDiscountPct(pct: Double) {
    _discountPct.value = pct
  }

  fun setSelectedClientId(clientId: String?) {
    _selectedClientId.value = clientId
  }

  fun addByBarcodeOrId(codeInput: String): Boolean {
    val q = codeInput.trim().lowercase()
    if (q.isEmpty()) return false
    val med = medicamentos.value.find { it.codigo.lowercase() == q || it.id.lowercase() == q }
    if (med == null) {
      notify("Código no encontrado: $codeInput", isError = true)
      return false
    }
    if (addToCart(med.id)) {
      notify("${med.nombre} agregado al ticket")
      return true
    }
    return false
  }

  fun clearReceipt() {
    _lastReceipt.value = null
  }

  fun completeSale(cashInput: String) {
    val user = currentUser.value ?: UserEntity(
      uid = currentUserId,
      email = "",
      nombre = initialDisplayName.ifBlank { "Usuario" },
      role = "empleado",
    )
    val items = _cart.value
    if (items.isEmpty()) {
      notify("El ticket está vacío", isError = true)
      return
    }
    val totals = PharmacyUtils.calcTotals(items, _discountPct.value)
    val parsedCash = cashInput.toDoubleOrNull()
    if (cashInput.isNotBlank() && (parsedCash == null || parsedCash < totals.total - 0.001)) {
      val missing = PharmacyUtils.q(totals.total - (parsedCash ?: 0.0))
      notify("Efectivo insuficiente (faltan $missing)", isError = true)
      return
    }

    val clientId = _selectedClientId.value
    val clientName = clientes.value.find { it.id == clientId }?.nombre

    viewModelScope.launch {
      _isProcessingSale.value = true
      repo.registrarVenta(
        items = items,
        descPct = _discountPct.value,
        vendedor = user,
        clienteId = clientId,
        currentSalesCount = ventas.value.size,
      ).fold(
        onSuccess = { saved ->
          val paid = if (cashInput.isNotBlank() && parsedCash != null) parsedCash else saved.venta.total
          _lastReceipt.value = SaleReceiptData(
            codigo = saved.venta.codigo,
            fechaMillis = saved.venta.fecha,
            vendedor = saved.venta.vendedorNombre,
            cliente = clientName,
            items = items,
            subtotal = saved.venta.subtotal,
            descuento = saved.venta.descuento,
            descPct = saved.venta.descPct,
            iva = saved.venta.iva,
            total = saved.venta.total,
            efectivo = paid,
            cambio = paid - saved.venta.total,
            puntos = saved.venta.puntos,
          )
          _cart.value = emptyList()
          _discountPct.value = 0.0
          val ptsMsg = if (saved.venta.puntos > 0) " · +${saved.venta.puntos} Puntos Salud" else ""
          notify("Venta ${saved.venta.codigo} guardada (${PharmacyUtils.q(saved.venta.total)})$ptsMsg")
        },
        onFailure = { err ->
          notify(err.localizedMessage ?: "Error al registrar venta", isError = true)
        },
      )
      _isProcessingSale.value = false
    }
  }

  fun saveMedicamento(med: MedicamentoEntity, onDone: () -> Unit) {
    viewModelScope.launch {
      val isNew = med.id.isBlank()
      repo.saveMedicamento(med, medicamentos.value.size).fold(
        onSuccess = {
          notify(if (isNew) "Producto guardado: ${med.nombre}" else "Producto actualizado: ${med.nombre}")
          onDone()
        },
        onFailure = { err ->
          notify(err.localizedMessage ?: "No se pudo guardar el producto", isError = true)
        },
      )
    }
  }

  fun deleteMedicamento(med: MedicamentoEntity) {
    viewModelScope.launch {
      repo.deleteMedicamento(med.id).fold(
        onSuccess = { notify("Producto eliminado: ${med.nombre}") },
        onFailure = { err -> notify(err.localizedMessage ?: "No se pudo eliminar", isError = true) },
      )
    }
  }

  fun saveCliente(cliente: ClienteEntity, onDone: () -> Unit) {
    viewModelScope.launch {
      val isNew = cliente.id.isBlank()
      repo.saveCliente(cliente, clientes.value.size).fold(
        onSuccess = {
          notify(if (isNew) "Cliente registrado: ${cliente.nombre}" else "Cliente actualizado: ${cliente.nombre}")
          onDone()
        },
        onFailure = { err ->
          notify(err.localizedMessage ?: "No se pudo guardar el cliente", isError = true)
        },
      )
    }
  }

  fun deleteCliente(cliente: ClienteEntity) {
    viewModelScope.launch {
      repo.deleteCliente(cliente.id).fold(
        onSuccess = { notify("Cliente eliminado: ${cliente.nombre}") },
        onFailure = { err -> notify(err.localizedMessage ?: "No se pudo eliminar cliente", isError = true) },
      )
    }
  }

  fun saveProveedor(prov: ProveedorEntity, onDone: () -> Unit) {
    viewModelScope.launch {
      val isNew = prov.id.isBlank()
      repo.saveProveedor(prov, proveedores.value.size).fold(
        onSuccess = {
          notify(if (isNew) "Proveedor registrado: ${prov.empresa}" else "Proveedor actualizado: ${prov.empresa}")
          onDone()
        },
        onFailure = { err ->
          notify(err.localizedMessage ?: "No se pudo guardar proveedor", isError = true)
        },
      )
    }
  }

  fun deleteProveedor(prov: ProveedorEntity) {
    viewModelScope.launch {
      repo.deleteProveedor(prov.id).fold(
        onSuccess = { notify("Proveedor eliminado: ${prov.empresa}") },
        onFailure = { err -> notify(err.localizedMessage ?: "No se pudo eliminar proveedor", isError = true) },
      )
    }
  }

  fun registrarCompra(
    proveedorId: String,
    medId: String,
    cantidad: Int,
    costo: Double,
    estado: String,
    nota: String? = null,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val medName = medicamentos.value.find { it.id == medId }?.nombre ?: "Producto"
      repo.registrarCompra(proveedorId, medId, cantidad, costo, estado, compras.value.size, nota).fold(
        onSuccess = {
          notify(
            if (estado == "Completado") "Compra registrada: +$cantidad uds. de $medName"
            else "Orden pendiente creada: $cantidad uds. de $medName"
          )
          onDone()
        },
        onFailure = { err ->
          notify(err.localizedMessage ?: "Error al registrar compra", isError = true)
        },
      )
    }
  }

  fun recibirCompra(compra: CompraEntity) {
    viewModelScope.launch {
      repo.recibirCompra(compra.id).fold(
        onSuccess = { ok ->
          if (ok) {
            val medName = medicamentos.value.find { it.id == compra.medId }?.nombre ?: compra.medId
            notify("Mercadería recibida: +${compra.cantidad} $medName")
          }
        },
        onFailure = { err ->
          notify(err.localizedMessage ?: "No se pudo recibir la compra", isError = true)
        },
      )
    }
  }

  fun sendChatMessage(rawQuery: String) {
    val clean = rawQuery.trim()
    if (clean.isEmpty()) return
    val userMsg = ChatMessage(
      id = "u-${System.currentTimeMillis()}",
      isUser = true,
      text = clean,
    )
    _chatMessages.value = _chatMessages.value + userMsg
    _isBotThinking.value = true

    viewModelScope.launch {
      try {
        val reply = aiRepo.askPharmacyAssistant(
          query = clean,
          context = FarmaciaAiContext(meds = medicamentos.value, ventas = ventas.value),
        )
        _chatMessages.value = _chatMessages.value + ChatMessage(
          id = "b-${System.currentTimeMillis()}",
          isUser = false,
          text = reply,
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Gemini error", e)
        _chatMessages.value = _chatMessages.value + ChatMessage(
          id = "err-${System.currentTimeMillis()}",
          isUser = false,
          text = "⚠️ No se pudo conectar con Gemini en este momento: ${e.localizedMessage ?: "verifica tu conexión"}",
        )
      } finally {
        _isBotThinking.value = false
      }
    }
  }

  fun clearChat() {
    _chatMessages.value = listOf(
      ChatMessage(
        id = "welcome-${System.currentTimeMillis()}",
        isUser = false,
        text = "👋 Conversación reiniciada. ¿En qué consulta clínica o de inventario te ayudo?",
      )
    )
  }

  private companion object {
    const val TAG = "FarmaciaVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
