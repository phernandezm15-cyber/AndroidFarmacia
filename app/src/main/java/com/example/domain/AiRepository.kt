package com.example.domain

import com.example.data.local.MedicamentoEntity
import com.example.data.local.VentaWithItems
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import java.util.Calendar
import java.util.Locale

data class FarmaciaAiContext(
  val meds: List<MedicamentoEntity> = emptyList(),
  val ventas: List<VentaWithItems> = emptyList(),
)

interface AiRepository {
  val isPlaceholderLocalMode: Boolean
  suspend fun askPharmacyAssistant(query: String, context: FarmaciaAiContext): String
}

class FirebaseAiRepository : AiRepository {
  override val isPlaceholderLocalMode: Boolean = false

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        temperature = 0.4f
      },
      systemInstruction = content {
        text(
          """
          Eres FarmaIA, el asistente farmacéutico clínico y operativo de FarmaciaDB (Guatemala, moneda Quetzales Q).
          Responde SIEMPRE en español claro, profesional y estructurado usando negritas (**texto**) y viñetas breves.
          Reglas:
          1. Cuando te consulten por inventario, stock crítico (< 10 uds.), vencimientos próximos (<= 60 días), precios o ventas de hoy, usa EXCLUSIVAMENTE los datos reales del contexto en vivo proporcionado.
          2. Cuando te consulten por síntomas, posología, interacciones (alcohol, embarazo) o monografías de medicamentos, ofrece orientación farmacológica precisa basada en evidencia y recuerda siempre consultar al médico ante signos de alarma o medicamentos que requieren receta.
          3. Incluye al inicio un título breve con emoji (ej. 🩺, 💊, 📊, ⚠️) y al final una recomendación práctica (💡 o 🛑).
          """.trimIndent()
        )
      },
    )
  }

  override suspend fun askPharmacyAssistant(query: String, context: FarmaciaAiContext): String {
    val contextSummary = buildLiveContextSummary(context)
    val fullPrompt = """
      [DATOS EN VIVO DE FARMACIADB]
      $contextSummary

      [CONSULTA DEL USUARIO]
      $query
    """.trimIndent()

    return withTimeout(60_000L) {
      val response = model.generateContent(fullPrompt)
      val text = response.text?.trim()
      if (text.isNullOrBlank()) {
        throw IllegalStateException("Gemini no devolvió contenido para la consulta.")
      }
      text
    }
  }

  private fun buildLiveContextSummary(context: FarmaciaAiContext): String {
    val meds = context.meds
    val ventas = context.ventas
    val now = System.currentTimeMillis()
    val todaySales = ventas.filter { isSameDay(it.venta.fecha, now) }
    val todayTotal = todaySales.sumOf { it.venta.total }

    val criticalStock = meds.filter { it.stock < 10 }.sortedBy { it.stock }
    val expiringSoon = meds.filter { PharmacyUtils.diasParaVencer(it.caducidad) <= 60 }
      .sortedBy { it.caducidad }

    val topSold = mutableMapOf<String, Int>()
    ventas.forEach { v ->
      v.items.forEach { item ->
        topSold[item.nombre] = (topSold[item.nombre] ?: 0) + item.cantidad
      }
    }
    val topList = topSold.entries.sortedByDescending { it.value }.take(5)
      .joinToString("; ") { "${it.key}: ${it.value} uds" }

    return buildString {
      appendLine("- Ventas de hoy: ${todaySales.size} transacciones, total ${PharmacyUtils.q(todayTotal)}.")
      appendLine("- Ventas históricas totales: ${ventas.size}.")
      if (topList.isNotBlank()) appendLine("- Más vendidos: $topList.")
      appendLine("- Stock crítico (<10 uds): " + if (criticalStock.isEmpty()) "Ninguno" else criticalStock.joinToString("; ") { "${it.nombre} (${it.principio})=${it.stock} uds @ Q${String.format(Locale.US, "%.2f", it.precioVenta)}" })
      appendLine("- Próximos a vencer (<=60 días): " + if (expiringSoon.isEmpty()) "Ninguno" else expiringSoon.joinToString("; ") { "${it.nombre} vence ${it.caducidad} (${PharmacyUtils.diasParaVencer(it.caducidad)}d, stock ${it.stock})" })
      appendLine("- Catálogo completo (${meds.size} productos): " + meds.joinToString("; ") { "${it.nombre} [${it.principio}, ${it.categoria}]: stock=${it.stock}, venta=Q${String.format(Locale.US, "%.2f", it.precioVenta)}, vence=${it.caducidad}" })
    }
  }

  private fun isSameDay(a: Long, b: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = a }
    val c2 = Calendar.getInstance().apply { timeInMillis = b }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
      c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
  }
}
