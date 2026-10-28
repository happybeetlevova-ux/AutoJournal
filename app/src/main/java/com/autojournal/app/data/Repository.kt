package com.autojournal.app.data

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.autojournal.app.domain.Calc
import com.autojournal.app.domain.Categories
import com.autojournal.app.ui.theme.AppStyle
import com.autojournal.app.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Repository(val db: AppDatabase, val prefs: SharedPreferences) {

    private val _activeCarId = MutableStateFlow(prefs.getLong(KEY_ACTIVE, 0L))
    val activeCarId: StateFlow<Long> = _activeCarId

    /** Вызывается при смене выбранного авто (обновление виджета). */
    var onActiveCarChanged: (() -> Unit)? = null

    fun setActiveCar(id: Long) {
        prefs.edit().putLong(KEY_ACTIVE, id).apply()
        _activeCarId.value = id
        onActiveCarChanged?.invoke()
    }

    private val _style = MutableStateFlow(
        runCatching { AppStyle.valueOf(prefs.getString(KEY_STYLE, null) ?: "") }.getOrDefault(AppStyle.GARAGE)
    )
    val style: StateFlow<AppStyle> = _style

    fun setStyle(s: AppStyle) {
        prefs.edit().putString(KEY_STYLE, s.name).apply()
        _style.value = s
    }

    val cars: Flow<List<Car>> = db.cars().observeAll()
    val contacts: Flow<List<Contact>> = db.contacts().observeAll()
    fun refuels(carId: Long): Flow<List<Refuel>> = db.refuels().observeForCar(carId)
    fun services(carId: Long): Flow<List<ServiceWithParts>> = db.services().observeForCar(carId)
    fun reminders(carId: Long): Flow<List<Reminder>> = db.reminders().observeForCar(carId)
    fun odometerLogs(carId: Long): Flow<List<OdometerLog>> = db.odometer().observeForCar(carId)
    fun documents(carId: Long): Flow<List<CarDocument>> = db.documents().observeForCar(carId)
    fun tires(carId: Long): Flow<List<TireSet>> = db.tires().observeForCar(carId)
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

    // ---------- Документы ----------
    suspend fun saveDocument(d: CarDocument) {
        if (d.id == 0L) db.documents().insert(d) else {
            val old = db.documents().get(d.id)
            // Новый срок действия — уведомлять заново.
            val stage = if (old?.expires != d.expires) 0 else d.notifiedStage
            db.documents().update(d.copy(notifiedStage = stage))
        }
    }

    suspend fun deleteDocument(d: CarDocument) = db.documents().delete(d)

    // ---------- Шины ----------
    suspend fun saveTireSet(t: TireSet) {
        if (t.id == 0L) db.tires().insert(t) else db.tires().update(t)
    }

    suspend fun deleteTireSet(t: TireSet) = db.tires().delete(t)

    /** Ставит комплект на машину; пробег снятых комплектов копится. */
    suspend fun installTireSet(set: TireSet, km: Int) {
        db.withTransaction {
            db.tires().getForCar(set.carId).filter { it.installed && it.id != set.id }.forEach { removeInternal(it, km) }
            val fresh = db.tires().get(set.id) ?: set
            if (!fresh.installed) db.tires().update(fresh.copy(installed = true, installedAtKm = km))
        }
    }

    suspend fun removeTireSet(set: TireSet, km: Int) {
        db.withTransaction {
            val fresh = db.tires().get(set.id) ?: set
            removeInternal(fresh, km)
        }
    }

    private suspend fun removeInternal(t: TireSet, km: Int) {
        db.tires().update(t.copy(installed = false, installedAtKm = null, accumulatedKm = t.mileage(km)))
    }

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
        private const val KEY_STYLE = "style"
    }
}
