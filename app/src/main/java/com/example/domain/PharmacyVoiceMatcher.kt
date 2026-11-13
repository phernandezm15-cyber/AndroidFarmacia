package com.example.domain

import com.example.data.local.MedicamentoEntity
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class VoiceConfidence { HIGH, MEDIUM, LOW }

data class VoiceAlternative(val name: String, val score: Double)

data class VoiceMatchResult(
  val searchTerm: String,
  val matchedMed: MedicamentoEntity? = null,
  val confidence: VoiceConfidence,
  val originalSpoken: String,
  val reason: String? = null,
  val alternatives: List<VoiceAlternative> = emptyList(),
)

object PharmacyVoiceMatcher {

  fun normalizeText(s: String): String {
    val decomposed = Normalizer.normalize(s, Normalizer.Form.NFD)
    return decomposed
      .replace(Regex("[\\u0300-\\u036f]"), "")
      .lowercase()
      .replace(Regex("[^a-z0-9\\s]"), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
  }

  fun phoneticKeySpanish(text: String): String {
    var s = normalizeText(text)
    s = s.replace("h", "")
    s = s.replace("v", "b")
    s = s.replace(Regex("ce|ci"), "se")
    s = s.replace("z", "s")
    s = s.replace(Regex("ca|co|cu|k|qu"), "k")
    s = s.replace(Regex("ge|gi|j"), "j")
    s = s.replace(Regex("ll|y"), "y")
    s = s.replace("ph", "f")
    s = s.replace("x", "ks")
    s = s.replace("w", "u")
    s = s.replace(Regex("(.)\\1+"), "$1")
    return s.trim()
  }

  fun levenshteinDistance(a: String, b: String): Int {
    val m = a.length
    val n = b.length
    if (m == 0) return n
    if (n == 0) return m
    val d = Array(m + 1) { IntArray(n + 1) }
    for (i in 0..m) d[i][0] = i
    for (j in 0..n) d[0][j] = j
    for (i in 1..m) {
      for (j in 1..n) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        d[i][j] = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
      }
    }
    return d[m][n]
  }

  fun similarityScore(a: String, b: String): Double {
    val normA = normalizeText(a)
    val normB = normalizeText(b)
    if (normA.isEmpty() || normB.isEmpty()) return 0.0
    if (normA == normB) return 1.0
    if (normA.contains(normB) || normB.contains(normA)) return 0.92

    val maxLen = max(normA.length, normB.length)
    val rawScore = 1.0 - levenshteinDistance(normA, normB).toDouble() / maxLen

    val pA = phoneticKeySpanish(normA)
    val pB = phoneticKeySpanish(normB)
    val pMaxLen = max(1, max(pA.length, pB.length))
    val phoneticScore = 1.0 - levenshteinDistance(pA, pB).toDouble() / pMaxLen

    return max(rawScore, phoneticScore)
  }

  val PHONETIC_CORRECTIONS: Map<String, String> = mapOf(
    "van a dar" to "Panadol Extra",
    "pan a dol" to "Panadol Extra",
    "para dolor" to "Panadol Extra",
    "panadoll" to "Panadol Extra",
    "panadol" to "Panadol Extra",
    "parador" to "Panadol Extra",
    "pan al sol" to "Panadol Extra",
    "paracetamol" to "Panadol Extra",
    "acetaminofen" to "Panadol Extra",
    "a mocil" to "Amoxil",
    "la mochila" to "Amoxil",
    "amorcito" to "Amoxil",
    "amoxil" to "Amoxil",
    "amoxicilina" to "Amoxil",
    "arbil" to "Advil",
    "abril" to "Advil",
    "alvil" to "Advil",
    "advil" to "Advil",
    "ibuprofeno" to "Advil",
    "hombre parasol" to "Omeprazol MK",
    "omeprazol" to "Omeprazol MK",
    "omeprasol" to "Omeprazol MK",
    "los estan" to "Losartán Denk",
    "lo saltan" to "Losartán Denk",
    "losartan" to "Losartán Denk",
    "bento link" to "Ventolin",
    "ventolin" to "Ventolin",
    "salbutamol" to "Ventolin",
    "inhalador" to "Ventolin",
    "por la tarde" to "Voltaren",
    "voltaren" to "Voltaren",
    "diclofenaco" to "Voltaren",
    "claritin" to "Clarityne",
    "clarityne" to "Clarityne",
    "loratadina" to "Clarityne",
    "aspirina" to "Aspirina Bayer",
    "si pero" to "Cipro",
    "cipro" to "Cipro",
    "ciprofloxacino" to "Cipro",
    "alegra" to "Allegra",
    "allegra" to "Allegra",
    "fexofenadina" to "Allegra",
    "gabi con" to "Gaviscon",
    "gaviscon" to "Gaviscon",
    "redoxon" to "Redoxon",
    "vitamina c" to "Redoxon",
    "atorvastatina" to "Atorvastatina MK",
    "gluco fay" to "Glucophage",
    "glucophage" to "Glucophage",
    "metformina" to "Glucophage",
    "mucosolvan" to "Mucosolvan",
    "ambroxol" to "Mucosolvan",
    "fucidin" to "Fucidin",
    "januvia" to "Januvia",
    "sitagliptina" to "Januvia",
    "centrum" to "Centrum",
    "multivitaminico" to "Centrum",
    "bepanthen" to "Bepanthen",
    "dexpantenol" to "Bepanthen",
  )

