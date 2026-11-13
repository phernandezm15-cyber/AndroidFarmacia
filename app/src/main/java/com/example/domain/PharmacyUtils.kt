package com.example.domain

import com.example.data.local.CompraEntity
import com.example.data.local.MedicamentoEntity
import com.example.data.local.ProveedorEntity
import com.example.data.local.VentaWithItems
import com.example.data.repository.CartLineItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

enum class EstadoMed(val label: String) {
  OPTIMO("Óptimo"),
  POR_VENCER("Por Vencer"),
  CRITICO("Crítico"),
}

enum class PasswordStrengthLevel { VACIA, BAJA, MEDIA, FUERTE }

data class PasswordChecks(
  val minChars: Boolean,
  val hasNumber: Boolean,
  val hasMixedCaseOrSymbol: Boolean,
  val strongChars: Boolean,
)

data class PasswordAnalysis(
  val level: PasswordStrengthLevel,
  val score: Int,
  val label: String,
  val isAllowed: Boolean,
  val feedback: String,
  val checks: PasswordChecks,
)

data class SaleTotals(
  val subtotal: Double,
  val descuento: Double,
  val iva: Double,
  val total: Double,
)

data class InsightItem(
  val icon: String,
  val text: String,
)

data class SupplierSuggestion(
  val proveedor: ProveedorEntity?,
  val costo: Double?,
)

object PharmacyUtils {
  const val IVA_RATE = 0.12
  const val META_DIARIA = 2000.0
  const val ADMIN_MASTER_PASSWORD = "farmaciadbadmin123"
  const val XP_NIVEL = 500

  val CATEGORIAS = listOf(
    "Analgésicos",
    "Antibióticos",
    "Antiinflamatorios",
    "Antihistamínicos",
    "Gastrointestinal",
    "Cardiovascular",
    "Vitaminas",
    "Respiratorio",
    "Dermatológicos",
    "Diabetes",
  )

  private val localeGt = Locale("es", "GT")
  private val dayMs = 86_400_000L

  fun q(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(localeGt).apply {
      minimumFractionDigits = 2
      maximumFractionDigits = 2
    }
    return "Q ${nf.format(amount)}"
  }

  fun fmtFecha(isoDate: String): String {
    return try {
      val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val date = parser.parse(isoDate.take(10)) ?: return isoDate
      SimpleDateFormat("dd MMM yyyy", localeGt).format(date)
    } catch (_: Exception) {
      isoDate
    }
  }

  fun fmtFecha(epochMillis: Long): String {
    return SimpleDateFormat("dd MMM yyyy", localeGt).format(Date(epochMillis))
  }

  fun fmtFechaHora(epochMillis: Long): String {
    return SimpleDateFormat("dd MMM, HH:mm", localeGt).format(Date(epochMillis))
  }

  fun diasParaVencer(isoDate: String): Int {
    return try {
      val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val d = parser.parse(isoDate.take(10)) ?: return 999
      ceil((d.time + 43_200_000L - System.currentTimeMillis()).toDouble() / dayMs).toInt()
    } catch (_: Exception) {
      999
    }
  }

  fun estadoMed(med: MedicamentoEntity): EstadoMed {
    val dias = diasParaVencer(med.caducidad)
    return when {
      med.stock < 10 || dias < 0 -> EstadoMed.CRITICO
      dias <= 30 -> EstadoMed.POR_VENCER
      else -> EstadoMed.OPTIMO
    }
  }

  fun calcTotals(items: List<CartLineItem>, descPct: Double): SaleTotals {
    val subtotal = (items.sumOf { it.precio * it.cantidad } * 100.0).roundToInt() / 100.0
    val descuento = ((subtotal * (descPct / 100.0)) * 100.0).roundToInt() / 100.0
    val base = subtotal - descuento
    val iva = ((base * IVA_RATE) * 100.0).roundToInt() / 100.0
    val total = ((base + iva) * 100.0).roundToInt() / 100.0
    return SaleTotals(subtotal, descuento, iva, total)
  }

  fun puntosPor(total: Double): Int = (floor(total / 100.0) * 5).toInt()

  fun clientTier(puntos: Int): String = when {
    puntos >= 150 -> "Oro"
    puntos >= 50 -> "Plata"
    else -> "Bronce"
  }

