package com.autojournal.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autojournal.app.App
import com.autojournal.app.domain.Calc
import com.autojournal.app.domain.DueState
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt

/**
 * Дважды в сутки проверяет напоминания всех автомобилей.
 * «Скоро» — уведомляет один раз, «просрочено» — повторяет раз в 3 дня.
 * Если пробег давно не обновлялся — мягко просит его обновить.
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val repo = app.repo
        val force = inputData.getBoolean(KEY_FORCE, false)
        val today = Dates.today()
        var shown = 0

        for (car in repo.db.cars().getAll()) {
            val (km, readings) = repo.snapshot(car)
            val kpd = Calc.kmPerDay(readings)
            val reminders = repo.db.reminders().getForCar(car.id).filter { it.enabled }
            for (r in reminders) {
                val st = Calc.reminderStatus(r, km, today, kpd)
                if (st.state != DueState.OVERDUE && st.state != DueState.SOON) continue
                val last = r.lastNotified
                val due = when {
                    force -> true
                    last == null -> true
                    st.state == DueState.OVERDUE -> today - last >= 3 * Dates.DAY
                    else -> false
                }
                if (!due) continue
                val title = if (st.state == DueState.OVERDUE) "Пора: ${r.title}" else "Скоро: ${r.title}"
                Notifications.show(applicationContext, (r.id % 100_000).toInt() + 1000, title,
                    "${car.name}. ${st.describe()}")
                repo.db.reminders().update(r.copy(lastNotified = today))
                shown++
            }

            // Напоминание обновить пробег
            val hasKmReminders = reminders.any { (it.intervalKm ?: 0) > 0 }
            val lastReading = readings.maxOfOrNull { it.first }
            val nudgeKey = "nudge_${car.id}"
            val lastNudge = repo.prefs.getLong(nudgeKey, 0L)
            if (hasKmReminders && lastReading != null && today - lastReading >= 14 * Dates.DAY &&
                today - lastNudge >= 7 * Dates.DAY
            ) {
                Notifications.show(applicationContext, 500_000 + (car.id % 100_000).toInt(),
                    "Обновите пробег: ${car.name}",
                    "Последние данные ${Dates.format(lastReading)} (${Fmt.km(km)}). Так прогноз замен будет точнее.",
                    openTab = "home")
                repo.prefs.edit().putLong(nudgeKey, today).apply()
            }
        }

        if (force && shown == 0) {
            Notifications.show(applicationContext, 999, "Всё в порядке",
                "Ближайших замен и сроков нет.")
        }
        return Result.success()
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
