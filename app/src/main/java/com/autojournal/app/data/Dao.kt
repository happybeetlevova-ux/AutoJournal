package com.autojournal.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY id")
    fun observeAll(): Flow<List<Car>>

    @Query("SELECT * FROM cars ORDER BY id")
    suspend fun getAll(): List<Car>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun get(id: Long): Car?

    @Insert
    suspend fun insert(item: Car): Long

    @Update
    suspend fun update(item: Car)

    @Delete
    suspend fun delete(item: Car)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Car>)

    @Query("DELETE FROM cars")
    suspend fun deleteAll()
}

@Dao
interface RefuelDao {
    @Query("SELECT * FROM refuels WHERE carId = :carId ORDER BY date DESC, odometer DESC")
    fun observeForCar(carId: Long): Flow<List<Refuel>>

    @Query("SELECT * FROM refuels WHERE carId = :carId")
    suspend fun getForCar(carId: Long): List<Refuel>

    @Query("SELECT * FROM refuels WHERE id = :id")
    suspend fun get(id: Long): Refuel?

    @Query("SELECT * FROM refuels")
    suspend fun getAll(): List<Refuel>

    @Insert
    suspend fun insert(item: Refuel): Long

    @Update
    suspend fun update(item: Refuel)

    @Delete
    suspend fun delete(item: Refuel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Refuel>)

    @Query("DELETE FROM refuels")
    suspend fun deleteAll()
}

@Dao
interface ServiceDao {
    @Transaction
    @Query("SELECT * FROM services WHERE carId = :carId ORDER BY date DESC, odometer DESC")
    fun observeForCar(carId: Long): Flow<List<ServiceWithParts>>

    @Transaction
    @Query("SELECT * FROM services WHERE carId = :carId")
    suspend fun getForCar(carId: Long): List<ServiceWithParts>

    @Transaction
    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun get(id: Long): ServiceWithParts?

    @Transaction
    @Query("SELECT * FROM services WHERE contactId = :contactId ORDER BY date DESC")
    fun observeByContact(contactId: Long): Flow<List<ServiceWithParts>>

    @Insert
    suspend fun insert(item: ServiceRecord): Long

    @Update
    suspend fun update(item: ServiceRecord)

    @Delete
    suspend fun delete(item: ServiceRecord)

    @Insert
    suspend fun insertParts(items: List<ServicePart>)

    @Query("DELETE FROM service_parts WHERE recordId = :recordId")
    suspend fun deleteParts(recordId: Long)

    @Query("SELECT * FROM services")
    suspend fun getAllRecords(): List<ServiceRecord>

    @Query("SELECT * FROM service_parts")
    suspend fun getAllParts(): List<ServicePart>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecords(items: List<ServiceRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllParts(items: List<ServicePart>)

    @Query("DELETE FROM services")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM service_parts")
    suspend fun deleteAllParts()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts")
    suspend fun getAll(): List<Contact>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun get(id: Long): Contact?

    @Insert
    suspend fun insert(item: Contact): Long

    @Update
    suspend fun update(item: Contact)

    @Delete
    suspend fun delete(item: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Contact>)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE carId = :carId ORDER BY id")
    fun observeForCar(carId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE carId = :carId")
    suspend fun getForCar(carId: Long): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): Reminder?

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<Reminder>

    @Insert
    suspend fun insert(item: Reminder): Long

    @Insert
    suspend fun insertMany(items: List<Reminder>)

    @Update
    suspend fun update(item: Reminder)

    @Delete
    suspend fun delete(item: Reminder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Reminder>)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_logs WHERE carId = :carId ORDER BY date DESC")
    fun observeForCar(carId: Long): Flow<List<OdometerLog>>

    @Query("SELECT * FROM odometer_logs WHERE carId = :carId")
    suspend fun getForCar(carId: Long): List<OdometerLog>

    @Query("SELECT * FROM odometer_logs")
    suspend fun getAll(): List<OdometerLog>

    @Insert
    suspend fun insert(item: OdometerLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OdometerLog>)

    @Query("DELETE FROM odometer_logs")
    suspend fun deleteAll()
}
