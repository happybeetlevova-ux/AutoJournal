package com.autojournal.app.data

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.autojournal.app.domain.Calc
import com.autojournal.app.domain.Categories
import com.autojournal.app.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Repository(val db: AppDatabase, val prefs: SharedPreferences) {

    private val _activeCarId = MutableStateFlow(prefs.getLong(KEY_ACTIVE, 0L))
    val activeCarId: StateFlow<Long> = _activeCarId

    fun setActiveCar(id: Long) {
        prefs.edit().putLong(KEY_ACTIVE, id).apply()
        _activeCarId.value = id
    }

    val cars: Flow<List<Car>> = db.cars().observeAll()
    val contacts: Flow<List<Contact>> = db.contacts().observeAll()
    fun refuels(carId: Long): Flow<List<Refuel>> = db.refuels().observeForCar(carId)
    fun services(carId: Long): Flow<List<ServiceWithParts>> = db.services().observeForCar(carId)
    fun reminders(carId: Long): Flow<List<Reminder>> = db.reminders().observeForCar(carId)
    fun odometerLogs(carId: Long): Flow<List<OdometerLog>> = db.odometer().observeForCar(carId)
    fun servicesByContact(contactId: Long): Flow<List<ServiceWithParts>> =
        db.services().observeByContact(contactId)

    // ---------- Автомобили ----------
    suspend fun saveCar(car: Car, addDefaultReminders: Boolean): Long {
        val id = if (car.id == 0L) db.cars().insert(car) else {
            db.cars().update(car); car.id
        }
        if (addDefaultReminders) {
            db.reminders().insertMany(Categories.defaultReminders(id, car.initialOdometer, Dates.today()))
        }
        return id
    }

    suspend fun deleteCar(car: Car) = db.cars().delete(car)

    // ---------- Заправки ----------
    suspend fun saveRefuel(r: Refuel) {
        if (r.id == 0L) db.refuels().insert(r) else db.refuels().update(r)
    }

    suspend fun deleteRefuel(r: Refuel) = db.refuels().delete(r)

    // ---------- Сервис ----------
    /**
     * Сохраняет запись и запчасти. Напоминания той же категории автоматически
     * сбрасываются на пробег/дату этой записи.
     */
    suspend fun saveService(record: ServiceRecord, parts: List<ServicePart>): Long = db.withTransaction {
        val id = if (record.id == 0L) db.services().insert(record) else {
            db.services().update(record); record.id
        }
        db.services().deleteParts(id)
        if (parts.isNotEmpty()) db.services().insertParts(parts.map { it.copy(id = 0, recordId = id) })
        db.reminders().getForCar(record.carId)
            .filter { it.category == record.category }
            .forEach { r ->
                val last = r.lastDate
                if (last == null || record.date >= last) {
                    db.reminders().update(
                        r.copy(lastKm = record.odometer, lastDate = record.date, lastNotified = null)
                    )
                }
            }
        id
    }

    suspend fun deleteService(record: ServiceRecord) = db.services().delete(record)

    // ---------- Контакты ----------
    suspend fun saveContact(c: Contact): Long =
        if (c.id == 0L) db.contacts().insert(c) else {
            db.contacts().update(c); c.id
        }

    suspend fun deleteContact(c: Contact) = db.contacts().delete(c)

    // ---------- Напоминания ----------
    suspend fun saveReminder(r: Reminder) {
        if (r.id == 0L) db.reminders().insert(r) else db.reminders().update(r)
    }

    suspend fun deleteReminder(r: Reminder) = db.reminders().delete(r)

    suspend fun markReminderDone(r: Reminder, km: Int, date: Long) =
        db.reminders().update(r.copy(lastKm = km, lastDate = date, lastNotified = null))

    suspend fun addDefaultReminders(carId: Long, km: Int) =
        db.reminders().insertMany(Categories.defaultReminders(carId, km, Dates.today()))

    // ---------- Пробег ----------
    suspend fun addOdometer(carId: Long, km: Int, date: Long = Dates.today()) {
        db.odometer().insert(OdometerLog(carId = carId, date = date, odometer = km))
    }

    /** Снимок данных по авто для фоновых проверок. */
    suspend fun snapshot(car: Car): Pair<Int, List<Pair<Long, Int>>> {
        val refuels = db.refuels().getForCar(car.id)
        val services = db.services().getForCar(car.id).map { it.record }
        val logs = db.odometer().getForCar(car.id)
        val km = Calc.currentKm(car, refuels, services, logs)
        return km to Calc.readings(car, refuels, services, logs)
    }

    companion object {
        private const val KEY_ACTIVE = "active_car"
    }
}
