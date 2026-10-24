package com.autojournal.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val plate: String = "",
    val vin: String = "",
    val fuelType: String = "АИ-95",
    val tankVolume: Double? = null,
    val initialOdometer: Int = 0,
    val photoPath: String? = null,
    val note: String = "",
)

@Entity(
    tableName = "refuels",
    foreignKeys = [ForeignKey(
        entity = Car::class, parentColumns = ["id"], childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class Refuel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val date: Long,
    val odometer: Int,
    val liters: Double,
    val pricePerLiter: Double,
    val total: Double,
    val fullTank: Boolean = true,
    /** Пропущена предыдущая заправка — расход по этому отрезку не считаем. */
    val missedPrevious: Boolean = false,
    val station: String = "",
    val fuelGrade: String = "",
    val note: String = "",
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "СТО",
    val phone: String = "",
    val phone2: String = "",
    val address: String = "",
    val note: String = "",
)

@Entity(
    tableName = "services",
    foreignKeys = [
        ForeignKey(
            entity = Car::class, parentColumns = ["id"], childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Contact::class, parentColumns = ["id"], childColumns = ["contactId"],
            onDelete = ForeignKey.SET_NULL
        ),
    ],
    indices = [Index("carId"), Index("contactId")]
)
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val date: Long,
    val odometer: Int,
    val category: String,
    val title: String = "",
    /** Стоимость работ. */
    val laborCost: Double = 0.0,
    val contactId: Long? = null,
    /** Где делали, если не выбран контакт. */
    val place: String = "",
    val note: String = "",
    val photoPath: String? = null,
)

@Entity(
    tableName = "service_parts",
    foreignKeys = [ForeignKey(
        entity = ServiceRecord::class, parentColumns = ["id"], childColumns = ["recordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("recordId")]
)
data class ServicePart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val name: String,
    val partNumber: String = "",
    val price: Double = 0.0,
    val quantity: Double = 1.0,
)

data class ServiceWithParts(
    @Embedded val record: ServiceRecord,
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val parts: List<ServicePart>,
)

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = Car::class, parentColumns = ["id"], childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val title: String,
    val category: String,
    val intervalKm: Int? = null,
    val intervalDays: Int? = null,
    val lastKm: Int? = null,
    val lastDate: Long? = null,
    val warnKm: Int = 500,
    val warnDays: Int = 7,
    val enabled: Boolean = true,
    val lastNotified: Long? = null,
)

@Entity(
    tableName = "odometer_logs",
    foreignKeys = [ForeignKey(
        entity = Car::class, parentColumns = ["id"], childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class OdometerLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val date: Long,
    val odometer: Int,
)

fun ServicePart.sum(): Double = price * quantity
fun ServiceWithParts.partsTotal(): Double = parts.sumOf { it.sum() }
fun ServiceWithParts.total(): Double = record.laborCost + partsTotal()