  fun evaluatePasswordStrength(password: String): PasswordAnalysis {
    if (password.isEmpty()) {
      return PasswordAnalysis(
        level = PasswordStrengthLevel.VACIA,
        score = 0,
        label = "Ingresa una contraseña",
        isAllowed = false,
        feedback = "Escribe una contraseña para evaluar su seguridad.",
        checks = PasswordChecks(false, false, false, false),
      )
    }

    val length = password.length
    val hasMinChars = length >= 6
    val hasStrongChars = length >= 8
    val hasNumber = password.any { it.isDigit() }
    val hasLower = password.any { it.isLowerCase() }
    val hasUpper = password.any { it.isUpperCase() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val hasMixedCaseOrSymbol = (hasUpper && hasLower) || hasSpecial

    val isObviousPattern = Regex(
      "^(123456|password|qwerty|abcdef|111111|000000|admin|empleado)$",
      RegexOption.IGNORE_CASE,
    ).matches(password.trim())

    var score = 0
    if (hasMinChars) score += 1
    if (hasNumber && (hasLower || hasUpper)) score += 1
    if (hasStrongChars && (hasUpper || hasSpecial)) score += 1

    val checks = PasswordChecks(
      minChars = hasMinChars,
      hasNumber = hasNumber,
      hasMixedCaseOrSymbol = hasMixedCaseOrSymbol,
      strongChars = hasStrongChars,
    )

    if (isObviousPattern || length < 6) {
      return PasswordAnalysis(
        level = PasswordStrengthLevel.BAJA,
        score = 1,
        label = "Baja (Débil — No permitida)",
        isAllowed = false,
        feedback = "Contraseña débil. Se requiere al menos nivel Medio (mínimo 6 caracteres combinando letras y números).",
        checks = checks,
      )
    }

    if (score >= 3 && hasStrongChars && (hasSpecial || (hasUpper && hasNumber))) {
      return PasswordAnalysis(
        level = PasswordStrengthLevel.FUERTE,
        score = 3,
        label = "Fuerte (Excelente)",
        isAllowed = true,
        feedback = "Contraseña de alta seguridad.",
        checks = checks,
      )
    }

    return PasswordAnalysis(
      level = PasswordStrengthLevel.MEDIA,
      score = 2,
      label = "Media (Aceptable)",
      isAllowed = true,
      feedback = "Contraseña aceptable para registrar tu cuenta.",
      checks = checks,
    )
  }

  fun suggestedSupplier(
    medId: String,
    compras: List<CompraEntity>,
    proveedores: List<ProveedorEntity>,
  ): SupplierSuggestion {
    val last = compras.filter { it.medId == medId }.maxByOrNull { it.fecha }
    val prov = proveedores.find { it.id == last?.proveedorId } ?: proveedores.firstOrNull()
    return SupplierSuggestion(proveedor = prov, costo = last?.costo)
  }

  fun buildInsights(
    meds: List<MedicamentoEntity>,
    ventas: List<VentaWithItems>,
    compras: List<CompraEntity>,
    proveedores: List<ProveedorEntity>,
  ): List<InsightItem> {
    val out = mutableListOf<InsightItem>()
    val since = System.currentTimeMillis() - 7 * dayMs
    val qty = mutableMapOf<String, Int>()
    ventas.filter { it.venta.fecha >= since }.forEach { v ->
      v.items.forEach { item ->
        qty[item.medId] = (qty[item.medId] ?: 0) + item.cantidad
      }
    }
    val top = qty.entries.maxByOrNull { it.value }
    if (top != null) {
      val med = meds.find { it.id == top.key }
      if (med != null) {
        val sup = suggestedSupplier(med.id, compras, proveedores)
        out.add(
          InsightItem(
            icon = "💡",
            text = "${med.nombre} se está vendiendo rápido (${top.value} uds. en 7 días). Te sugerimos comprar 50 unidades más a ${sup.proveedor?.empresa ?: "tu proveedor"}.",
          )
        )
      }
    }
    val crit = meds.filter { it.stock < 10 }.sortedBy { it.stock }
    if (crit.isNotEmpty()) {
      val names = crit.take(2).joinToString(" y ") { "${it.nombre} (${it.stock})" }
      out.add(
        InsightItem(
          icon = "⚠️",
          text = "${crit.size} productos en stock crítico. Prioriza reponer $names.",
        )
      )
    }
    val venc = meds.filter {
      val d = diasParaVencer(it.caducidad)
      d in 0..30
    }
    if (venc.isNotEmpty()) {
      out.add(
        InsightItem(
          icon = "🏷️",
          text = "${venc.take(2).joinToString(", ") { it.nombre }} vence pronto. Considera una promoción del 10% para rotarlo.",
        )
      )
    }
    val pend = compras.count { it.estado == "Pendiente" }
    if (pend > 0) {
      out.add(
        InsightItem(
          icon = "📦",
          text = "Tienes $pend compras pendientes de recibir. Confírmalas para actualizar el stock.",
        )
      )
    }
    if (out.isEmpty()) {
      out.add(InsightItem("✅", "Todo en orden. El inventario está saludable."))
    }
    return out.take(4)
  }

  fun isValidEmail(email: String): Boolean =
    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$").matches(email.trim())
}
