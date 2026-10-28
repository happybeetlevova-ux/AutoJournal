package com.autojournal.app

import android.app.Application
import androidx.room.InvalidationTracker
import com.autojournal.app.data.AppDatabase
import com.autojournal.app.data.Repository
import com.autojournal.app.notify.Notifications
import com.autojournal.app.notify.ReminderScheduler
import com.autojournal.app.widget.CarWidgetProvider

class App : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.build(this)
        repo = Repository(db, getSharedPreferences("autojournal", MODE_PRIVATE))
        Notifications.createChannel(this)
        ReminderScheduler.schedule(this)

        // Виджет обновляется сам при любом изменении данных или смене машины.
        db.invalidationTracker.addObserver(object : InvalidationTracker.Observer(
            arrayOf("cars", "refuels", "services", "reminders", "odometer_logs")
        ) {
            override fun onInvalidated(tables: Set<String>) = CarWidgetProvider.requestUpdate(this@App)
        })
        repo.onActiveCarChanged = { CarWidgetProvider.requestUpdate(this) }
    }
}
