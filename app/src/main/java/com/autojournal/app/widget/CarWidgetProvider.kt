package com.autojournal.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.autojournal.app.App
import com.autojournal.app.MainActivity
import com.autojournal.app.R
import com.autojournal.app.domain.Calc
import com.autojournal.app.domain.DueState
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Виджет: выбранная машина, пробег, ближайшая замена и кнопка «+ Заправка». */
class CarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                render(context.applicationContext, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var debounce: Job? = null

        /** Обновить все виджеты (с задержкой, чтобы не дёргать на каждое изменение). */
        fun requestUpdate(context: Context) {
            val ctx = context.applicationContext
            debounce?.cancel()
            debounce = scope.launch {
                delay(600)
                val manager = AppWidgetManager.getInstance(ctx)
                val ids = manager.getAppWidgetIds(ComponentName(ctx, CarWidgetProvider::class.java))
                if (ids.isNotEmpty()) render(ctx, manager, ids)
            }
        }

        private fun open(ctx: Context, action: String?, code: Int): PendingIntent {
            val i = Intent(ctx, MainActivity::class.java).apply {
                if (action != null) this.action = action
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private suspend fun render(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
            val app = ctx as? App ?: return
            val repo = app.repo
            val cars = repo.db.cars().getAll()
            val car = cars.firstOrNull { it.id == repo.activeCarId.value } ?: cars.firstOrNull()
            val views = RemoteViews(ctx.packageName, R.layout.widget_car)

            if (car == null) {
                views.setTextViewText(R.id.w_car, "АвтоЖурнал")
                views.setTextViewText(R.id.w_km, "Нет машины")
                views.setTextViewText(R.id.w_title, "Добавьте автомобиль в приложении")
                views.setTextViewText(R.id.w_desc, "")
                views.setProgressBar(R.id.w_progress, 100, 0, false)
            } else {
                val (km, readings) = repo.snapshot(car)
                val today = Dates.today()
                val kpd = Calc.kmPerDay(readings)
                val top = Calc.sortStatuses(
                    repo.db.reminders().getForCar(car.id).map { Calc.reminderStatus(it, km, today, kpd) }
                ).firstOrNull { it.state != DueState.OFF }
                views.setTextViewText(R.id.w_car, car.name.uppercase())
                views.setTextViewText(R.id.w_km, Fmt.km(km))
                if (top == null) {
                    views.setTextViewText(R.id.w_title, "Напоминаний нет")
                    views.setTextViewText(R.id.w_desc, "")
                    views.setProgressBar(R.id.w_progress, 100, 0, false)
                } else {
                    views.setTextViewText(R.id.w_title, top.reminder.title)
                    views.setTextViewText(R.id.w_desc, top.describe())
                    val color = when (top.state) {
                        DueState.OVERDUE -> 0xFFFF5A4E.toInt()
                        DueState.SOON -> 0xFFFF9F1A.toInt()
                        else -> 0xFF4CC38A.toInt()
                    }
                    views.setTextColor(R.id.w_desc, color)
                    views.setProgressBar(R.id.w_progress, 100, (top.progress * 100).toInt(), false)
                }
            }
            views.setOnClickPendingIntent(R.id.widget_root, open(ctx, null, 7001))
            views.setOnClickPendingIntent(R.id.w_refuel, open(ctx, MainActivity.ACTION_NEW_REFUEL, 7002))
            manager.updateAppWidget(ids, views)
        }
    }
}
