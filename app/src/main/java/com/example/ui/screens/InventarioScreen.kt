package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.data.local.MedicamentoEntity
import com.example.data.local.UserEntity
import com.example.domain.EstadoMed
import com.example.domain.PharmacyUtils
import com.example.domain.PharmacyVoiceMatcher
import com.example.ui.components.BarcodeView
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.RoleBannerCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.VoiceSearchButton

private enum class InvFilterChip { TODO, CRITICO, VENCER }

@Composable
fun InventarioScreen(
  user: UserEntity?,
  meds: List<MedicamentoEntity>,
  onSaveMed: (MedicamentoEntity, onDone: () -> Unit) -> Unit,
  onDeleteMed: (MedicamentoEntity) -> Unit,
  onRestrictedNotice: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isAdmin = user?.isAdmin == true
  var search by remember { mutableStateOf("") }
  var chip by remember { mutableStateOf(InvFilterChip.TODO) }
  var selectedCat by remember { mutableStateOf("Todas") }
  var showAdvFilters by remember { mutableStateOf(false) }
  var editingMed by remember { mutableStateOf<MedicamentoEntity?>(null) }
  var toDelete by remember { mutableStateOf<MedicamentoEntity?>(null) }

  val filtered = remember(meds, search, chip, selectedCat) {
    meds.filter { m ->
      val estado = PharmacyUtils.estadoMed(m)
      val matchQuery = search.isBlank() || PharmacyVoiceMatcher.matchMedicineFilter(m, search)
      val matchChip = when (chip) {
        InvFilterChip.TODO -> true
        InvFilterChip.CRITICO -> m.stock < 10
        InvFilterChip.VENCER -> estado == EstadoMed.POR_VENCER
      }
      val matchCat = selectedCat == "Todas" || m.categoria == selectedCat
      matchQuery && matchChip && matchCat
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("Inventario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("${meds.size} medicamentos registrados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (isAdmin) {
        Button(
          onClick = {
            editingMed = MedicamentoEntity("", "", "", "", PharmacyUtils.CATEGORIAS.first(), "", 0.0, 0.0, 0)
          },
          modifier = Modifier.testTag("inv_add_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Nuevo")
        }
      } else {
        OutlinedButton(
          onClick = { onRestrictedNotice("Solo los Administradores pueden registrar nuevos medicamentos.") },
        ) {
          Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Solo Admin")
        }
      }
    }

    RoleBannerCard(
      isAdmin = isAdmin,
      adminText = "Modo Administrador Total: alta de fármacos, edición de precios y eliminación.",
      employeeText = "Modo Catálogo de Empleado: consulta existencias por texto o voz y caducidades.",
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        placeholder = { Text("Buscar por nombre, principio o código...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("inv_search_input"),
      )
      VoiceSearchButton(meds = meds, onResolvedQuery = { search = it })
      Box {
        IconButton(
          onClick = { showAdvFilters = true },
          modifier = Modifier.size(48.dp),
        ) {
          Icon(Icons.Filled.FilterList, contentDescription = "Filtrar por categoría")
        }
        DropdownMenu(
          expanded = showAdvFilters,
          onDismissRequest = { showAdvFilters = false },
        ) {
          DropdownMenuItem(
            text = { Text("Todas las categorías") },
            onClick = { selectedCat = "Todas"; showAdvFilters = false },
          )
          PharmacyUtils.CATEGORIAS.forEach { cat ->
            DropdownMenuItem(
              text = { Text(cat) },
              onClick = { selectedCat = cat; showAdvFilters = false },
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = chip == InvFilterChip.TODO,
        onClick = { chip = InvFilterChip.TODO },
        label = { Text("Todos (${meds.size})") },
      )
      FilterChip(
        selected = chip == InvFilterChip.CRITICO,
        onClick = { chip = InvFilterChip.CRITICO },
        label = { Text("Stock Crítico (${meds.count { it.stock < 10 }})") },
      )
      FilterChip(
        selected = chip == InvFilterChip.VENCER,
        onClick = { chip = InvFilterChip.VENCER },
        label = { Text("Por Vencer (${meds.count { PharmacyUtils.estadoMed(it) == EstadoMed.POR_VENCER }})") },
      )
      if (selectedCat != "Todas") {
        FilterChip(
          selected = true,
          onClick = { selectedCat = "Todas" },
          label = { Text("$selectedCat ✕") },
        )
      }
    }

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(filtered, key = { it.id }) { m ->
        val estado = PharmacyUtils.estadoMed(m)
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${m.id} · ${m.nombre}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("${m.principio} · ${m.categoria}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              StatusBadge(estado)
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              BarcodeView(code = m.codigo)
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = PharmacyUtils.q(m.precioVenta),
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "Stock: ${m.stock} · Vence: ${PharmacyUtils.fmtFecha(m.caducidad)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (m.stock < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (isAdmin) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
              ) {
                TextButton(onClick = { editingMed = m }) {
                  Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Editar")
                }
                TextButton(onClick = { toDelete = m }) {
                  Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
      item { Spacer(Modifier.height(80.dp)) }
    }
  }

  editingMed?.let { med ->
    MedFormDialog(
      initial = med,
      onDismiss = { editingMed = null },
      onSave = { updated -> onSaveMed(updated) { editingMed = null } },
    )
  }

  toDelete?.let { med ->
    ConfirmDeleteDialog(
      title = "¿Eliminar medicamento?",
      description = "Se eliminará \"${med.nombre}\" del inventario. Esta acción no se puede deshacer.",
      onDismiss = { toDelete = null },
      onConfirm = {
        onDeleteMed(med)
        toDelete = null
      },
    )
  }
}

@Composable
private fun MedFormDialog(
  initial: MedicamentoEntity,
  onDismiss: () -> Unit,
  onSave: (MedicamentoEntity) -> Unit,
) {
  var nombre by remember { mutableStateOf(initial.nombre) }
  var principio by remember { mutableStateOf(initial.principio) }
  var categoria by remember { mutableStateOf(initial.categoria.ifBlank { PharmacyUtils.CATEGORIAS.first() }) }
  var caducidad by remember { mutableStateOf(initial.caducidad.ifBlank { "2027-06-30" }) }
  var pCompra by remember { mutableStateOf(if (initial.precioCompra > 0) initial.precioCompra.toString() else "") }
  var pVenta by remember { mutableStateOf(if (initial.precioVenta > 0) initial.precioVenta.toString() else "") }
  var stock by remember { mutableStateOf(initial.stock.toString()) }
  var codigo by remember { mutableStateOf(initial.codigo) }
  var showCatMenu by remember { mutableStateOf(false) }
  var errMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial.id.isBlank()) "Nuevo medicamento" else "Editar medicamento", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre comercial") }, singleLine = true)
        OutlinedTextField(value = principio, onValueChange = { principio = it }, label = { Text("Principio activo") }, singleLine = true)
        Box {
          OutlinedButton(onClick = { showCatMenu = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Categoría: $categoria")
          }
          DropdownMenu(expanded = showCatMenu, onDismissRequest = { showCatMenu = false }) {
            PharmacyUtils.CATEGORIAS.forEach { cat ->
              DropdownMenuItem(text = { Text(cat) }, onClick = { categoria = cat; showCatMenu = false })
            }
          }
        }
        OutlinedTextField(value = caducidad, onValueChange = { caducidad = it }, label = { Text("Caducidad (YYYY-MM-DD)") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = pCompra,
            onValueChange = { pCompra = it },
            label = { Text("Compra (Q)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = pVenta,
            onValueChange = { pVenta = it },
            label = { Text("Venta (Q)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = stock,
          onValueChange = { stock = it },
          label = { Text("Stock actual") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
        )
        OutlinedTextField(
          value = codigo,
          onValueChange = { codigo = it },
          label = { Text("Código (vacío = autogenerado)") },
          singleLine = true,
        )
        if (!errMsg.isNullOrBlank()) {
          Text(errMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val pc = pCompra.toDoubleOrNull() ?: 0.0
          val pv = pVenta.toDoubleOrNull() ?: 0.0
          val st = stock.toIntOrNull() ?: -1
          when {
            nombre.isBlank() || principio.isBlank() -> errMsg = "Nombre y principio son obligatorios"
            !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(caducidad.trim()) -> errMsg = "Usa formato YYYY-MM-DD"
            pc <= 0 || pv <= 0 -> errMsg = "Precios deben ser mayores a 0"
            pv < pc -> errMsg = "Precio de venta debe ser ≥ compra"
            st < 0 -> errMsg = "Stock debe ser entero ≥ 0"
            else -> onSave(
              initial.copy(
                nombre = nombre.trim(),
                principio = principio.trim(),
                categoria = categoria,
                caducidad = caducidad.trim(),
                precioCompra = pc,
                precioVenta = pv,
                stock = st,
                codigo = codigo.trim(),
              )
            )
          }
        },
        modifier = Modifier.testTag("inv_save_button"),
      ) {
        Text("Guardar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar") }
    },
  )
}
