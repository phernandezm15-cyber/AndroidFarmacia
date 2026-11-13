package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ClienteEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.UserEntity
import com.example.data.repository.CartLineItem
import com.example.domain.PharmacyUtils
import com.example.domain.PharmacyVoiceMatcher
import com.example.domain.ReportExporter
import com.example.domain.SaleReceiptData
import com.example.ui.components.VoiceSearchButton
import com.example.ui.theme.FarmaciaColors
import kotlinx.coroutines.delay

@Composable
fun PosScreen(
  user: UserEntity?,
  meds: List<MedicamentoEntity>,
  clientes: List<ClienteEntity>,
  cart: List<CartLineItem>,
  discountPct: Double,
  selectedClientId: String?,
  focusMode: Boolean,
  isProcessing: Boolean,
  lastReceipt: SaleReceiptData?,
  onToggleFocus: () -> Unit,
  onAddToCart: (String) -> Unit,
  onDecrementCart: (String) -> Unit,
  onRemoveCart: (String) -> Unit,
  onDiscountChange: (Double) -> Unit,
  onClientChange: (String?) -> Unit,
  onScanCodeSubmit: (String) -> Boolean,
  onCompleteSale: (String) -> Unit,
  onClearReceipt: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val ext = FarmaciaColors.extended
  var query by remember { mutableStateOf("") }
  var barcodeInput by remember { mutableStateOf("") }
  var cashInput by remember { mutableStateOf("") }
  var showClientDropdown by remember { mutableStateOf(false) }
  var showScannerDialog by remember { mutableStateOf(false) }
  var showCartSheetOnCompact by remember { mutableStateOf(false) }

  val filteredMeds = remember(meds, query) {
    if (query.isBlank()) meds else meds.filter { PharmacyVoiceMatcher.matchMedicineFilter(it, query) }
  }
  val totals = remember(cart, discountPct) { PharmacyUtils.calcTotals(cart, discountPct) }
  val selectedClient = remember(clientes, selectedClientId) { clientes.find { it.id == selectedClientId } }
  val previewPoints = if (selectedClient != null) PharmacyUtils.puntosPor(totals.total) else 0
  val cashVal = cashInput.toDoubleOrNull() ?: 0.0
  val changeVal = cashVal - totals.total

  if (showScannerDialog) {
    LaunchedEffect(Unit) {
      delay(1600)
      val avail = meds.filter { m ->
        m.stock > (cart.find { it.medId == m.id }?.cantidad ?: 0)
      }
      if (avail.isNotEmpty()) {
        val picked = avail.random()
        onAddToCart(picked.id)
      }
      showScannerDialog = false
    }
    ScannerSimulationDialog(onCancel = { showScannerDialog = false })
  }

  if (lastReceipt != null) {
    ReceiptDialog(
      receipt = lastReceipt,
      onExportPdf = { ReportExporter.exportReceiptPdf(context, lastReceipt) },
      onDismiss = {
        cashInput = ""
        onClearReceipt()
      },
    )
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
  ) {
    val isWide = maxWidth >= 780.dp

    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Punto de Venta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
          Text(
            text = "Cajero: ${user?.nombre ?: "Usuario"} (${user?.rolDisplay ?: "Empleado"})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = onToggleFocus,
            modifier = Modifier.testTag("pos_focus_button"),
          ) {
            Icon(
              imageVector = if (focusMode) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
              contentDescription = "Alternar Modo Enfoque",
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(if (focusMode) "Salir Enfoque" else "Enfoque")
          }
          if (!isWide) {
            Button(
              onClick = { showCartSheetOnCompact = true },
              modifier = Modifier.testTag("pos_open_ticket_button"),
            ) {
              Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Ticket (${cart.sumOf { it.cantidad }})")
            }
          }
        }
      }

      Row(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Catalog column
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = query,
              onValueChange = { query = it },
              placeholder = { Text("Buscar medicamento o síntoma...") },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("pos_search_input"),
            )
            VoiceSearchButton(meds = meds, onResolvedQuery = { query = it })
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = barcodeInput,
              onValueChange = { barcodeInput = it },
              placeholder = { Text("Código de barras (ej. ${meds.firstOrNull()?.codigo ?: ""})") },
              leadingIcon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(
                onDone = {
                  if (onScanCodeSubmit(barcodeInput)) barcodeInput = ""
                }
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("pos_barcode_input"),
            )
            Button(
              onClick = { showScannerDialog = true },
              modifier = Modifier
                .height(54.dp)
                .testTag("pos_scan_button"),
            ) {
              Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código")
              Spacer(Modifier.width(6.dp))
              Text("Escanear")
            }
          }

          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(filteredMeds, key = { it.id }) { m ->
              val inCart = cart.find { it.medId == m.id }?.cantidad ?: 0
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(m.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(m.principio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text(
                        text = PharmacyUtils.q(m.precioVenta),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                      )
                      Badge(
                        containerColor = if (m.stock < 10) MaterialTheme.colorScheme.errorContainer else ext.successContainer,
                      ) {
                        Text(
                          text = "Stock: ${m.stock}",
                          color = if (m.stock < 10) MaterialTheme.colorScheme.error else ext.success,
                          modifier = Modifier.padding(horizontal = 4.dp),
                        )
                      }
                      if (inCart > 0) {
                        Text("En ticket: $inCart", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                      }
                    }
                  }
                  Button(
                    onClick = { onAddToCart(m.id) },
                    enabled = m.stock > 0,
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("pos_add_${m.id}"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                  ) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar ${m.nombre}")
                  }
                }
              }
            }
            if (filteredMeds.isEmpty()) {
              item {
                Text(
                  "Sin resultados para \"$query\"",
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            item { Spacer(Modifier.height(72.dp)) }
          }
        }

        if (isWide) {
          TicketPanel(
            clientes = clientes,
            selectedClient = selectedClient,
            previewPoints = previewPoints,
            showClientDropdown = showClientDropdown,
            onToggleClientDropdown = { showClientDropdown = it },
            onSelectClient = onClientChange,
            cart = cart,
            discountPct = discountPct,
            totals = totals,
            cashInput = cashInput,
            onCashChange = { cashInput = it },
            cashVal = cashVal,
            changeVal = changeVal,
            isProcessing = isProcessing,
            onDiscountChange = onDiscountChange,
            onIncrement = onAddToCart,
            onDecrement = onDecrementCart,
            onRemove = onRemoveCart,
            onCompleteSale = { onCompleteSale(cashInput) },
            modifier = Modifier
              .width(370.dp)
              .fillMaxHeight()
              .padding(bottom = 12.dp),
          )
        }
      }
    }

    if (!isWide && showCartSheetOnCompact) {
      AlertDialog(
        onDismissRequest = { showCartSheetOnCompact = false },
        title = { Text("Ticket de Venta", fontWeight = FontWeight.Bold) },
        text = {
          TicketPanel(
            clientes = clientes,
            selectedClient = selectedClient,
            previewPoints = previewPoints,
            showClientDropdown = showClientDropdown,
            onToggleClientDropdown = { showClientDropdown = it },
            onSelectClient = onClientChange,
            cart = cart,
            discountPct = discountPct,
            totals = totals,
            cashInput = cashInput,
            onCashChange = { cashInput = it },
            cashVal = cashVal,
            changeVal = changeVal,
            isProcessing = isProcessing,
            onDiscountChange = onDiscountChange,
            onIncrement = onAddToCart,
            onDecrement = onDecrementCart,
            onRemove = onRemoveCart,
            onCompleteSale = {
              onCompleteSale(cashInput)
              showCartSheetOnCompact = false
            },
            modifier = Modifier.fillMaxWidth(),
          )
        },
        confirmButton = {
          TextButton(onClick = { showCartSheetOnCompact = false }) { Text("Cerrar") }
        },
      )
    }
  }
}

