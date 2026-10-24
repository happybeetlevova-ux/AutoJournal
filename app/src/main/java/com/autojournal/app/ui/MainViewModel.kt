package com.autojournal.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autojournal.app.App
import com.autojournal.app.data.Car
import com.autojournal.app.data.Contact
import com.autojournal.app.data.OdometerLog
import com.autojournal.app.data.Refuel
import com.autojournal.app.data.Reminder
import com.autojournal.app.data.Repository
import com.autojournal.app.data.ServiceWithParts
import com.autojournal.app.domain.Calc
import com.autojournal.app.domain.ReminderStatus
import com.autojournal.app.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo: Repository = (app as App).repo

    /** null — ещё загружается. */
    val cars: StateFlow<List<Car>?> =
        repo.cars.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeCar: StateFlow<Car?> = combine(cars, repo.activeCarId) { list, id ->
        list?.firstOrNull { it.id == id } ?: list?.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val carId = activeCar.map { it?.id ?: 0L }.distinctUntilChanged()

    val refuels: StateFlow<List<Refuel>> = carId.flatMapLatest { repo.refuels(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val services: StateFlow<List<ServiceWithParts>> = carId.flatMapLatest { repo.services(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val reminders: StateFlow<List<Reminder>> = carId.flatMapLatest { repo.reminders(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val odometerLogs: StateFlow<List<OdometerLog>> = carId.flatMapLatest { repo.odometerLogs(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val contacts: StateFlow<List<Contact>> =
        repo.contacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val readings: StateFlow<List<Pair<Long, Int>>> =
        combine(activeCar, refuels, services, odometerLogs) { car, r, s, l ->
            if (car == null) emptyList() else Calc.readings(car, r, s.map { it.record }, l)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentKm: StateFlow<Int> =
        combine(activeCar, refuels, services, odometerLogs) { car, r, s, l ->
            if (car == null) 0 else Calc.currentKm(car, r, s.map { it.record }, l)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val kmPerDay: StateFlow<Double?> = readings.map { Calc.kmPerDay(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val reminderStatuses: StateFlow<List<ReminderStatus>> =
        combine(reminders, currentKm, kmPerDay) { rs, km, kpd ->
            val t = Dates.today()
            Calc.sortStatuses(rs.map { Calc.reminderStatus(it, km, t, kpd) })
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun io(block: suspend Repository.() -> Unit) {
        viewModelScope.launch { repo.block() }
    }
}
