package com.autojournal.app.data

import androidx.room.withTransaction
import com.autojournal.app.util.Dates
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Резервная копия всех данных в JSON и экспорт в CSV для Excel. */
object Backup {

    private fun JSONObject.putN(key: String, v: Any?): JSONObject = put(key, v ?: JSONObject.NULL)
    private fun JSONObject.longOrNull(k: String): Long? = if (!has(k) || isNull(k)) null else getLong(k)
    private fun JSONObject.intOrNull(k: String): Int? = if (!has(k) || isNull(k)) null else getInt(k)
    private fun JSONObject.doubleOrNull(k: String): Double? = if (!has(k) || isNull(k)) null else getDouble(k)
    private fun JSONObject.str(k: String): String = if (!has(k) || isNull(k)) "" else getString(k)
    private fun JSONObject.strOrNull(k: String): String? = if (!has(k) || isNull(k)) null else getString(k)

    private inline fun <T> JSONArray.mapObjects(f: (JSONObject) -> T): List<T> =
        (0 until length()).map { f(getJSONObject(it)) }

    suspend fun exportJson(db: AppDatabase): String {
        val root = JSONObject()
        root.put("app", "AutoJournal")
        root.put("version", 1)
        root.put("exported", System.currentTimeMillis())
        root.put("cars", JSONArray().apply {
            db.cars().getAll().forEach { c ->
                put(JSONObject().put("id", c.id).put("name", c.name).put("make", c.make).put("model", c.model)
                    .putN("year", c.year).put("plate", c.plate).put("vin", c.vin).put("fuelType", c.fuelType)
                    .putN("tankVolume", c.tankVolume).put("initialOdometer", c.initialOdometer)
                    .putN("photoPath", c.photoPath).put("note", c.note))
            }
        })
        root.put("refuels", JSONArray().apply {
            db.refuels().getAll().forEach { r ->
                put(JSONObject().put("id", r.id).put("carId", r.carId).put("date", r.date).put("odometer", r.odometer)
                    .put("liters", r.liters).put("pricePerLiter", r.pricePerLiter).put("total", r.total)
                    .put("fullTank", r.fullTank).put("missedPrevious", r.missedPrevious).put("station", r.station)
                    .put("fuelGrade", r.fuelGrade).put("note", r.note))
            }
        })
        root.put("contacts", JSONArray().apply {
            db.contacts().getAll().forEach { c ->
                put(JSONObject().put("id", c.id).put("name", c.name).put("type", c.type).put("phone", c.phone)
                    .put("phone2", c.phone2).put("address", c.address).put("note", c.note))
            }
        })
        root.put("services", JSONArray().apply {
            db.services().getAllRecords().forEach { s ->
                put(JSONObject().put("id", s.id).put("carId", s.carId).put("date", s.date).put("odometer", s.odometer)
                    .put("category", s.category).put("title", s.title).put("laborCost", s.laborCost)
                    .putN("contactId", s.contactId).put("place", s.place).put("note", s.note)
                    .putN("photoPath", s.photoPath))
            }
        })
        root.put("parts", JSONArray().apply {
            db.services().getAllParts().forEach { p ->
                put(JSONObject().put("id", p.id).put("recordId", p.recordId).put("name", p.name)
                    .put("partNumber", p.partNumber).put("price", p.price).put("quantity", p.quantity))
            }
        })
        root.put("reminders", JSONArray().apply {
            db.reminders().getAll().forEach { r ->
                put(JSONObject().put("id", r.id).put("carId", r.carId).put("title", r.title).put("category", r.category)
                    .putN("intervalKm", r.intervalKm).putN("intervalDays", r.intervalDays).putN("lastKm", r.lastKm)
                    .putN("lastDate", r.lastDate).put("warnKm", r.warnKm).put("warnDays", r.warnDays)
                    .put("enabled", r.enabled))
            }
        })
        root.put("odometer", JSONArray().apply {
            db.odometer().getAll().forEach { o ->
                put(JSONObject().put("id", o.id).put("carId", o.carId).put("date", o.date).put("odometer", o.odometer))
            }
        })
        return root.toString(2)
    }