@Composable
private fun TicketPanel(
  clientes: List<ClienteEntity>,
  selectedClient: ClienteEntity?,
  previewPoints: Int,
  showClientDropdown: Boolean,
  onToggleClientDropdown: (Boolean) -> Unit,
  onSelectClient: (String?) -> Unit,
  cart: List<CartLineItem>,
  discountPct: Double,
  totals: com.example.domain.SaleTotals,
  cashInput: String,
  onCashChange: (String) -> Unit,
  cashVal: Double,
  changeVal: Double,
  isProcessing: Boolean,
  onDiscountChange: (Double) -> Unit,
  onIncrement: (String) -> Unit,
  onDecrement: (String) -> Unit,
  onRemove: (String) -> Unit,
  onCompleteSale: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = FarmaciaColors.extended
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .padding(14.dp)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box {
        OutlinedButton(
          onClick = { onToggleClientDropdown(true) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = selectedClient?.let { "${it.nombre} (${it.puntos} pts)" } ?: "Cliente: Consumidor Final",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        DropdownMenu(
          expanded = showClientDropdown,
          onDismissRequest = { onToggleClientDropdown(false) },
        ) {
          DropdownMenuItem(
            text = { Text("Consumidor Final") },
            onClick = {
              onSelectClient(null)
              onToggleClientDropdown(false)
            },
          )
          clientes.forEach { c ->
            DropdownMenuItem(
              text = { Text("${c.nombre} · ${c.puntos} pts") },
              onClick = {
                onSelectClient(c.id)
                onToggleClientDropdown(false)
              },
            )
          }
        }
      }

      if (selectedClient != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Icon(Icons.Filled.Favorite, contentDescription = null, tint = ext.success, modifier = Modifier.size(14.dp))
          Text("Ganará +$previewPoints Puntos Salud", style = MaterialTheme.typography.labelSmall, color = ext.success)
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(145.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(cart, key = { it.medId }) { item ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(item.nombre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${PharmacyUtils.q(item.precio)} c/u", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              IconButton(onClick = { onDecrement(item.medId) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Disminuir ${item.nombre}", modifier = Modifier.size(16.dp))
              }
              Text("${item.cantidad}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
              IconButton(onClick = { onIncrement(item.medId) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar ${item.nombre}", modifier = Modifier.size(16.dp))
              }
              IconButton(onClick = { onRemove(item.medId) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Quitar ${item.nombre}", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
        if (cart.isEmpty()) {
          item {
            Text(
              "Agrega medicamentos al ticket",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 24.dp),
            )
          }
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0.0, 5.0, 10.0).forEach { pct ->
          FilterChip(
            selected = discountPct == pct,
            onClick = { onDiscountChange(pct) },
            label = { Text("${pct.toInt()}%") },
            modifier = Modifier.weight(1f),
          )
        }
      }

      SummaryLine("Subtotal", PharmacyUtils.q(totals.subtotal))
      SummaryLine("Descuento (${discountPct.toInt()}%)", "- ${PharmacyUtils.q(totals.descuento)}")
      SummaryLine("IVA (12%)", PharmacyUtils.q(totals.iva))
      HorizontalDivider()
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(PharmacyUtils.q(totals.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
      }

      OutlinedTextField(
        value = cashInput,
        onValueChange = onCashChange,
        label = { Text("Efectivo recibido (Q)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("pos_cash_input"),
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(50, 100, 200).forEach { bill ->
          OutlinedButton(
            onClick = { onCashChange(bill.toString()) },
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
          ) {
            Text("Q$bill", style = MaterialTheme.typography.labelSmall)
          }
        }
        OutlinedButton(
          onClick = { onCashChange(String.format("%.2f", totals.total)) },
          enabled = cart.isNotEmpty(),
          modifier = Modifier.weight(1f),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
        ) {
          Text("Exacto", style = MaterialTheme.typography.labelSmall)
        }
      }

      if (cashInput.isNotBlank()) {
        SummaryLine(
          label = if (changeVal < 0) "Faltante" else "Cambio a devolver",
          value = if (changeVal < 0) PharmacyUtils.q(-changeVal) else PharmacyUtils.q(changeVal),
          valueColor = if (changeVal < 0) MaterialTheme.colorScheme.error else ext.success,
        )
      }

      Button(
        onClick = onCompleteSale,
        enabled = cart.isNotEmpty() && !isProcessing,
        colors = ButtonDefaults.buttonColors(containerColor = ext.success, contentColor = ext.onSuccess),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("pos_complete_sale_button"),
      ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Completar Venta", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun SummaryLine(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
  }
}

@Composable
private fun ScannerSimulationDialog(onCancel: () -> Unit) {
  val inf = rememberInfiniteTransition(label = "laser")
  val yFrac by inf.animateFloat(
    initialValue = 0.15f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
    label = "laserY",
  )

  AlertDialog(
    onDismissRequest = onCancel,
    title = { Text("Escaneando código...", fontWeight = FontWeight.Bold) },
    text = {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .size(190.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(18.dp)),
        ) {
          Icon(
            imageVector = Icons.Filled.QrCodeScanner,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
              .size(88.dp)
              .align(Alignment.Center),
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .padding(top = (yFrac * 170).dp)
              .height(3.dp)
              .background(Color(0xFF34D399)),
          )
        }
        Text("Detectando código de barras del catálogo...", style = MaterialTheme.typography.bodySmall)
      }
    },
    confirmButton = {
      TextButton(onClick = onCancel) { Text("Cancelar") }
    },
  )
}

@Composable
private fun ReceiptDialog(
  receipt: SaleReceiptData,
  onExportPdf: () -> Unit,
  onDismiss: () -> Unit,
) {
  val ext = FarmaciaColors.extended
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Recibo ${receipt.codigo}", fontWeight = FontWeight.Bold) },
    text = {
      Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text("FarmaciaDB · NIT 1234567-8", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          Text(PharmacyUtils.fmtFechaHora(receipt.fechaMillis), style = MaterialTheme.typography.labelSmall)
          Text("Atendió: ${receipt.vendedor}", style = MaterialTheme.typography.labelSmall)
          if (!receipt.cliente.isNullOrBlank()) {
            Text("Cliente: ${receipt.cliente}", style = MaterialTheme.typography.labelSmall)
          }
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          receipt.items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${item.cantidad}x ${item.nombre}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
              Text(PharmacyUtils.q(item.precio * item.cantidad), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
          }
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          SummaryLine("Subtotal", PharmacyUtils.q(receipt.subtotal))
          SummaryLine("Descuento (${receipt.descPct.toInt()}%)", "- ${PharmacyUtils.q(receipt.descuento)}")
          SummaryLine("IVA (12%)", PharmacyUtils.q(receipt.iva))
          SummaryLine("TOTAL", PharmacyUtils.q(receipt.total), MaterialTheme.colorScheme.primary)
          SummaryLine("Efectivo", PharmacyUtils.q(receipt.efectivo))
          SummaryLine("Cambio", PharmacyUtils.q(receipt.cambio), ext.success)
          if (receipt.puntos > 0) {
            Text(
              "Puntos ganados: +${receipt.puntos} Puntos Salud",
              color = ext.success,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onExportPdf, modifier = Modifier.testTag("receipt_pdf_button")) {
        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("PDF / Compartir")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) { Text("Cerrar") }
    },
  )
}
