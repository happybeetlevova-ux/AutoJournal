package com.autojournal.app

import android.app.Application
import com.autojournal.app.data.AppDatabase
import com.autojournal.app.data.Repository
import com.autojournal.app.notify.Notifications
import com.autojournal.app.notify.ReminderScheduler

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
    }
}
