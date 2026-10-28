package com.autojournal.app.domain

import java.time.LocalDate
import kotlin.math.abs

/**
 * Разбор текста кассового чека АЗС.
 * Распознавание идёт латинской моделью, поэтому русские слова могут прийти искажёнными —
 * опираемся в первую очередь на числа: ищем тройку «литры × цена ≈ сумма».
 */
object ReceiptParser {

    data class Result(
        val liters: Double? = null,
        val price: Double? = null,
        val total: Double? = null,
        val grade: String? = null,
        val date: LocalDate? = null,
    ) {
        val isEmpty get() = liters == null && price == null && total == null
    }

    private val numberRe = Regex("""(?<![\d.,])(\d{1,3}(?:[  ]\d{3})+|\d{1,6})(?:[.,](\d{1,3}))?(?![\d])""")
    private val litersRe = Regex("""(?<![\d.,])(\d{1,3}(?:[.,]\d{1,3})?)\s*(?:л|Л|n|l|L|I|лит|литр)(?![a-zA-Zа-яА-Я])""")
    private val totalWords = Regex(
        """(?i)(ИТОГ|ИTOГ|ИТOГ|MTOF|MTOГ|NTOF|ИТОГО|TOTAL|СУММА|CYMMA|K\s*ОПЛАТЕ|К\s*ОПЛАТЕ|K\s*OПЛATE|ОПЛАЧЕНО|БЕЗНАЛ|БE3HAЛ)"""
    )
    private val gradeRe = Regex("""(?i)(?:A[ИIUNM1lЛ]|АИ|Аи|AN|Al)\s*[-–—.]?\s*(92|95|98|100)(?!\d)""")
    private val brandGradeRe = Regex("""(?i)(?:G[-\s]?DRIVE|PULSAR|ЕКТО|ECTO|ULTIMATE|EURO|ЕВРО|PREMIUM|ТАНЕКО|TANECO)\D{0,6}(92|95|98|100)(?!\d)""")
    private val dieselRe = Regex("""(?i)(?<![A-ZА-Я])(ДТ|DT|ДИЗ|DIESEL|ДИЗЕЛЬ|ДT|AT[\s-]*(?:ЕВРО|EURO|K5|К5|ЗИМ|МЕЖ))""")
    private val gasRe = Regex("""(?i)(ПРОПАН|ГАЗ|LPG|СУГ|CYГ|ПБА|ПБ\b)""")
    private val methaneRe = Regex("""(?i)(МЕТАН|CNG|КПГ)""")
    private val dateRe = Regex("""(?<!\d)(\d{2})[./-](\d{2})[./-](\d{2}|\d{4})(?!\d)""")

    private fun toDouble(intPart: String, frac: String?): Double? {
        val i = intPart.replace(" ", "").replace(" ", "")
        return (if (frac.isNullOrEmpty()) i else "$i.$frac").toDoubleOrNull()
    }

    /** Все числа строки: (значение, есть ли дробная часть). */
    private fun numbers(line: String): List<Pair<Double, Boolean>> =
        numberRe.findAll(line).mapNotNull { m ->
            toDouble(m.groupValues[1], m.groupValues[2].ifEmpty { null })?.let { it to m.groupValues[2].isNotEmpty() }
        }.toList()

    fun parse(text: String, today: LocalDate = LocalDate.now()): Result {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val decimals = lines.flatMap { l -> numbers(l).filter { it.second }.map { it.first } }
            .filter { it in 0.5..200_000.0 }.distinct()

        // Литры бывают и целыми («20 л»)
        val literCandidates = (decimals + lines.flatMap { l -> numbers(l).filter { !it.second }.map { it.first } }
            .filter { it in 1.0..300.0 }).distinct()

        // 1. Тройка литры × цена ≈ сумма
        var best: Triple<Double, Double, Double>? = null
        for (l in literCandidates) {
            if (l !in 1.0..300.0) continue
            for (p in decimals) {
                if (p == l || p !in 15.0..250.0) continue
                val expected = l * p
                val tol = maxOf(0.6, expected * 0.004)
                val t = decimals.filter { it != l && it != p && abs(it - expected) <= tol }.minByOrNull { abs(it - expected) }
                    ?: continue
                if (best == null || t > best.third) best = Triple(l, p, t)
            }
        }

        var liters = best?.first
        var price = best?.second
        var total = best?.third

        // 2. Сумма по словам «ИТОГ», «К ОПЛАТЕ»…
        if (total == null) {
            total = lines.filter { totalWords.containsMatchIn(it) }
                .flatMap { l -> numbers(l).filter { it.second }.map { it.first } }
                .filter { it >= 20 }.maxOrNull()
        }
        // 3. Литры по «л»
        if (liters == null) {
            liters = litersRe.findAll(text).mapNotNull { it.groupValues[1].replace(',', '.').toDoubleOrNull() }
                .filter { it in 1.0..300.0 }.firstOrNull()
        }
        // 4. Досчитываем недостающее
        if (price == null && liters != null && total != null && liters > 0) {
            val p = total / liters
            if (p in 15.0..250.0) price = Math.round(p * 100) / 100.0
        }
        if (total == null && liters != null && price != null) total = Math.round(liters * price * 100) / 100.0
        // Последний шанс: самая большая сумма с копейками
        if (total == null) total = decimals.filter { it in 50.0..50_000.0 }.maxOrNull()

        val grade = gradeRe.find(text)?.groupValues?.get(1)?.let { "АИ-$it" }
            ?: brandGradeRe.find(text)?.groupValues?.get(1)?.let { "АИ-$it" }
            ?: when {
                dieselRe.containsMatchIn(text) -> "ДТ"
                methaneRe.containsMatchIn(text) -> "Метан"
                gasRe.containsMatchIn(text) -> "Газ (пропан)"
                else -> null
            }

        val date = dateRe.findAll(text).mapNotNull { m ->
            val d = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            var y = m.groupValues[3].toInt()
            if (y < 100) y += 2000
            runCatching { LocalDate.of(y, mo, d) }.getOrNull()
        }.firstOrNull { it.year >= 2000 && !it.isAfter(today.plusDays(1)) }

        return Result(liters, price, total, grade, date)
    }
}
