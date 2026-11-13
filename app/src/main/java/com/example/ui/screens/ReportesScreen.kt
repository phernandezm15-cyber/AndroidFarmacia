package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MedicamentoEntity
import com.example.data.local.UserEntity
import com.example.data.local.VentaWithItems
import com.example.domain.PharmacyUtils
import com.example.domain.ReportExporter
import com.example.ui.components.RoleBannerCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.FarmaciaColors
import java.util.Calendar

private enum class ReportType { VENTAS, INVENTARIO }
private enum class RangePreset(val label: String) {
  HOY("Hoy"),
  SIETE_DIAS("7 días"),
  MES("Este Mes"),
  TODO("Todo"),
}

@Composable
fun ReportesScreen(
  user: UserEntity?,
  ventas: List<VentaWithItems>,
  meds: List<MedicamentoEntity>,
  onNotify: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val isAdmin = user?.isAdmin == true
  val ext = FarmaciaColors.extended

  var reportType by remember { mutableStateOf(ReportType.VENTAS) }
  var preset by remember { mutableStateOf(RangePreset.MES) }

  val fromEpoch = remember(preset) {
    val cal = Calendar.getInstance()
    when (preset) {
      RangePreset.HOY -> {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.timeInMillis
      }
      RangePreset.SIETE_DIAS -> cal.timeInMillis - 6 * 86_400_000L
      RangePreset.MES -> {
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.timeInMillis
      }
      RangePreset.TODO -> Long.MIN_VALUE
    }
  }

  val ventasFiltered = remember(ventas, fromEpoch) {
    ventas.filter { it.venta.fecha >= fromEpoch }
  }
  val totalVentas = remember(ventasFiltered) { ventasFiltered.sumOf { it.venta.total } }
  val valorInventario = remember(meds) { meds.sumOf { it.stock * it.precioCompra } }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
      Text(
        text = if (isAdmin) "Reportes Financieros" else "Corte de Turno & Ventas",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
      )
      Text(
        text = if (reportType == ReportType.VENTAS)
          "${ventasFiltered.size} ventas en rango · Total ${PharmacyUtils.q(totalVentas)}"
        else
          "${meds.size} productos · Valor al costo ${PharmacyUtils.q(valorInventario)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    RoleBannerCard(
      isAdmin = isAdmin,
      adminText = "Modo Administrador Total: acceso a balances financieros, costos valorizados y exportación PDF/CSV.",
      employeeText = "Vista de Empleado (Corte de Turno): arqueo de caja y exportación de ventas del turno.",
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = reportType == ReportType.VENTAS,
        onClick = { reportType = ReportType.VENTAS },
        label = { Text("Reporte de Ventas") },
        modifier = Modifier.weight(1f),
      )
      FilterChip(
        selected = reportType == ReportType.INVENTARIO,
        onClick = {
          if (isAdmin) reportType = ReportType.INVENTARIO
          else onNotify("El reporte de inventario valorizado es exclusivo de Administradores.", true)
        },
        leadingIcon = if (!isAdmin) ({ Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null,
        label = { Text("Inventario (Admin)") },
        modifier = Modifier.weight(1f),
      )
    }

    if (reportType == ReportType.VENTAS) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        RangePreset.entries.forEach { p ->
          FilterChip(
            selected = preset == p,
            onClick = { preset = p },
            label = { Text(p.label) },
          )
        }
      }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(
        onClick = {
          try {
            if (reportType == ReportType.VENTAS) {
              if (ventasFiltered.isEmpty()) onNotify("Sin ventas en el rango seleccionado", true)
              else {
                ReportExporter.exportSalesPdf(context, ventasFiltered, preset.label)
                onNotify("PDF generado y listo para compartir", false)
              }
            } else {
              ReportExporter.exportInventoryPdf(context, meds, "Catálogo Completo")
              onNotify("PDF de inventario generado", false)
            }
          } catch (_: Exception) {
            onNotify("No se pudo exportar el PDF", true)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.weight(1f).testTag("export_pdf_button"),
      ) {
        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Exportar PDF")
      }
      OutlinedButton(
        onClick = {
          try {
            if (reportType == ReportType.VENTAS) {
              if (ventasFiltered.isEmpty()) onNotify("Sin ventas en el rango seleccionado", true)
              else {
                ReportExporter.exportSalesCsv(context, ventasFiltered, preset.label)
                onNotify("CSV generado y listo para compartir", false)
              }
            } else {
              ReportExporter.exportInventoryCsv(context, meds, "Catálogo Completo")
              onNotify("CSV de inventario generado", false)
            }
          } catch (_: Exception) {
            onNotify("No se pudo exportar el CSV", true)
          }
        },
        modifier = Modifier.weight(1f).testTag("export_csv_button"),
      ) {
        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Exportar CSV")
      }
    }

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (reportType == ReportType.VENTAS) {
        items(ventasFiltered, key = { it.venta.uuid }) { v ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${v.venta.codigo} · ${v.venta.vendedorNombre}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(PharmacyUtils.fmtFechaHora(v.venta.fecha), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                  v.items.joinToString { "${it.nombre} x${it.cantidad}" },
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(PharmacyUtils.q(v.venta.total), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text("IVA: ${PharmacyUtils.q(v.venta.iva)}", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
        if (ventasFiltered.isEmpty()) {
          item {
            Text("Sin ventas en este rango.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
          }
        }
      } else {
        items(meds, key = { it.id }) { m ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${m.id} · ${m.nombre}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("Compra: ${PharmacyUtils.q(m.precioCompra)} · Venta: ${PharmacyUtils.q(m.precioVenta)}", style = MaterialTheme.typography.labelSmall)
                Text("Stock: ${m.stock} · Vence: ${PharmacyUtils.fmtFecha(m.caducidad)}", style = MaterialTheme.typography.labelSmall, color = ext.success)
              }
              StatusBadge(PharmacyUtils.estadoMed(m))
            }
          }
        }
      }
      item { Spacer(Modifier.height(80.dp)) }
    }
  }
}
