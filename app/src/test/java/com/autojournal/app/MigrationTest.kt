package com.autojournal.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.autojournal.app.data.AppDatabase
import com.autojournal.app.data.CarDocument
import com.autojournal.app.data.TireSet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Проверяет, что база версии 1 (как у установленного приложения) обновляется до версии 2
 * без потери данных и проходит проверку схемы Room.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MigrationTest {

    private val v2Tables = setOf("documents", "tire_sets")

    @Test
    fun migrate1to2_keepsData() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()

        // Точный SQL таблиц версии 1 берём у самого Room (эти таблицы не менялись).
        val ref = Room.databaseBuilder(ctx, AppDatabase::class.java, "ref.db").allowMainThreadQueries().build()
        val sql = mutableListOf<String>()
        ref.openHelper.readableDatabase.query(
            "SELECT tbl_name, sql FROM sqlite_master WHERE sql IS NOT NULL AND name NOT LIKE 'sqlite_%' " +
                "AND name NOT IN ('room_master_table', 'android_metadata') ORDER BY CASE type WHEN 'table' THEN 0 ELSE 1 END"
        ).use { c -> while (c.moveToNext()) if (c.getString(0) !in v2Tables) sql += c.getString(1) }
        ref.close()

        val file = ctx.getDatabasePath("old.db")
        file.parentFile?.mkdirs()
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            sql.forEach { old.execSQL(it) }
            old.execSQL(
                "INSERT INTO cars (id,name,make,model,year,plate,vin,fuelType,tankVolume,initialOdometer,photoPath,note) " +
                    "VALUES (1,'Солярис','Hyundai','Solaris',2019,'А123ВС','','АИ-95',50.0,120000,NULL,'')"
            )
            old.execSQL(
                "INSERT INTO refuels (id,carId,date,odometer,liters,pricePerLiter,total,fullTank,missedPrevious,station,fuelGrade,note) " +
                    "VALUES (1,1,0,120500,40.0,56.2,2248.0,1,0,'Лукойл','АИ-95','')"
            )
            old.execSQL(
                "INSERT INTO reminders (id,carId,title,category,intervalKm,intervalDays,lastKm,lastDate,warnKm,warnDays,enabled,lastNotified) " +
                    "VALUES (1,1,'Масло','Моторное масло',10000,365,115000,0,500,7,1,NULL)"
            )
            old.version = 1
        }

        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, "old.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        assertEquals(1, db.cars().getAll().size)
        assertEquals("Солярис", db.cars().getAll().first().name)
        assertEquals(1, db.refuels().getAll().size)
        assertEquals(1, db.reminders().getAll().size)

        db.documents().insert(CarDocument(carId = 1, type = "ОСАГО", expires = 1000L))
        db.tires().insert(TireSet(carId = 1, name = "Зима", season = "Зима", studded = true))
        assertEquals(1, db.documents().getAll().size)
        assertEquals(1, db.tires().getAll().size)
        db.close()
    }
}