  val SYMPTOM_TO_MED: Map<String, List<String>> = mapOf(
    "dolor de cabeza" to listOf("Panadol Extra", "Aspirina Bayer", "Advil"),
    "migrana" to listOf("Panadol Extra", "Advil"),
    "fiebre" to listOf("Panadol Extra", "Advil"),
    "dolor muscular" to listOf("Voltaren", "Advil"),
    "inflamacion" to listOf("Voltaren", "Advil"),
    "gripe" to listOf("Mucosolvan", "Redoxon", "Panadol Extra"),
    "resfriado" to listOf("Mucosolvan", "Redoxon", "Panadol Extra"),
    "tos" to listOf("Mucosolvan", "Ventolin"),
    "asma" to listOf("Ventolin"),
    "acidez" to listOf("Omeprazol MK", "Gaviscon"),
    "gastritis" to listOf("Omeprazol MK", "Gaviscon"),
    "reflujo" to listOf("Omeprazol MK", "Gaviscon"),
    "alergia" to listOf("Clarityne", "Allegra"),
    "infeccion" to listOf("Amoxil", "Cipro", "Fucidin"),
    "presion" to listOf("Losartán Denk"),
    "colesterol" to listOf("Atorvastatina MK"),
    "diabetes" to listOf("Glucophage", "Januvia"),
    "vitaminas" to listOf("Centrum", "Redoxon"),
    "quemadura" to listOf("Bepanthen"),
    "herida" to listOf("Fucidin", "Bepanthen"),
  )

  val MED_INDICATIONS_TAGS: Map<String, List<String>> = mapOf(
    "MED-001" to listOf("fiebre", "calentura", "dolor", "dolor de cabeza", "paracetamol", "acetaminofen"),
    "MED-002" to listOf("inflamacion", "dolor muscular", "ibuprofeno", "golpe", "espalda"),
    "MED-003" to listOf("amoxicilina", "infeccion", "garganta", "amigdalas", "antibiotico"),
    "MED-004" to listOf("loratadina", "alergia", "rinitis", "estornudos", "picazon"),
    "MED-005" to listOf("gastritis", "acidez", "reflujo", "estomago", "ardor", "omeprazol"),
    "MED-006" to listOf("presion", "hipertension", "corazon", "losartan"),
    "MED-007" to listOf("vitaminas", "multivitaminico", "cansancio", "energia"),
    "MED-008" to listOf("asma", "bronquios", "respirar", "salbutamol", "inhalador"),
    "MED-009" to listOf("aspirina", "dolor de cabeza", "corazon", "fiebre"),
    "MED-010" to listOf("ciprofloxacino", "infeccion urinaria", "antibiotico"),
    "MED-011" to listOf("diclofenaco", "dolor fuerte", "inflamacion", "articulaciones"),
    "MED-012" to listOf("fexofenadina", "alergia fuerte", "ronchas", "picazon"),
    "MED-013" to listOf("reflujo", "acidez", "ardor", "estomago", "alginato"),
    "MED-014" to listOf("vitamina c", "defensas", "gripe", "resfriado"),
    "MED-015" to listOf("colesterol", "trigliceridos", "atorvastatina"),
    "MED-016" to listOf("quemaduras", "rozaduras", "piel", "cicatrizante"),
    "MED-017" to listOf("diabetes", "azucar", "glucosa", "metformina"),
    "MED-018" to listOf("tos", "flema", "expectorante", "gripe", "ambroxol"),
    "MED-019" to listOf("heridas", "infeccion piel", "crema", "acido fusidico"),
    "MED-020" to listOf("diabetes", "glucosa", "azucar", "sitagliptina"),
  )

  fun cleanSpokenPhrase(raw: String): String {
    var s = raw.trim()
    s = s.replace(Regex("^[¿¡\"'\\s.,;:-]+|[¿¡\"'\\s.,;:-]+$"), "")
    s = s.replace(
      Regex(
        "^(busca|buscar|encuentra|encontrar|quiero|necesito|dame|deme|tienes|tienen|hay|precio de|cuanto cuesta|consultar|ver|vendes|venden|medicamento para|medicina para|pastilla para|pastillas para|jarabe para|algo para|remedio para)\\s+",
        RegexOption.IGNORE_CASE,
      ),
      "",
    )
    s = s.replace(Regex("\\s+(por favor|gracias|muchas gracias)$", RegexOption.IGNORE_CASE), "")
    return s.trim()
  }

