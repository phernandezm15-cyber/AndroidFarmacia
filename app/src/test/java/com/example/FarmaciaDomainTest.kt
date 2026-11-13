package com.example

import com.example.data.local.MedicamentoEntity
import com.example.data.repository.CartLineItem
import com.example.domain.EstadoMed
import com.example.domain.PasswordStrengthLevel
import com.example.domain.PharmacyUtils
import com.example.domain.PharmacyVoiceMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmaciaDomainTest {

  @Test
  fun calcTotals_computesDiscountAndIva() {
    val items = listOf(
      CartLineItem("MED-001", "Panadol Extra", 100.0, 2),
    )
    val totals = PharmacyUtils.calcTotals(items, 10.0)
    assertEquals(200.0, totals.subtotal, 0.01)
    assertEquals(20.0, totals.descuento, 0.01)
    assertEquals(21.6, totals.iva, 0.01)
    assertEquals(201.6, totals.total, 0.01)
    assertEquals(10, PharmacyUtils.puntosPor(totals.total))
  }

  @Test
  fun passwordStrength_enforcesMinMedia() {
    assertFalse(PharmacyUtils.evaluatePasswordStrength("12345").isAllowed)
    assertFalse(PharmacyUtils.evaluatePasswordStrength("admin").isAllowed)

    val media = PharmacyUtils.evaluatePasswordStrength("ab1234")
    assertEquals(PasswordStrengthLevel.MEDIA, media.level)
    assertTrue(media.isAllowed)

    val fuerte = PharmacyUtils.evaluatePasswordStrength("Farma2026!")
    assertEquals(PasswordStrengthLevel.FUERTE, fuerte.level)
    assertTrue(fuerte.isAllowed)
  }

  @Test
  fun voiceMatcher_resolvesPhoneticCorruptionAndSymptoms() {
    val sampleMeds = listOf(
      MedicamentoEntity("MED-001", "75010000010", "Panadol Extra", "Paracetamol 500mg", "Analgésicos", "2028-01-01", 8.5, 14.0, 120),
      MedicamentoEntity("MED-003", "75010158382", "Amoxil", "Amoxicilina 500mg", "Antibióticos", "2028-01-01", 35.0, 55.0, 6),
    )
    val res1 = PharmacyVoiceMatcher.resolvePharmaceuticalVoiceQuery("dame para dolor", sampleMeds)
    assertEquals("Panadol Extra", res1.searchTerm)
    assertEquals("MED-001", res1.matchedMed?.id)

    val res2 = PharmacyVoiceMatcher.resolvePharmaceuticalVoiceQuery("necesito algo para infeccion", sampleMeds)
    assertEquals("Amoxil", res2.searchTerm)
    assertEquals(EstadoMed.CRITICO, PharmacyUtils.estadoMed(sampleMeds[1]))
  }
}
