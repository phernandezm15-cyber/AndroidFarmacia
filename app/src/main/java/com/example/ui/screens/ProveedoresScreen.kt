package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.domain.PharmacyUtils
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.RoleBannerCard

@Composable
fun ProveedoresScreen(
  user: UserEntity?,
  proveedores: List<ProveedorEntity>,
  onSaveProveedor: (ProveedorEntity, onDone: () -> Unit) -> Unit,
  onDeleteProveedor: (ProveedorEntity) -> Unit,
  onRestrictedNotice: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isAdmin = user?.isAdmin == true
  var search by remember { mutableStateOf("") }
  var editing by remember { mutableStateOf<ProveedorEntity?>(null) }
  var toDelete by remember { mutableStateOf<ProveedorEntity?>(null) }

  val filtered = remember(proveedores, search) {
    val q = search.trim().lowercase()
    if (q.isEmpty()) proveedores
    else proveedores.filter { "${it.empresa} ${it.contacto}".lowercase().contains(q) }
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
        Text("Proveedores", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("${proveedores.size} distribuidoras registradas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (isAdmin) {
        Button(
          onClick = { editing = ProveedorEntity("", "", "", "", "", "") },
          modifier = Modifier.testTag("prov_add_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Nuevo")
        }
      } else {
        OutlinedButton(
          onClick = { onRestrictedNotice("Solo los Administradores pueden registrar o modificar proveedores.") },
        ) {
          Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Solo Admin")
        }
      }
    }

    RoleBannerCard(
      isAdmin = isAdmin,
      adminText = "Modo Administrador: gestión de contratos con droguerías, contactos y catálogo.",
      employeeText = "Directorio Telefónico (Empleado): consulta de contactos autorizados.",
    )

    OutlinedTextField(
      value = search,
      onValueChange = { search = it },
      placeholder = { Text("Buscar empresa o contacto...") },
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("proveedores_search_input"),
    )

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(filtered, key = { it.id }) { p ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${p.id} · ${p.empresa}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Contacto: ${p.contacto}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
              }
              if (isAdmin) {
                Row {
                  IconButton(onClick = { editing = p }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar ${p.empresa}")
                  }
                  IconButton(onClick = { toDelete = p }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${p.empresa}", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(p.telefono, style = MaterialTheme.typography.bodySmall)
              Spacer(Modifier.width(8.dp))
              Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(p.correo, style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(p.direccion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
      item { Spacer(Modifier.height(80.dp)) }
    }
  }

  editing?.let { p ->
    ProvFormDialog(
      initial = p,
      onDismiss = { editing = null },
      onSave = { updated -> onSaveProveedor(updated) { editing = null } },
    )
  }

  toDelete?.let { p ->
    ConfirmDeleteDialog(
      title = "¿Eliminar proveedor?",
      description = "Se eliminará \"${p.empresa}\". Esta acción no se puede deshacer.",
      onDismiss = { toDelete = null },
      onConfirm = {
        onDeleteProveedor(p)
        toDelete = null
      },
    )
  }
}

@Composable
private fun ProvFormDialog(
  initial: ProveedorEntity,
  onDismiss: () -> Unit,
  onSave: (ProveedorEntity) -> Unit,
) {
  var empresa by remember { mutableStateOf(initial.empresa) }
  var contacto by remember { mutableStateOf(initial.contacto) }
  var telefono by remember { mutableStateOf(initial.telefono) }
  var correo by remember { mutableStateOf(initial.correo) }
  var direccion by remember { mutableStateOf(initial.direccion) }
  var errMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial.id.isBlank()) "Nuevo proveedor" else "Editar proveedor", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = empresa, onValueChange = { empresa = it }, label = { Text("Empresa") }, singleLine = true)
        OutlinedTextField(value = contacto, onValueChange = { contacto = it }, label = { Text("Contacto") }, singleLine = true)
        OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono") }, singleLine = true)
        OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo") }, singleLine = true)
        OutlinedTextField(value = direccion, onValueChange = { direccion = it }, label = { Text("Dirección") }, singleLine = true)
        if (!errMsg.isNullOrBlank()) {
          Text(errMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            empresa.isBlank() || contacto.isBlank() || telefono.isBlank() || direccion.isBlank() ->
              errMsg = "Completa todos los campos requeridos"
            !PharmacyUtils.isValidEmail(correo) -> errMsg = "Correo inválido"
            else -> onSave(
              initial.copy(
                empresa = empresa.trim(),
                contacto = contacto.trim(),
                telefono = telefono.trim(),
                correo = correo.trim(),
                direccion = direccion.trim(),
              )
            )
          }
        },
        modifier = Modifier.testTag("prov_save_button"),
      ) {
        Text("Guardar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar") }
    },
  )
}
