package com.autojournal.app.domain

import com.autojournal.app.data.Car
import com.autojournal.app.data.OdometerLog
import com.autojournal.app.data.Refuel
import com.autojournal.app.data.Reminder
import com.autojournal.app.data.ServiceRecord
import com.autojournal.app.data.ServiceWithParts
import com.autojournal.app.data.total
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import kotlin.math.max

enum class DueState { OVERDUE, SOON, OK, OFF }

data class ReminderStatus(
    val reminder: Reminder,
    val kmLeft: Int?,
    val daysLeft: Int?,
    val state: DueState,
    /** 0..1 — какая часть интервала пройдена. */
    val progress: Float,
    /** Прогноз: через сколько дней наступит срок по пробегу (по среднему пробегу в день). */
    val predictedDays: Int?,
) {
    fun describe(): String {
        if (!reminder.enabled) return "Выключено"
        val parts = mutableListOf<String>()
        kmLeft?.let {
            parts += if (it <= 0) "просрочено на ${Fmt.km(-it)}" else "через ${Fmt.km(it)}"
        }
        daysLeft?.let {
            parts += when {
                it < 0 -> "просрочено на ${Fmt.days(-it)}"
                it == 0 -> "срок сегодня"
                else -> "через ${Fmt.days(it)}"
            }
        }
        if (parts.isEmpty()) return "Интервал не задан"
        return parts.joinToString(" · ").replaceFirstChar { it.uppercase() }
    }

    fun predictedDateText(today: Long): String? {
        val d = predictedDays ?: return null
        return "≈ " + Dates.format(today + d * Dates.DAY) + " по пробегу"
    }
}

data class FuelStats(
    val avgConsumption: Double?,
    /** id заправки -> расход л/100 км на отрезке, закончившемся этой заправкой. */
    val perRefuel: Map<Long, Double>,
    val series: List<Double>,
    val totalLiters: Double,
    val totalCost: Double,
    val avgPrice: Double?,
)

data class Expense(val date: Long, val category: String, val amount: Double, val isFuel: Boolean)

object Calc {

    /** Расход по методу «от полного бака до полного бака». */
    fun fuelStats(refuels: List<Refuel>): FuelStats {
        val sorted = refuels.sortedWith(compareBy<Refuel> { it.odometer }.thenBy { it.date })
        val per = LinkedHashMap<Long, Double>()
        var prevFull: Refuel? = null
        var acc = 0.0
        var broken = false
        var sumL = 0.0
        var sumKm = 0.0
        for (r in sorted) {
            if (r.missedPrevious) broken = true
            if (prevFull != null) acc += r.liters
            if (r.fullTank) {
                val p = prevFull
                if (p != null && !broken) {
                    val dist = r.odometer - p.odometer
                    if (dist > 0 && acc > 0) {
                        per[r.id] = acc / dist * 100.0
                        sumL += acc
                        sumKm += dist
                    }
                }
                prevFull = r
                acc = 0.0
                broken = false
            }
        }
        val totalL = refuels.sumOf { it.liters }
        val totalC = refuels.sumOf { it.total }
        return FuelStats(
            avgConsumption = if (sumKm > 0) sumL / sumKm * 100.0 else null,
            perRefuel = per,
            series = per.values.toList(),
            totalLiters = totalL,
            totalCost = totalC,
            avgPrice = if (totalL > 0) totalC / totalL else null,
        )
    }

    /** Все известные показания одометра: (дата, км). */
    fun readings(
        car: Car,
        refuels: List<Refuel>,
        services: List<ServiceRecord>,
        logs: List<OdometerLog>,
    ): List<Pair<Long, Int>> {
        val list = ArrayList<Pair<Long, Int>>()
        refuels.forEach { list += it.date to it.odometer }
        services.forEach { if (it.odometer > 0) list += it.date to it.odometer }
        logs.forEach { list += it.date to it.odometer }
        if (list.isEmpty() && car.initialOdometer > 0) list += Dates.today() to car.initialOdometer
        return list.sortedBy { it.first }
    }

    fun currentKm(
        car: Car,
        refuels: List<Refuel>,
        services: List<ServiceRecord>,
        logs: List<OdometerLog>,
    ): Int {
        var m = car.initialOdometer
        refuels.forEach { m = max(m, it.odometer) }
        services.forEach { m = max(m, it.odometer) }
        logs.forEach { m = max(m, it.odometer) }
        return m
    }

    /** Средний пробег в день — для прогноза даты замены. */
    fun kmPerDay(readings: List<Pair<Long, Int>>): Double? {
        if (readings.size < 2) return null
        val first = readings.first()
        val lastDate = readings.maxOf { it.first }
        val maxKm = readings.maxOf { it.second }
        val minKm = readings.minOf { it.second }
        val days = (lastDate - first.first) / Dates.DAY
        if (days < 14 || maxKm <= minKm) return null
        return (maxKm - minKm).toDouble() / days
    }

    fun reminderStatus(r: Reminder, currentKm: Int, today: Long, kmPerDay: Double?): ReminderStatus {
        var kmLeft: Int? = null
        var daysLeft: Int? = null
        var progress = 0f
        val ik = r.intervalKm
        if (ik != null && ik > 0) {
            val last = r.lastKm ?: 0
            kmLeft = last + ik - currentKm
            progress = max(progress, (currentKm - last).toFloat() / ik)
        }
        val id = r.intervalDays
        if (id != null && id > 0) {
            val last = r.lastDate ?: today
            val due = last + id * Dates.DAY
            daysLeft = Math.floorDiv(due - today, Dates.DAY).toInt()
            progress = max(progress, (today - last).toFloat() / (id * Dates.DAY).toFloat())
        }
        val kl = kmLeft
        val dl = daysLeft
        val state = when {
            !r.enabled || (kl == null && dl == null) -> DueState.OFF
            (kl != null && kl <= 0) || (dl != null && dl <= 0) -> DueState.OVERDUE
            (kl != null && kl <= r.warnKm) || (dl != null && dl <= r.warnDays) -> DueState.SOON
            else -> DueState.OK
        }
        val predicted = if (kl != null && kl > 0 && kmPerDay != null && kmPerDay > 0) {
            (kl / kmPerDay).toInt()
        } else null
        return ReminderStatus(r, kl, dl, state, progress.coerceIn(0f, 1f), predicted)
    }

    fun sortStatuses(list: List<ReminderStatus>): List<ReminderStatus> =
        list.sortedWith(compareBy<ReminderStatus> { it.state.ordinal }.thenByDescending { it.progress })

    fun expenses(refuels: List<Refuel>, services: List<ServiceWithParts>): List<Expense> =
        refuels.map { Expense(it.date, Categories.FUEL, it.total, true) } +
            services.map { Expense(it.record.date, it.record.category, it.total(), false) }

    fun costPerKm(readings: List<Pair<Long, Int>>, total: Double): Double? {
        if (readings.size < 2) return null
        val dist = readings.maxOf { it.second } - readings.minOf { it.second }
        return if (dist > 0) total / dist else null
    }
}
