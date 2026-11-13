package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.MedicamentoEntity
import com.example.data.local.VentaWithItems
import java.io.File
import java.io.FileOutputStream

data class SaleReceiptData(
  val codigo: String,
  val fechaMillis: Long,
  val vendedor: String,
  val cliente: String?,
  val items: List<com.example.data.repository.CartLineItem>,
  val subtotal: Double,
  val descuento: Double,
  val descPct: Double,
  val iva: Double,
  val total: Double,
  val efectivo: Double,
  val cambio: Double,
  val puntos: Int,
)

object ReportExporter {

  private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, file.name)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, chooserTitle).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }

  fun exportSalesCsv(context: Context, ventas: List<VentaWithItems>, rangeLabel: String): File {
    val file = File(context.cacheDir, "FarmaciaDB_Ventas_${System.currentTimeMillis() / 1000}.csv")
    val sb = StringBuilder()
    sb.appendLine("ID,Fecha,Vendedor,Productos,Subtotal (Q),Descuento (Q),IVA (Q),Total (Q)")
    ventas.forEach { v ->
      val prods = v.items.joinToString("; ") { "${it.nombre} x${it.cantidad}" }.replace("\"", "\"\"")
      sb.appendLine(
        "\"${v.venta.codigo}\",\"${PharmacyUtils.fmtFechaHora(v.venta.fecha)}\",\"${v.venta.vendedorNombre}\",\"$prods\",${String.format("%.2f", v.venta.subtotal)},${String.format("%.2f", v.venta.descuento)},${String.format("%.2f", v.venta.iva)},${String.format("%.2f", v.venta.total)}"
      )
    }
    val total = ventas.sumOf { it.venta.total }
    sb.appendLine("TOTAL ($rangeLabel),,,,,,,${String.format("%.2f", total)}")
    file.writeText(sb.toString(), Charsets.UTF_8)
    shareFile(context, file, "text/csv", "Compartir Reporte CSV")
    return file
  }

  fun exportInventoryCsv(context: Context, meds: List<MedicamentoEntity>, rangeLabel: String): File {
    val file = File(context.cacheDir, "FarmaciaDB_Inventario_${System.currentTimeMillis() / 1000}.csv")
    val sb = StringBuilder()
    sb.appendLine("ID,Codigo,Nombre,Principio,Categoria,Vencimiento,Compra (Q),Venta (Q),Stock,Estado")
    meds.forEach { m ->
      sb.appendLine(
        "\"${m.id}\",\"${m.codigo}\",\"${m.nombre}\",\"${m.principio}\",\"${m.categoria}\",\"${m.caducidad}\",${String.format("%.2f", m.precioCompra)},${String.format("%.2f", m.precioVenta)},${m.stock},\"${PharmacyUtils.estadoMed(m).label}\""
      )
    }
    val valor = meds.sumOf { it.stock * it.precioCompra }
    sb.appendLine("TOTAL ($rangeLabel),,,,,,,,${meds.sumOf { it.stock }},Valor Q ${String.format("%.2f", valor)}")
    file.writeText(sb.toString(), Charsets.UTF_8)
    shareFile(context, file, "text/csv", "Compartir Inventario CSV")
    return file
  }

  fun exportPdfDocument(
    context: Context,
    fileName: String,
    title: String,
    subtitle: String,
    lines: List<String>,
  ): File {
    val pdf = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40f
    var pageNumber = 1

    val headerPaint = Paint().apply {
      color = Color.rgb(30, 58, 138)
      textSize = 18f
      isFakeBoldText = true
    }
    val subPaint = Paint().apply {
      color = Color.rgb(71, 85, 105)
      textSize = 11f
    }
    val bodyPaint = Paint().apply {
      color = Color.rgb(15, 23, 42)
      textSize = 10.5f
    }
    val boldBodyPaint = Paint().apply {
      color = Color.rgb(15, 23, 42)
      textSize = 11.5f
      isFakeBoldText = true
    }
    val linePaint = Paint().apply {
      color = Color.rgb(203, 213, 225)
      strokeWidth = 1f
    }

    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas: Canvas = page.canvas
    var y = 52f

    fun drawPageHeader() {
      canvas.drawText("FarmaciaDB · Sistema de Control Farmacéutico", margin, y, subPaint)
      y += 22f
      canvas.drawText(title, margin, y, headerPaint)
      y += 16f
      canvas.drawText(subtitle, margin, y, subPaint)
      y += 12f
      canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
      y += 20f
    }

    drawPageHeader()

    for (raw in lines) {
      val wrapped = wrapText(raw, 78)
      for (segment in wrapped) {
        if (y > pageHeight - 50f) {
          pdf.finishPage(page)
          pageNumber += 1
          page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
          canvas = page.canvas
          y = 52f
          drawPageHeader()
        }
        val paint = if (segment.startsWith("##") || segment.startsWith("TOTAL")) boldBodyPaint else bodyPaint
        canvas.drawText(segment.removePrefix("##").trim(), margin, y, paint)
        y += 16f
      }
    }

    pdf.finishPage(page)
    val file = File(context.cacheDir, fileName)
    FileOutputStream(file).use { pdf.writeTo(it) }
    pdf.close()
    shareFile(context, file, "application/pdf", "Exportar PDF")
    return file
  }

  fun exportSalesPdf(context: Context, ventas: List<VentaWithItems>, rangeLabel: String): File {
    val total = ventas.sumOf { it.venta.total }
    val lines = mutableListOf<String>()
    lines.add("##Resumen: ${ventas.size} ventas · Total ${PharmacyUtils.q(total)}")
    lines.add("")
    ventas.forEach { v ->
      val prods = v.items.joinToString(", ") { "${it.nombre} x${it.cantidad}" }
      lines.add("${v.venta.codigo} | ${PharmacyUtils.fmtFechaHora(v.venta.fecha)} | ${v.venta.vendedorNombre} | ${PharmacyUtils.q(v.venta.total)}")
      lines.add("   Productos: $prods")
    }
    lines.add("")
    lines.add("TOTAL GENERAL DEL REPORTE: ${PharmacyUtils.q(total)}")
    return exportPdfDocument(
      context = context,
      fileName = "FarmaciaDB_Reporte_Ventas.pdf",
      title = "Reporte de Ventas — FarmaciaDB",
      subtitle = "Rango: $rangeLabel · ${ventas.size} transacciones",
      lines = lines,
    )
  }

  fun exportInventoryPdf(context: Context, meds: List<MedicamentoEntity>, rangeLabel: String): File {
    val valor = meds.sumOf { it.stock * it.precioCompra }
    val lines = mutableListOf<String>()
    lines.add("##Resumen: ${meds.size} productos · Valor al costo ${PharmacyUtils.q(valor)}")
    lines.add("")
    meds.forEach { m ->
      lines.add("${m.id} | ${m.nombre} (${m.principio}) | Stock: ${m.stock} | ${PharmacyUtils.estadoMed(m).label}")
      lines.add("   Cat: ${m.categoria} | Compra: ${PharmacyUtils.q(m.precioCompra)} | Venta: ${PharmacyUtils.q(m.precioVenta)} | Vence: ${m.caducidad}")
    }
    lines.add("")
    lines.add("TOTAL STOCK: ${meds.sumOf { it.stock }} unidades · VALORIZADO: ${PharmacyUtils.q(valor)}")
    return exportPdfDocument(
      context = context,
      fileName = "FarmaciaDB_Reporte_Inventario.pdf",
      title = "Reporte de Inventario — FarmaciaDB",
      subtitle = "Vencimiento: $rangeLabel · ${meds.size} productos",
      lines = lines,
    )
  }

  fun exportReceiptPdf(context: Context, r: SaleReceiptData): File {
    val lines = mutableListOf<String>()
    lines.add("NIT 1234567-8 · Ciudad de Guatemala")
    lines.add("Comprobante: ${r.codigo} · ${PharmacyUtils.fmtFechaHora(r.fechaMillis)}")
    lines.add("Atendió: ${r.vendedor}")
    if (!r.cliente.isNullOrBlank()) lines.add("Cliente: ${r.cliente}")
    lines.add("------------------------------------------------------------")
    r.items.forEach { item ->
      lines.add("${item.cantidad} x ${item.nombre} — ${PharmacyUtils.q(item.precio * item.cantidad)}")
    }
    lines.add("------------------------------------------------------------")
    lines.add("Subtotal: ${PharmacyUtils.q(r.subtotal)}")
    lines.add("Descuento (${r.descPct.toInt()}%): -${PharmacyUtils.q(r.descuento)}")
    lines.add("IVA (12%): ${PharmacyUtils.q(r.iva)}")
    lines.add("TOTAL A PAGAR: ${PharmacyUtils.q(r.total)}")
    lines.add("Efectivo recibido: ${PharmacyUtils.q(r.efectivo)}")
    lines.add("Cambio devuelto: ${PharmacyUtils.q(r.cambio)}")
    if (r.puntos > 0) lines.add("Puntos Salud ganados: +${r.puntos} pts")
    lines.add("")
    lines.add("¡Gracias por su compra en FarmaciaDB!")
    return exportPdfDocument(
      context = context,
      fileName = "Recibo_${r.codigo}.pdf",
      title = "Recibo de Venta ${r.codigo}",
      subtitle = "FarmaciaDB POS · Guatemala",
      lines = lines,
    )
  }

  private fun wrapText(text: String, maxChars: Int): List<String> {
    if (text.length <= maxChars) return listOf(text)
    val words = text.split(" ")
    val result = mutableListOf<String>()
    var current = StringBuilder()
    for (w in words) {
      if (current.length + w.length + 1 > maxChars) {
        result.add(current.toString())
        current = StringBuilder(w)
      } else {
        if (current.isNotEmpty()) current.append(" ")
        current.append(w)
      }
    }
    if (current.isNotEmpty()) result.add(current.toString())
    return result
  }
}
