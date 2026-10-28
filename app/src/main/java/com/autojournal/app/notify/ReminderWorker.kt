package com.autojournal.app.notify

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autojournal.app.App
import com.autojournal.app.data.Backup
import com.autojournal.app.domain.Calc
import com.autojournal.app.domain.DueState
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import com.autojournal.app.widget.CarWidgetProvider

/**
 * Дважды в сутки:
 *  • напоминания о заменах («скоро» — один раз, «просрочено» — раз в 3 дня);
 *  • сроки документов (за 30 дней, за 7 дней, истёк);
 *  • просьба обновить пробег, если он давно не вводился;
 *  • автокопия раз в неделю в выбранную папку;
 *  • обновление виджета.
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

            // Документы
            repo.db.documents().getAll().filter { it.carId == car.id }.forEach { d ->
                val exp = d.expires ?: return@forEach
                val days = Math.floorDiv(exp - today, Dates.DAY).toInt()
                val stage = when {
                    days < 0 -> 3
                    days <= 7 -> 2
                    days <= 30 -> 1
                    else -> 0
                }
                if (stage > d.notifiedStage || (force && stage > 0)) {
                    val text = when (stage) {
                        3 -> "Срок истёк ${Dates.format(exp)}"
                        else -> "Действует до ${Dates.format(exp)} — осталось ${Fmt.days(days)}"
                    }
                    Notifications.show(applicationContext, 300_000 + (d.id % 100_000).toInt(),
                        "${d.type}: ${car.name}", text, openTab = "documents")
                    repo.db.documents().update(d.copy(notifiedStage = stage))
                    shown++
                }
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
                    openTab = "odometer")
                repo.prefs.edit().putLong(nudgeKey, today).apply()
            }
        }

        autoBackup(app)
        CarWidgetProvider.requestUpdate(applicationContext)

        if (force && shown == 0) {
            Notifications.show(applicationContext, 999, "Всё в порядке",
                "Ближайших замен и сроков нет.")
        }
        return Result.success()
    }

    private suspend fun autoBackup(app: App) {
        val prefs = app.repo.prefs
        val uri = prefs.getString(KEY_AUTO_URI, null) ?: return
        val last = prefs.getLong(KEY_AUTO_LAST, 0L)
        if (System.currentTimeMillis() - last < 7 * Dates.DAY) return
        runCatching { Backup.autoBackupToFolder(app, app.db, Uri.parse(uri)) }
            .onSuccess { prefs.edit().putLong(KEY_AUTO_LAST, System.currentTimeMillis()).remove(KEY_AUTO_ERROR).apply() }
            .onFailure { prefs.edit().putString(KEY_AUTO_ERROR, it.message ?: "ошибка").apply() }
    }

    companion object {
        const val KEY_FORCE = "force"
        const val KEY_AUTO_URI = "auto_backup_uri"
        const val KEY_AUTO_LAST = "auto_backup_last"
        const val KEY_AUTO_ERROR = "auto_backup_error"
    }
}