    /** Полностью заменяет данные содержимым копии. Возвращает число автомобилей. */
    suspend fun importJson(db: AppDatabase, json: String): Int {
        val root = JSONObject(json)
        require(root.optString("app") == "AutoJournal") { "Это не резервная копия АвтоЖурнала" }
        fun photo(p: String?): String? = p?.takeIf { File(it).exists() }

        val cars = root.optJSONArray("cars")?.mapObjects { o ->
            Car(o.getLong("id"), o.str("name"), o.str("make"), o.str("model"), o.intOrNull("year"),
                o.str("plate"), o.str("vin"), o.str("fuelType"), o.doubleOrNull("tankVolume"),
                o.optInt("initialOdometer"), photo(o.strOrNull("photoPath")), o.str("note"))
        } ?: emptyList()
        val refuels = root.optJSONArray("refuels")?.mapObjects { o ->
            Refuel(o.getLong("id"), o.getLong("carId"), o.getLong("date"), o.getInt("odometer"),
                o.getDouble("liters"), o.getDouble("pricePerLiter"), o.getDouble("total"),
                o.optBoolean("fullTank", true), o.optBoolean("missedPrevious"), o.str("station"),
                o.str("fuelGrade"), o.str("note"))
        } ?: emptyList()
        val contacts = root.optJSONArray("contacts")?.mapObjects { o ->
            Contact(o.getLong("id"), o.str("name"), o.str("type"), o.str("phone"), o.str("phone2"),
                o.str("address"), o.str("note"))
        } ?: emptyList()
        val services = root.optJSONArray("services")?.mapObjects { o ->
            ServiceRecord(o.getLong("id"), o.getLong("carId"), o.getLong("date"), o.getInt("odometer"),
                o.str("category"), o.str("title"), o.optDouble("laborCost", 0.0), o.longOrNull("contactId"),
                o.str("place"), o.str("note"), photo(o.strOrNull("photoPath")))
        } ?: emptyList()
        val parts = root.optJSONArray("parts")?.mapObjects { o ->
            ServicePart(o.getLong("id"), o.getLong("recordId"), o.str("name"), o.str("partNumber"),
                o.optDouble("price", 0.0), o.optDouble("quantity", 1.0))
        } ?: emptyList()
        val reminders = root.optJSONArray("reminders")?.mapObjects { o ->
            Reminder(o.getLong("id"), o.getLong("carId"), o.str("title"), o.str("category"),
                o.intOrNull("intervalKm"), o.intOrNull("intervalDays"), o.intOrNull("lastKm"),
                o.longOrNull("lastDate"), o.optInt("warnKm", 500), o.optInt("warnDays", 7),
                o.optBoolean("enabled", true), null)
        } ?: emptyList()
        val logs = root.optJSONArray("odometer")?.mapObjects { o ->
            OdometerLog(o.getLong("id"), o.getLong("carId"), o.getLong("date"), o.getInt("odometer"))
        } ?: emptyList()

        db.withTransaction {
            db.services().deleteAllParts()
            db.services().deleteAllRecords()
            db.refuels().deleteAll()
            db.reminders().deleteAll()
            db.odometer().deleteAll()
            db.contacts().deleteAll()
            db.cars().deleteAll()
            db.cars().insertAll(cars)
            db.contacts().insertAll(contacts)
            db.refuels().insertAll(refuels)
            db.services().insertAllRecords(services)
            db.services().insertAllParts(parts)
            db.reminders().insertAll(reminders)
            db.odometer().insertAll(logs)
        }
        return cars.size
    }

    private fun csv(v: String): String = "\"" + v.replace("\"", "\"\"") + "\""
    private fun n(v: Double): String = String.format(java.util.Locale.US, "%.2f", v).replace('.', ',')

    /** CSV (разделитель «;», UTF-8 с BOM) — открывается в Excel без «кракозябр». */
    fun exportCsv(car: Car, refuels: List<Refuel>, services: List<ServiceWithParts>, contacts: List<Contact>): String {
        val sb = StringBuilder("﻿")
        sb.append("Автомобиль;").append(csv(car.name)).append("\n\n")
        sb.append("ЗАПРАВКИ\n")
        sb.append("Дата;Пробег;Топливо;Литры;Цена за литр;Сумма;Полный бак;АЗС;Заметка\n")
        refuels.sortedBy { it.date }.forEach { r ->
            sb.append(Dates.format(r.date)).append(';').append(r.odometer).append(';').append(csv(r.fuelGrade)).append(';')
                .append(n(r.liters)).append(';').append(n(r.pricePerLiter)).append(';').append(n(r.total)).append(';')
                .append(if (r.fullTank) "да" else "нет").append(';').append(csv(r.station)).append(';')
                .append(csv(r.note)).append('\n')
        }
        sb.append("\nОБСЛУЖИВАНИЕ И РАСХОДЫ\n")
        sb.append("Дата;Пробег;Категория;Описание;Работы;Запчасти;Итого;Где делали;Телефон;Запчасти (список);Заметка\n")
        services.sortedBy { it.record.date }.forEach { s ->
            val r = s.record
            val contact = contacts.firstOrNull { it.id == r.contactId }
            val partsSum = s.parts.sumOf { it.price * it.quantity }
            val partsList = s.parts.joinToString(", ") {
                it.name + (if (it.partNumber.isNotBlank()) " [${it.partNumber}]" else "") +
                    " ${n(it.quantity)}×${n(it.price)}"
            }
            sb.append(Dates.format(r.date)).append(';').append(r.odometer).append(';').append(csv(r.category)).append(';')
                .append(csv(r.title)).append(';').append(n(r.laborCost)).append(';').append(n(partsSum)).append(';')
                .append(n(r.laborCost + partsSum)).append(';').append(csv(contact?.name ?: r.place)).append(';')
                .append(csv(contact?.phone ?: "")).append(';').append(csv(partsList)).append(';')
                .append(csv(r.note)).append('\n')
        }
        return sb.toString()
    }
}
