package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.domain.PharmacyUtils
import com.example.ui.components.RoleBannerCard
import com.example.ui.theme.FarmaciaColors

@Composable
fun ComprasScreen(
  user: UserEntity?,
  meds: List<MedicamentoEntity>,
  proveedores: List<ProveedorEntity>,
  compras: List<CompraEntity>,
  onRegisterPurchase: (
    proveedorId: String,
    medId: String,
    cantidad: Int,
    costo: Double,
    estado: String,
    onDone: () -> Unit,
  ) -> Unit,
  onReceivePurchase: (CompraEntity) -> Unit,
  onRestrictedNotice: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isAdmin = user?.isAdmin == true
  val ext = FarmaciaColors.extended

  var selectedMedId by remember { mutableStateOf("") }
  var selectedProvId by remember { mutableStateOf("") }
  var cantidad by remember { mutableStateOf("") }
  var costo by remember { mutableStateOf("") }
  var estado by remember { mutableStateOf("Completado") }
  var isSuggested by remember { mutableStateOf(false) }
  var showMedMenu by remember { mutableStateOf(false) }
  var showProvMenu by remember { mutableStateOf(false) }
  var formError by remember { mutableStateOf<String?>(null) }
  var detailCompra by remember { mutableStateOf<CompraEntity?>(null) }

  val medName = { id: String -> meds.find { it.id == id }?.nombre ?: id }
  val provName = { id: String -> proveedores.find { it.id == id }?.empresa ?: id }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Spacer(Modifier.height(4.dp))
      Text("Compras & Reabastecimiento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
      Text("Ingreso de mercadería al inventario", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    item {
      RoleBannerCard(
        isAdmin = isAdmin,
        adminText = "Modo Administrador: emisión de órdenes de compra, costos unitarios y recepción directa.",
        employeeText = "Vista de Consulta (Empleado): puedes confirmar entregas pendientes de droguerías.",
      )
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.AddShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Registrar ingreso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          Box {
            OutlinedButton(
              onClick = { showMedMenu = true },
              modifier = Modifier.fillMaxWidth().testTag("compra_pick_med"),
            ) {
              Text(
                if (selectedMedId.isBlank()) "Selecciona medicamento"
                else meds.find { it.id == selectedMedId }?.let { "${it.nombre} (stock ${it.stock})" } ?: selectedMedId
              )
            }
            DropdownMenu(expanded = showMedMenu, onDismissRequest = { showMedMenu = false }) {
              meds.forEach { m ->
                DropdownMenuItem(
                  text = { Text("${m.nombre} (stock ${m.stock})") },
                  onClick = {
                    selectedMedId = m.id
                    val sug = PharmacyUtils.suggestedSupplier(m.id, compras, proveedores)
                    selectedProvId = sug.proveedor?.id ?: ""
                    costo = (sug.costo ?: m.precioCompra).toString()
                    isSuggested = true
                    showMedMenu = false
                  },
                )
              }
            }
          }

          Box {
            OutlinedButton(
              onClick = { showProvMenu = true },
              modifier = Modifier.fillMaxWidth().testTag("compra_pick_prov"),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (selectedProvId.isBlank()) "Selecciona proveedor" else provName(selectedProvId))
                if (isSuggested) {
                  Icon(Icons.Filled.AutoAwesome, contentDescription = "Sugerido", tint = ext.success, modifier = Modifier.size(16.dp))
                }
              }
            }
            DropdownMenu(expanded = showProvMenu, onDismissRequest = { showProvMenu = false }) {
              proveedores.forEach { p ->
                DropdownMenuItem(
                  text = { Text(p.empresa) },
                  onClick = {
                    selectedProvId = p.id
                    isSuggested = false
                    showProvMenu = false
                  },
                )
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = cantidad,
              onValueChange = { cantidad = it; formError = null },
              label = { Text("Cantidad") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = costo,
              onValueChange = { costo = it; formError = null },
              label = { Text("Costo unit. (Q)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = estado == "Completado",
              onClick = { estado = "Completado" },
              label = { Text("Completado (+stock)") },
              modifier = Modifier.weight(1f),
            )
            FilterChip(
              selected = estado == "Pendiente",
              onClick = { estado = "Pendiente" },
              label = { Text("Pendiente") },
              modifier = Modifier.weight(1f),
            )
          }

          val cInt = cantidad.toIntOrNull() ?: 0
          val kDbl = costo.toDoubleOrNull() ?: 0.0
          if (cInt > 0 && kDbl > 0) {
            Text(
              "Total compra: ${PharmacyUtils.q(cInt * kDbl)}",
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          if (!formError.isNullOrBlank()) {
            Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }

          if (isAdmin) {
            Button(
              onClick = {
                when {
                  selectedMedId.isBlank() || selectedProvId.isBlank() -> formError = "Selecciona producto y proveedor"
                  cInt <= 0 -> formError = "Cantidad inválida"
                  kDbl <= 0.0 -> formError = "Costo unitario inválido"
                  else -> onRegisterPurchase(selectedProvId, selectedMedId, cInt, kDbl, estado) {
                    cantidad = ""
                    isSuggested = false
                    formError = null
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = ext.success, contentColor = ext.onSuccess),
              modifier = Modifier.fillMaxWidth().testTag("compra_submit_button"),
            ) {
              Text("Registrar Compra", fontWeight = FontWeight.Bold)
            }
          } else {
            OutlinedButton(
              onClick = { onRestrictedNotice("Solo los Administradores pueden registrar nuevas compras.") },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Registrar Compra (Solo Admin)")
            }
          }
        }
      }
    }

    item {
      Text("Historial de compras", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }

    items(compras, key = { it.id }) { c ->
      val isComp = c.estado == "Completado"
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(c.codigo, fontWeight = FontWeight.Bold)
              Badge(containerColor = if (isComp) ext.successContainer else ext.warningContainer) {
                Text(
                  text = c.estado,
                  color = if (isComp) ext.success else ext.warning,
                  modifier = Modifier.padding(horizontal = 4.dp),
                )
              }
            }
            Text("${medName(c.medId)} x${c.cantidad} · ${provName(c.proveedorId)}", style = MaterialTheme.typography.bodySmall)
            Text(
              "${PharmacyUtils.fmtFechaHora(c.fecha)} · ${PharmacyUtils.q(c.cantidad * c.costo)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Row {
            if (!isComp) {
              IconButton(onClick = { onReceivePurchase(c) }) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Marcar recibida", tint = ext.success)
              }
            }
            IconButton(onClick = { detailCompra = c }) {
              Icon(Icons.Filled.Visibility, contentDescription = "Ver detalle ${c.codigo}")
            }
          }
        }
      }
    }
    item { Spacer(Modifier.height(80.dp)) }
  }

  detailCompra?.let { d ->
    val p = proveedores.find { it.id == d.proveedorId }
    AlertDialog(
      onDismissRequest = { detailCompra = null },
      title = { Text("Detalle ${d.codigo}", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Fecha: ${PharmacyUtils.fmtFechaHora(d.fecha)}")
          Text("Estado: ${d.estado}", fontWeight = FontWeight.Bold)
          Text("Proveedor: ${p?.empresa ?: "—"}")
          Text("Contacto: ${p?.contacto ?: "—"} · ${p?.telefono ?: ""}")
          Text("Producto: ${medName(d.medId)}")
          Text("Cantidad: ${d.cantidad} unidades")
          Text("Costo unitario: ${PharmacyUtils.q(d.costo)}")
          Text("Total: ${PharmacyUtils.q(d.costo * d.cantidad)}", fontWeight = FontWeight.Bold)
          if (!d.nota.isNullOrBlank()) Text("Nota: ${d.nota}")
        }
      },
      confirmButton = {
        if (d.estado == "Pendiente") {
          Button(
            onClick = {
              onReceivePurchase(d)
              detailCompra = null
            }
          ) {
            Text("Marcar recibida")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { detailCompra = null }) { Text("Cerrar") }
      },
    )
  }
}
