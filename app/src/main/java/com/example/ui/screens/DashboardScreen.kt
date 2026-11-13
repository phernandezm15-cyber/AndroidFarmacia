package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.ProveedorEntity
import com.example.data.local.UserEntity
import com.example.data.local.VentaWithItems
import com.example.domain.PharmacyUtils
import com.example.ui.theme.FarmaciaColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private data class DayChartPoint(val label: String, val total: Double)
private data class CatSlice(val name: String, val value: Double, val color: Color)
private data class HeatCell(val dayOfWeek: Int, val hour: Int, val count: Int)
private data class StockPrediction(val med: MedicamentoEntity, val daysLeft: Int)

@Composable
fun DashboardScreen(
  user: UserEntity?,
  meds: List<MedicamentoEntity>,
  ventas: List<VentaWithItems>,
  proveedores: List<ProveedorEntity>,
  compras: List<CompraEntity>,
  onOpenManuales: () -> Unit,
  onGenerateRestockOrder: (medId: String, supplierId: String, cost: Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ext = FarmaciaColors.extended
  val now = remember { System.currentTimeMillis() }

  val todaySales = remember(ventas) {
    val c1 = Calendar.getInstance()
    val c2 = Calendar.getInstance()
    ventas.filter { v ->
      c2.timeInMillis = v.venta.fecha
      c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
  }
  val ventasHoy = remember(todaySales) { todaySales.sumOf { it.venta.total } }
  val alertas = remember(meds) { meds.count { it.stock < 10 } }
  val metaPct = min(1f, (ventasHoy / PharmacyUtils.META_DIARIA).toFloat())

  val insights = remember(meds, ventas, compras, proveedores) {
    PharmacyUtils.buildInsights(meds, ventas, compras, proveedores)
  }

  val predictions = remember(meds, ventas) {
    val since = now - 14 * 86_400_000L
    val qtyMap = mutableMapOf<String, Int>()
    ventas.filter { it.venta.fecha >= since }.forEach { v ->
      v.items.forEach { item -> qtyMap[item.medId] = (qtyMap[item.medId] ?: 0) + item.cantidad }
    }
    meds.mapNotNull { m ->
      val rate = (qtyMap[m.id] ?: 0) / 14.0
      if (rate <= 0.0) null
      else {
        val days = floor(m.stock / rate).toInt()
        if (days <= 14) StockPrediction(m, days) else null
      }
    }.sortedBy { it.daysLeft }.take(4)
  }

  val chart7Days = remember(ventas) {
    val fmt = SimpleDateFormat("EEE d", Locale("es", "GT"))
    val calTarget = Calendar.getInstance()
    val calItem = Calendar.getInstance()
    (6 downTo 0).map { offset ->
      calTarget.timeInMillis = now - offset * 86_400_000L
      val total = ventas.filter { v ->
        calItem.timeInMillis = v.venta.fecha
        calItem.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
          calItem.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
      }.sumOf { it.venta.total }
      DayChartPoint(fmt.format(Date(calTarget.timeInMillis)), total)
    }
  }

  val palette = listOf(ext.chartIndigo, ext.chartEmerald, ext.chartAmber, ext.chartCyan, ext.chartViolet, ext.chartSlate)
  val categorySlices = remember(meds, ventas, palette) {
    val catOf = meds.associate { it.id to it.categoria }
    val acc = mutableMapOf<String, Double>()
    ventas.forEach { v ->
      v.items.forEach { i ->
        val cat = catOf[i.medId] ?: "Otros"
        acc[cat] = (acc[cat] ?: 0.0) + i.precio * i.cantidad
      }
    }
    val sorted = acc.entries.sortedByDescending { it.value }
    val top = sorted.take(5).mapIndexed { idx, e -> CatSlice(e.key, e.value, palette[idx % palette.size]) }
    val rest = sorted.drop(5).sumOf { it.value }
    if (rest > 0.0) top + CatSlice("Otros", rest, ext.chartSlate) else top
  }

  val heatCells = remember(ventas) {
    val cal = Calendar.getInstance()
    val acc = mutableMapOf<Pair<Int, Int>, Int>()
    ventas.forEach { v ->
      cal.timeInMillis = v.venta.fecha
      val key = (cal.get(Calendar.DAY_OF_WEEK) - 1) to cal.get(Calendar.HOUR_OF_DAY)
      acc[key] = (acc[key] ?: 0) + 1
    }
    acc.map { (k, count) -> HeatCell(k.first, k.second, count) }
  }

  val ranking = remember(ventas) {
    val nowCal = Calendar.getInstance()
    val saleCal = Calendar.getInstance()
    val acc = mutableMapOf<String, Double>()
    ventas.filter { v ->
      saleCal.timeInMillis = v.venta.fecha
      saleCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
        saleCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
    }.forEach { v ->
      acc[v.venta.vendedorNombre] = (acc[v.venta.vendedorNombre] ?: 0.0) + v.venta.total
    }
    acc.entries.sortedByDescending { it.value }.take(5)
  }

  val mySalesToday = remember(todaySales, user) {
    todaySales.filter { it.venta.vendedorId == user?.uid }.sumOf { it.venta.total }
  }
  val xp = floor(mySalesToday / 10.0).toInt()
  val level = (xp / PharmacyUtils.XP_NIVEL) + 1

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Spacer(Modifier.height(4.dp))
      Text("Panel de Control", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
      Text(
        "Datos en tiempo real desde Cloud Firestore",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // Manuals Banner
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(10.dp),
              )
            }
            Column {
              Text("Manual Técnico & Usuario", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(
                "Documentación oficial y exportación a PDF",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Button(
            onClick = onOpenManuales,
            modifier = Modifier.testTag("open_manuals_banner_button"),
          ) {
            Text("Abrir")
          }
        }
      }
    }

    // Daily Target Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Meta del Día: ${PharmacyUtils.q(PharmacyUtils.META_DIARIA)}", fontWeight = FontWeight.Bold)
            }
            Text(
              text = "${PharmacyUtils.q(ventasHoy)} · ${(metaPct * 100).toInt()}%",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = if (metaPct >= 1f) ext.success else MaterialTheme.colorScheme.primary,
            )
          }
          LinearProgressIndicator(
            progress = { metaPct },
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(999.dp)),
          )
          if (metaPct >= 1f) {
            Text("¡Meta diaria alcanzada!", color = ext.success, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    // 4 KPIs (2x2 grid)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          KpiCard("Ventas del Día", PharmacyUtils.q(ventasHoy), Icons.Filled.AttachMoney, ext.success, Modifier.weight(1f))
          KpiCard("Productos", meds.size.toString(), Icons.Filled.Medication, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          KpiCard("Stock Crítico", alertas.toString(), Icons.Filled.Warning, MaterialTheme.colorScheme.error, Modifier.weight(1f))
          KpiCard("Proveedores", proveedores.size.toString(), Icons.Filled.LocalShipping, ext.warning, Modifier.weight(1f))
        }
      }
    }

    // Insights
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Sugerencias del Sistema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          insights.forEach { insight ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(12.dp),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(insight.icon)
                Text(insight.text, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }

    // Predictive Alerts
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.TrendingDown, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text("Alertas Predictivas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          if (predictions.isEmpty()) {
            Text(
              "Ningún producto se agotará en las próximas 2 semanas.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            predictions.forEach { pred ->
              val sug = PharmacyUtils.suggestedSupplier(pred.med.id, compras, proveedores)
              Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(pred.med.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                      "Se agotará en ${if (pred.daysLeft <= 0) "<1" else pred.daysLeft} días (stock ${pred.med.stock})",
                      style = MaterialTheme.typography.bodySmall,
                    )
                  }
                  if (user?.isAdmin == true) {
                    OutlinedButton(
                      onClick = {
                        onGenerateRestockOrder(
                          pred.med.id,
                          sug.proveedor?.id ?: "",
                          sug.costo ?: pred.med.precioCompra,
                        )
                      },
                      modifier = Modifier.testTag("restock_${pred.med.id}"),
                    ) {
                      Text("Reponer 50", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Admin-only ranking & XP
    if (user?.isAdmin == true) {
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
              Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ext.warning)
              Text("Empleado del Mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            ranking.forEachIndexed { idx, entry ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(
                    if (idx == 0) ext.warningContainer.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                  )
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "${idx + 1}. ${entry.key}",
                  fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium,
                  style = MaterialTheme.typography.bodyMedium,
                )
                Text(PharmacyUtils.q(entry.value), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
              }
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Tu experiencia de hoy · Nivel $level", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            LinearProgressIndicator(
              progress = { (xp % PharmacyUtils.XP_NIVEL).toFloat() / PharmacyUtils.XP_NIVEL },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            )
            Text(
              "${xp % PharmacyUtils.XP_NIVEL} / ${PharmacyUtils.XP_NIVEL} XP (1 XP por cada Q10 vendidos · Hoy: ${PharmacyUtils.q(mySalesToday)})",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // 7-day Bar Chart
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Ventas últimos 7 días", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          val maxVal = max(1.0, chart7Days.maxOfOrNull { it.total } ?: 1.0)
          val barColor = ext.chartIndigo
          val gridColor = MaterialTheme.colorScheme.outlineVariant
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(150.dp)
          ) {
            val count = chart7Days.size
            val slot = size.width / count
            val barW = slot * 0.56f
            for (g in 1..3) {
              val y = size.height * (g / 4f)
              drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            chart7Days.forEachIndexed { idx, pt ->
              val ratio = (pt.total / maxVal).toFloat().coerceIn(0f, 1f)
              val barH = max(4f, size.height * 0.9f * ratio)
              val x = idx * slot + (slot - barW) / 2f
              drawRoundRect(
                color = barColor,
                topLeft = Offset(x, size.height - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(12f, 12f),
              )
            }
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            chart7Days.forEach { pt ->
              Text(pt.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }

    // Category Donut Chart
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Ventas por Categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          val totalCats = max(1.0, categorySlices.sumOf { it.value })
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Canvas(modifier = Modifier.size(128.dp)) {
              var startAngle = -90f
              val strokeWidth = 26f
              categorySlices.forEach { slice ->
                val sweep = (slice.value / totalCats).toFloat() * 360f
                drawArc(
                  color = slice.color,
                  startAngle = startAngle,
                  sweepAngle = max(2f, sweep - 3f),
                  useCenter = false,
                  style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                startAngle += sweep
              }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              categorySlices.forEach { slice ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                    Text(slice.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                  Text(PharmacyUtils.q(slice.value), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }

    // Heatmap Scatter Chart
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Mapa de calor: horas pico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            "Franjas día / hora (08:00 a 20:00). Círculos mayores indican mayor volumen de ventas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          val maxHeat = max(1, heatCells.maxOfOrNull { it.count } ?: 1)
          val dotColor = ext.chartIndigo
          val lineColor = MaterialTheme.colorScheme.outlineVariant
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
          ) {
            for (d in 0..6) {
              val y = (d + 0.5f) / 7f * size.height
              drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            heatCells.forEach { cell ->
              val normHour = ((cell.hour - 7).coerceIn(0, 14)) / 14f
              val x = 16f + normHour * (size.width - 32f)
              val y = (cell.dayOfWeek + 0.5f) / 7f * size.height
              val intensity = cell.count.toFloat() / maxHeat
              drawCircle(
                color = dotColor.copy(alpha = 0.28f + 0.72f * intensity),
                radius = 7f + 13f * intensity,
                center = Offset(x, y),
              )
            }
          }
        }
      }
    }

    // Last 5 sales
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Últimas 5 ventas realizadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          ventas.take(5).forEach { v ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${v.venta.codigo} · ${v.venta.vendedorNombre}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = "${PharmacyUtils.fmtFechaHora(v.venta.fecha)} — ${v.items.joinToString { "${it.nombre} x${it.cantidad}" }}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(Modifier.width(8.dp))
              Text(PharmacyUtils.q(v.venta.total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
          }
        }
      }
      Spacer(Modifier.height(80.dp))
    }
  }
}

@Composable
private fun KpiCard(
  label: String,
  value: String,
  icon: ImageVector,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
