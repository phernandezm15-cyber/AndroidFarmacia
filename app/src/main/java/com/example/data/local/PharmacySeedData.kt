package com.example.data.local

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

object PharmacySeedData {
  data class InitialSeedBundle(
    val medicamentos: List<MedicamentoEntity>,
    val proveedores: List<ProveedorEntity>,
    val clientes: List<ClienteEntity>,
    val compras: List<CompraEntity>,
    val ventas: List<VentaWithItems>,
  )

  fun buildInitialSeed(ownerUid: String, ownerName: String): InitialSeedBundle {
    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fun futureDate(days: Int): String = dateFormat.format(Date(now + days * dayMs))

    val initialMeds = listOf(
      MedicamentoEntity("MED-001", "75010000010", "Panadol Extra", "Paracetamol 500mg", "Analgésicos", futureDate(400), 8.50, 14.00, 120),
      MedicamentoEntity("MED-002", "75010079191", "Advil", "Ibuprofeno 400mg", "Antiinflamatorios", futureDate(300), 12.00, 19.50, 85),
      MedicamentoEntity("MED-003", "75010158382", "Amoxil", "Amoxicilina 500mg", "Antibióticos", futureDate(20), 35.00, 55.00, 6),
      MedicamentoEntity("MED-004", "75010237573", "Clarityne", "Loratadina 10mg", "Antihistamínicos", futureDate(250), 22.00, 34.00, 48),
      MedicamentoEntity("MED-005", "75010316764", "Omeprazol MK", "Omeprazol 20mg", "Gastrointestinal", futureDate(180), 15.00, 25.00, 64),
      MedicamentoEntity("MED-006", "75010395955", "Losartán Denk", "Losartán 50mg", "Cardiovascular", futureDate(365), 28.00, 42.00, 8),
      MedicamentoEntity("MED-007", "75010475146", "Centrum", "Multivitamínico", "Vitaminas", futureDate(500), 95.00, 139.00, 30),
      MedicamentoEntity("MED-008", "75010554337", "Ventolin", "Salbutamol 100mcg", "Respiratorio", futureDate(25), 48.00, 72.00, 22),
      MedicamentoEntity("MED-009", "75010633528", "Aspirina Bayer", "Ácido acetilsalicílico", "Analgésicos", futureDate(600), 6.00, 10.00, 150),
      MedicamentoEntity("MED-010", "75010712719", "Cipro", "Ciprofloxacino 500mg", "Antibióticos", futureDate(210), 40.00, 62.00, 5),
      MedicamentoEntity("MED-011", "75010791900", "Voltaren", "Diclofenaco 50mg", "Antiinflamatorios", futureDate(15), 18.00, 29.00, 40),
      MedicamentoEntity("MED-012", "75010871091", "Allegra", "Fexofenadina 180mg", "Antihistamínicos", futureDate(320), 45.00, 68.00, 26),
      MedicamentoEntity("MED-013", "75010950282", "Gaviscon", "Alginato de sodio", "Gastrointestinal", futureDate(140), 38.00, 58.00, 18),
      MedicamentoEntity("MED-014", "75011029473", "Redoxon", "Vitamina C 1g", "Vitaminas", futureDate(28), 30.00, 46.00, 55),
      MedicamentoEntity("MED-015", "75011108664", "Atorvastatina MK", "Atorvastatina 20mg", "Cardiovascular", futureDate(400), 32.00, 50.00, 9),
      MedicamentoEntity("MED-016", "75011187855", "Bepanthen", "Dexpantenol 5%", "Dermatológicos", futureDate(420), 42.00, 65.00, 35),
      MedicamentoEntity("MED-017", "75011267046", "Glucophage", "Metformina 850mg", "Diabetes", futureDate(380), 26.00, 40.00, 70),
      MedicamentoEntity("MED-018", "75011346237", "Mucosolvan", "Ambroxol 30mg", "Respiratorio", futureDate(12), 33.00, 49.00, 14),
      MedicamentoEntity("MED-019", "75011425428", "Fucidin", "Ácido fusídico 2%", "Dermatológicos", futureDate(260), 55.00, 82.00, 3),
      MedicamentoEntity("MED-020", "75011504619", "Januvia", "Sitagliptina 100mg", "Diabetes", futureDate(500), 210.00, 295.00, 12),
    )

    val initialProvs = listOf(
      ProveedorEntity("PRV-001", "Droguería Centroamericana", "Luis Méndez", "2245-1100", "ventas@drocen.gt", "Zona 9, Ciudad de Guatemala"),
      ProveedorEntity("PRV-002", "Laboratorios Bonin", "Ana Castillo", "2380-4422", "pedidos@bonin.gt", "Zona 12, Ciudad de Guatemala"),
      ProveedorEntity("PRV-003", "Farmasa Distribuciones", "Carlos Rivera", "2421-7788", "info@farmasa.com", "Mixco, Guatemala"),
      ProveedorEntity("PRV-004", "MediPharma GT", "Sofía Herrera", "2290-3355", "contacto@medipharma.gt", "Zona 10, Ciudad de Guatemala"),
      ProveedorEntity("PRV-005", "Grupo Selecta Salud", "Jorge Pérez", "7765-0099", "selecta@salud.gt", "Quetzaltenango"),
    )

    val initialClients = listOf(
      ClienteEntity("CLI-001", "Ana Morales", "5512-3344", "4589632-1", 45),
      ClienteEntity("CLI-002", "Carlos Juárez", "5523-8899", "CF", 10),
      ClienteEntity("CLI-003", "Lucía Barrios", "4456-7788", "7845123-6", 120),
      ClienteEntity("CLI-004", "Pedro Gómez", "5590-1122", "1234567-8", 0),
      ClienteEntity("CLI-005", "María Fernanda Ruiz", "3345-6677", "9988776-5", 75),
      ClienteEntity("CLI-006", "José Ramírez", "5567-0011", "CF", 20),
      ClienteEntity("CLI-007", "Elena Castillo", "4478-2233", "5566778-9", 200),
      ClienteEntity("CLI-008", "Diego Hernández", "5501-9988", "3322114-0", 35),
      ClienteEntity("CLI-009", "Sofía Pérez", "3390-4455", "CF", 5),
      ClienteEntity("CLI-010", "Ricardo López", "5544-6677", "6677889-1", 60),
    )

    val initialCompras = listOf(
      CompraEntity("CMP-001", "CMP-001", now - 1 * dayMs, "PRV-001", "MED-003", 50, 35.00, "Pendiente", "Entrega programada"),
      CompraEntity("CMP-002", "CMP-002", now - 3 * dayMs, "PRV-002", "MED-001", 100, 8.50, "Completado", null),
      CompraEntity("CMP-003", "CMP-003", now - 6 * dayMs, "PRV-004", "MED-007", 20, 95.00, "Completado", null),
      CompraEntity("CMP-004", "CMP-004", now - 8 * dayMs, "PRV-003", "MED-010", 40, 40.00, "Pendiente", null),
      CompraEntity("CMP-005", "CMP-005", now - 12 * dayMs, "PRV-001", "MED-009", 80, 6.00, "Completado", null),
      CompraEntity("CMP-006", "CMP-006", now - 15 * dayMs, "PRV-005", "MED-017", 60, 26.00, "Completado", null),
    )

    val cal = Calendar.getInstance()
    val initialVentas = (0 until 24).map { i ->
      val dayOffset = (i * 3) % 21
      val hour = 9 + ((i * 4) % 10)
      val minute = (i * 17) % 60
      cal.timeInMillis = now - dayOffset * dayMs
      cal.set(Calendar.HOUR_OF_DAY, hour)
      cal.set(Calendar.MINUTE, minute)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)

      val med = initialMeds[(i * 2) % initialMeds.size]
      val qty = 1 + (i % 3)
      val precio = med.precioVenta
      val subtotal = ((precio * qty) * 100.0).roundToInt() / 100.0
      val descPct = if (i % 5 == 0) 5.0 else 0.0
      val descuento = (((subtotal * descPct) / 100.0) * 100.0).roundToInt() / 100.0
      val iva = (((subtotal - descuento) * 0.12) * 100.0).roundToInt() / 100.0
      val total = ((subtotal - descuento + iva) * 100.0).roundToInt() / 100.0

      val cliente = if (i % 3 == 0) initialClients[i % initialClients.size] else null
      val puntos = if (cliente != null) (floor(total / 100.0) * 5).toInt() else 0
      val ventaUuid = "uuid-seed-vta-${i + 1}"
      val codigo = "VTA-${1000 + i + 1}"

      VentaWithItems(
        venta = VentaEntity(
          uuid = ventaUuid,
          codigo = codigo,
          fecha = cal.timeInMillis,
          subtotal = subtotal,
          descuento = descuento,
          descPct = descPct,
          iva = iva,
          total = total,
          vendedorNombre = ownerName,
          vendedorId = ownerUid,
          clienteId = cliente?.id,
          puntos = puntos,
        ),
        items = listOf(
          VentaItemEntity(
            itemId = 0,
            ventaId = ventaUuid,
            medId = med.id,
            nombre = med.nombre,
            cantidad = qty,
            precio = precio,
          )
        ),
      )
    }

    return InitialSeedBundle(
      medicamentos = initialMeds,
      proveedores = initialProvs,
      clientes = initialClients,
      compras = initialCompras,
      ventas = initialVentas,
    )
  }
}