  fun resolvePharmaceuticalVoiceQuery(
    rawInput: String,
    catalogMeds: List<MedicamentoEntity>,
  ): VoiceMatchResult {
    val cleaned = cleanSpokenPhrase(rawInput)
    val normCleaned = normalizeText(cleaned)
    if (normCleaned.isEmpty()) {
      return VoiceMatchResult("", null, VoiceConfidence.LOW, rawInput)
    }

    PHONETIC_CORRECTIONS[normCleaned]?.let { targetName ->
      val med = catalogMeds.find {
        normalizeText(it.nombre).contains(normalizeText(targetName)) ||
          normalizeText(it.principio).contains(normalizeText(targetName))
      }
      return VoiceMatchResult(
        searchTerm = targetName,
        matchedMed = med,
        confidence = VoiceConfidence.HIGH,
        originalSpoken = rawInput,
        reason = "Corregido de \"$cleaned\" a \"$targetName\"",
      )
    }

    for ((misheard, proper) in PHONETIC_CORRECTIONS) {
      if (normCleaned.contains(misheard)) {
        val med = catalogMeds.find {
          normalizeText(it.nombre).contains(normalizeText(proper)) ||
            normalizeText(it.principio).contains(normalizeText(proper))
        }
        return VoiceMatchResult(
          searchTerm = proper,
          matchedMed = med,
          confidence = VoiceConfidence.HIGH,
          originalSpoken = rawInput,
          reason = "Corregido de \"$misheard\" a \"$proper\"",
        )
      }
    }

    for ((symptom, suggestions) in SYMPTOM_TO_MED) {
      if (normCleaned.contains(symptom)) {
        val primary = suggestions.first()
        val med = catalogMeds.find { normalizeText(it.nombre).contains(normalizeText(primary)) }
        val alts = suggestions.drop(1).map { VoiceAlternative(it, 0.85) }
        return VoiceMatchResult(
          searchTerm = primary,
          matchedMed = med,
          confidence = VoiceConfidence.MEDIUM,
          originalSpoken = rawInput,
          reason = "Indicado para \"$symptom\"",
          alternatives = alts,
        )
      }
    }

    data class ScoredMed(val med: MedicamentoEntity, val score: Double)
    val scored = mutableListOf<ScoredMed>()
    val tokens = normCleaned.split(" ").filter { it.length > 2 }

    for (m in catalogMeds) {
      var best = max(
        similarityScore(normCleaned, m.nombre),
        max(similarityScore(normCleaned, m.principio), similarityScore(normCleaned, m.categoria) * 0.8),
      )
      for (t in tokens) {
        best = max(best, max(similarityScore(t, m.nombre), similarityScore(t, m.principio)))
      }
      if (best >= 0.52) scored.add(ScoredMed(m, best))
    }

    scored.sortByDescending { it.score }
    if (scored.isNotEmpty()) {
      val top = scored.first()
      val conf = when {
        top.score >= 0.78 -> VoiceConfidence.HIGH
        top.score >= 0.60 -> VoiceConfidence.MEDIUM
        else -> VoiceConfidence.LOW
      }
      return VoiceMatchResult(
        searchTerm = top.med.nombre,
        matchedMed = top.med,
        confidence = conf,
        originalSpoken = rawInput,
        reason = if (top.score > 0.9) null else "Coincidencia fonética (${(top.score * 100).roundToInt()}%)",
        alternatives = scored.drop(1).take(3).map { VoiceAlternative(it.med.nombre, it.score) },
      )
    }

    return VoiceMatchResult(
      searchTerm = cleaned,
      matchedMed = null,
      confidence = VoiceConfidence.LOW,
      originalSpoken = rawInput,
    )
  }

  fun matchMedicineFilter(m: MedicamentoEntity, rawQuery: String): Boolean {
    val qClean = cleanSpokenPhrase(rawQuery)
    val qNorm = normalizeText(qClean)
    if (qNorm.isEmpty()) return true

    val mText = normalizeText("${m.nombre} ${m.principio} ${m.categoria} ${m.codigo}")
    if (mText.contains(qNorm)) return true

    val tokens = qNorm.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.size > 1 && tokens.all { mText.contains(it) }) return true

    PHONETIC_CORRECTIONS[qNorm]?.let { corrected ->
      if (mText.contains(normalizeText(corrected))) return true
    }

    val tags = MED_INDICATIONS_TAGS[m.id].orEmpty()
    for (tag in tags) {
      val normTag = normalizeText(tag)
      if (normTag.contains(qNorm) || qNorm.contains(normTag)) return true
    }

    return similarityScore(qNorm, m.nombre) >= 0.72
  }
}
