package com.autojournal.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Car::class, Refuel::class, Contact::class, ServiceRecord::class,
        ServicePart::class, Reminder::class, OdometerLog::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cars(): CarDao
    abstract fun refuels(): RefuelDao
    abstract fun services(): ServiceDao
    abstract fun contacts(): ContactDao
    abstract fun reminders(): ReminderDao
    abstract fun odometer(): OdometerDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "autojournal.db").build()
    }
}
