package com.autojournal.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Car::class, Refuel::class, Contact::class, ServiceRecord::class,
        ServicePart::class, Reminder::class, OdometerLog::class,
        CarDocument::class, TireSet::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cars(): CarDao
    abstract fun refuels(): RefuelDao
    abstract fun services(): ServiceDao
    abstract fun contacts(): ContactDao
    abstract fun reminders(): ReminderDao
    abstract fun odometer(): OdometerDao
    abstract fun documents(): DocumentDao
    abstract fun tires(): TireDao

    companion object {
        const val NAME = "autojournal.db"

        /** Версия 2: документы и комплекты шин. Существующие данные не трогаем. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `documents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`carId` INTEGER NOT NULL, `type` TEXT NOT NULL, `number` TEXT NOT NULL, " +
                        "`issued` INTEGER, `expires` INTEGER, `photo1` TEXT, `photo2` TEXT, `note` TEXT NOT NULL, " +
                        "`notifiedStage` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_carId` ON `documents` (`carId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tire_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`carId` INTEGER NOT NULL, `name` TEXT NOT NULL, `season` TEXT NOT NULL, " +
                        "`brand` TEXT NOT NULL, `size` TEXT NOT NULL, `studded` INTEGER NOT NULL, `year` INTEGER, " +
                        "`pressureFront` REAL, `pressureRear` REAL, `treadMm` REAL, `storage` TEXT NOT NULL, " +
                        "`price` REAL NOT NULL, `purchaseDate` INTEGER, `installed` INTEGER NOT NULL, " +
                        "`installedAtKm` INTEGER, `accumulatedKm` INTEGER NOT NULL, `photoPath` TEXT, " +
                        "`note` TEXT NOT NULL, " +
                        "FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tire_sets_carId` ON `tire_sets` (`carId`)")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
