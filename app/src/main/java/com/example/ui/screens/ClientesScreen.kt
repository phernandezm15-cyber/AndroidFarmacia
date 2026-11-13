package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ClienteEntity
import com.example.domain.PharmacyUtils
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.FarmaciaColors

@Composable
fun ClientesScreen(
  clientes: List<ClienteEntity>,
  onSaveCliente: (ClienteEntity, onDone: () -> Unit) -> Unit,
  onDeleteCliente: (ClienteEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = FarmaciaColors.extended
  var search by remember { mutableStateOf("") }
  var editing by remember { mutableStateOf<ClienteEntity?>(null) }
  var toDelete by remember { mutableStateOf<ClienteEntity?>(null) }

  val totalPoints = remember(clientes) { clientes.sumOf { it.puntos } }
  val filtered = remember(clientes, search) {
    val q = search.trim().lowercase()
    if (q.isEmpty()) clientes
    else clientes.filter {
      "${it.nombre} ${it.nit} ${it.telefono}".lowercase().contains(q)
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
        Text("Clientes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("${clientes.size} clientes · $totalPoints Puntos Salud", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(
        onClick = { editing = ClienteEntity("", "", "", "CF", 0) },
        modifier = Modifier.testTag("cliente_add_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Nuevo")
      }
    }

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = ext.successContainer.copy(alpha = 0.45f),
      border = BorderStroke(1.dp, ext.success.copy(alpha = 0.35f)),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Favorite, contentDescription = null, tint = ext.success)
        Text(
          text = "Programa Puntos Salud: por cada Q100 de compra el cliente acumula 5 puntos. Niveles: Bronce (<50), Plata (50+), Oro (150+).",
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    OutlinedTextField(
      value = search,
      onValueChange = { search = it },
      placeholder = { Text("Buscar por nombre, NIT o teléfono...") },
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("clientes_search_input"),
    )

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(filtered, key = { it.id }) { c ->
        val tier = PharmacyUtils.clientTier(c.puntos)
        val tierColor = when (tier) {
          "Oro" -> ext.warning
          "Plata" -> MaterialTheme.colorScheme.primary
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
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
                Text(c.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Badge(containerColor = tierColor.copy(alpha = 0.18f)) {
                  Text(tier, color = tierColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                }
              }
              Text("Tel: ${c.telefono} · NIT: ${c.nit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${c.puntos} Puntos Salud", color = ext.success, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            Row {
              IconButton(onClick = { editing = c }) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar ${c.nombre}")
              }
              IconButton(onClick = { toDelete = c }) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${c.nombre}", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
      item { Spacer(Modifier.height(80.dp)) }
    }
  }

  editing?.let { c ->
    ClienteFormDialog(
      initial = c,
      onDismiss = { editing = null },
      onSave = { updated -> onSaveCliente(updated) { editing = null } },
    )
  }

  toDelete?.let { c ->
    ConfirmDeleteDialog(
      title = "¿Eliminar cliente?",
      description = "Se eliminará \"${c.nombre}\" y sus puntos acumulados.",
      onDismiss = { toDelete = null },
      onConfirm = {
        onDeleteCliente(c)
        toDelete = null
      },
    )
  }
}

@Composable
private fun ClienteFormDialog(
  initial: ClienteEntity,
  onDismiss: () -> Unit,
  onSave: (ClienteEntity) -> Unit,
) {
  var nombre by remember { mutableStateOf(initial.nombre) }
  var telefono by remember { mutableStateOf(initial.telefono) }
  var nit by remember { mutableStateOf(initial.nit.ifBlank { "CF" }) }
  var errMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial.id.isBlank()) "Nuevo cliente" else "Editar cliente", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre completo") }, singleLine = true)
        OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono (ej. 5512-3344)") }, singleLine = true)
        OutlinedTextField(value = nit, onValueChange = { nit = it }, label = { Text("NIT (o CF)") }, singleLine = true)
        if (!errMsg.isNullOrBlank()) {
          Text(errMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            nombre.trim().length < 3 -> errMsg = "Mínimo 3 caracteres para el nombre"
            !Regex("^[\\d\\s-]{8,}$").matches(telefono.trim()) -> errMsg = "Teléfono inválido (mín. 8 dígitos)"
            !Regex("^(CF|\\d{4,}-?[\\dkK])$", RegexOption.IGNORE_CASE).matches(nit.trim()) -> errMsg = "NIT inválido (usa dígitos o CF)"
            else -> onSave(
              initial.copy(
                nombre = nombre.trim(),
                telefono = telefono.trim(),
                nit = nit.trim().uppercase(),
              )
            )
          }
        },
        modifier = Modifier.testTag("cliente_save_button"),
      ) {
        Text("Guardar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar") }
    },
  )
}
