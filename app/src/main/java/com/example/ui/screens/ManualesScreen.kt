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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ReportExporter

private data class ManualSection(
  val title: String,
  val body: List<String>,
)

@Composable
fun ManualesScreen(
  onNotify: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }

  val tecnicoSections = remember {
    listOf(
      ManualSection(
        "1. Stack Tecnológico Android Nativo",
        listOf(
          "• Lenguaje: Kotlin con corrutinas, Flow y StateFlow reactivo.",
          "• Interfaz: Jetpack Compose + Material Design 3 + tipografía Plus Jakarta Sans.",
          "• Base de Datos Compartida: Supabase PostgreSQL (REST + RPC atómicos) con respaldo SQLite (Room).",
          "• Autenticación e IA: Correo y contraseña (hash PBKDF2) sin Google + Firebase AI Logic (Gemini).",
          "• Exportación: PdfDocument nativo + FileProvider para PDF y CSV.",
        ),
      ),
      ManualSection(
        "2. Arquitectura MVVM y Capas",
        listOf(
          "• Capa de Datos: SupabaseApi, SupabaseFarmaciaRepository, RoomFarmaciaRepository, FarmaciaRepositoryProvider y SessionPreferences.",
          "• Capa de Dominio: PharmacyUtils, PharmacyVoiceMatcher, FirebaseAiRepository y ReportExporter.",
          "• Capa UI: FarmaciaViewModel con sincronización automática cada 5s y pantallas modulares en Compose.",
        ),
      ),
      ManualSection(
        "3. Modelo de Datos Compartido (PostgreSQL / Room)",
        listOf(
          "• farmacia_users: uid, nombre, email (único), role (admin | empleado), password_hash.",
          "• medicamentos: código, nombre, principio, categoría, caducidad, precios y stock.",
          "• clientes, proveedores, compras, ventas y venta_items con RPC atómicos en PostgreSQL.",
        ),
      ),
      ManualSection(
        "4. Control de Acceso Basado en Roles (RBAC)",
        listOf(
          "• Administrador: alta/edición/eliminación en inventario y proveedores, registro de compras, reportes valorizados y gestión de personal.",
          "• Empleado: Punto de Venta, cobro rápido, búsqueda por voz/código, clientes Puntos Salud, corte de turno y asistente clínico.",
        ),
      ),
    )
  }

  val usuarioSections = remember {
    listOf(
      ManualSection(
        "1. Inicio de Sesión y Registro Multi-celular",
        listOf(
          "• Inicia sesión o crea tu cuenta con correo electrónico y contraseña compartidos en la nube (Supabase).",
          "• Para registrar una cuenta de Administrador se solicita el código maestro de autorización; las cuentas Empleado se crean de forma directa.",
          "• Puedes cambiar tu contraseña desde '¿Olvidaste tu contraseña?' o desde Perfil y Permisos.",
        ),
      ),
      ManualSection(
        "2. Punto de Venta (POS)",
        listOf(
          "• Busca medicamentos por nombre, principio activo, síntoma (ej. 'dolor de cabeza') o código de barras.",
          "• Usa el botón de micrófono para dictado farmacéutico con corrección fonética.",
          "• Asocia un cliente para acumular 5 Puntos Salud por cada Q100 facturados.",
          "• Aplica descuentos (0%, 5%, 10%), calcula el cambio en efectivo y comparte el recibo PDF.",
        ),
      ),
      ManualSection(
        "3. Inventario, Compras y Alertas",
        listOf(
          "• Filtra productos en Stock Crítico (<10 uds.) o Próximos a Vencer (≤30 días).",
          "• En Compras, al elegir un medicamento se sugiere automáticamente el último proveedor y costo.",
          "• Confirma compras pendientes con el botón 'Marcar recibida' para sumar stock automáticamente.",
        ),
      ),
      ManualSection(
        "4. Asistente FarmaIA y Reportes",
        listOf(
          "• Pulsa el botón flotante de FarmaIA para consultar monografías, interacciones y datos de inventario.",
          "• En Reportes puedes filtrar por fecha y exportar a PDF o CSV usando el selector del sistema.",
        ),
      ),
    )
  }

  val activeSections = if (selectedTab == 0) tecnicoSections else usuarioSections
  val activeTitle = if (selectedTab == 0) "Manual Técnico — FarmaciaDB" else "Manual de Usuario — FarmaciaDB"

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
      Column(modifier = Modifier.weight(1f)) {
        Text("Manuales Oficiales", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Documentación Técnica y Guía de Usuario", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Button(
        onClick = {
          try {
            val lines = activeSections.flatMap { sec -> listOf("##${sec.title}") + sec.body + "" }
            ReportExporter.exportPdfDocument(
              context = context,
              fileName = if (selectedTab == 0) "Manual_Tecnico_FarmaciaDB.pdf" else "Manual_Usuario_FarmaciaDB.pdf",
              title = activeTitle,
              subtitle = "Universidad Mariano Gálvez de Guatemala · FarmaciaDB",
              lines = lines,
            )
            onNotify("PDF de manual generado", false)
          } catch (_: Exception) {
            onNotify("Error al generar PDF del manual", true)
          }
        },
        modifier = Modifier.testTag("manual_export_pdf_button"),
      ) {
        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("PDF")
      }
    }

    TabRow(
      selectedTabIndex = selectedTab,
      modifier = Modifier.clip(RoundedCornerShape(12.dp)),
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        icon = { Icon(Icons.Filled.Code, contentDescription = null) },
        text = { Text("Manual Técnico") },
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        icon = { Icon(Icons.Filled.Book, contentDescription = null) },
        text = { Text("Manual de Usuario") },
      )
    }

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("UNIVERSIDAD MARIANO GÁLVEZ DE GUATEMALA", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
            Text("Facultad de Ingeniería en Sistemas de Información", style = MaterialTheme.typography.bodySmall)
            Text("FarmaciaDB · Sistema de Control Farmacéutico", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }
        }
      }

      items(activeSections) { sec ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(sec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            sec.body.forEach { line ->
              Text(line, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
      item { Spacer(Modifier.height(80.dp)) }
    }
  }
}
