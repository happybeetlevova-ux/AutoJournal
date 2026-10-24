package com.autojournal.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Все даты хранятся как полночь UTC выбранного календарного дня. */
object Dates {
    const val DAY = 86_400_000L
    private val zone = ZoneOffset.UTC
    private val dayFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val ru: Locale = Locale.forLanguageTag("ru-RU")

    fun today(): Long = fromLocal(LocalDate.now())
    fun toLocal(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun fromLocal(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()
    fun format(ms: Long): String = toLocal(ms).format(dayFmt)
    fun yearMonth(ms: Long): YearMonth = YearMonth.from(toLocal(ms))
    fun monthShort(ym: YearMonth): String =
        ym.month.getDisplayName(TextStyle.SHORT_STANDALONE, ru).trimEnd('.')
    fun monthLong(ym: YearMonth): String =
        ym.month.getDisplayName(TextStyle.FULL_STANDALONE, ru).replaceFirstChar { it.uppercase() } + " " + ym.year
}

object Fmt {
    private val ru: Locale = Locale.forLanguageTag("ru-RU")

    private fun isWhole(v: Double) = abs(v - v.roundToLong()) < 0.005

    fun money(v: Double): String {
        val nf = NumberFormat.getNumberInstance(ru)
        val d = if (isWhole(v)) 0 else 2
        nf.maximumFractionDigits = d
        nf.minimumFractionDigits = d
        return nf.format(v) + " ₽"
    }

    fun num(v: Double, digits: Int = 1): String {
        val nf = NumberFormat.getNumberInstance(ru)
        nf.maximumFractionDigits = digits
        nf.minimumFractionDigits = digits
        return nf.format(v)
    }

    fun numTrim(v: Double, maxDigits: Int = 2): String {
        val nf = NumberFormat.getNumberInstance(ru)
        nf.maximumFractionDigits = maxDigits
        nf.minimumFractionDigits = 0
        return nf.format(v)
    }

    fun km(v: Int): String = NumberFormat.getIntegerInstance(ru).format(v) + " км"
    fun liters(v: Double): String = numTrim(v) + " л"

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n)
        val n10 = a % 10
        val n100 = a % 100
        return when {
            n10 == 1 && n100 != 11 -> one
            n10 in 2..4 && n100 !in 12..14 -> few
            else -> many
        }
    }

    fun days(n: Int): String = "$n ${plural(n, "день", "дня", "дней")}"

    /** Число для поля ввода: без лишних нулей, с точкой. */
    fun input(v: Double): String {
        if (isWhole(v)) return v.roundToLong().toString()
        return String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
    }

    fun parse(s: String): Double? =
        s.replace(',', '.').replace(" ", "").replace(" ", "").toDoubleOrNull()

    fun parseInt(s: String): Int? = s.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toLongOrNull()
        ?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
}
