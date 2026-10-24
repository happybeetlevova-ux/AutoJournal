package com.autojournal.app.domain

import com.autojournal.app.data.Reminder

object Categories {
    const val FUEL = "Топливо"

    val service = listOf(
        "ТО (плановое)",
        "Моторное масло",
        "Масляный фильтр",
        "Воздушный фильтр",
        "Салонный фильтр",
        "Топливный фильтр",
        "Свечи зажигания",
        "Тормозные колодки",
        "Тормозные диски",
        "Тормозная жидкость",
        "Антифриз",
        "Масло КПП",
        "ГРМ (ремень / цепь)",
        "Ремень навесного",
        "Аккумулятор",
        "Шины / шиномонтаж",
        "Щётки стеклоочистителя",
        "Подвеска",
        "Диагностика",
        "Ремонт",
        "Кузов / покраска",
        "Мойка",
        "Страховка ОСАГО / КАСКО",
        "Техосмотр",
        "Налог",
        "Штраф",
        "Парковка / платные дороги",
        "Прочее",
    )

    val fuelGrades = listOf("АИ-92", "АИ-95", "АИ-98", "АИ-100", "ДТ", "Газ (пропан)", "Метан", "Электро")

    val contactTypes = listOf(
        "СТО", "Мастер", "Магазин запчастей", "АЗС", "Шиномонтаж",
        "Мойка", "Эвакуатор", "Страховая", "Дилер", "Другое",
    )

    /** Типовой регламент для нового автомобиля. */
    fun defaultReminders(carId: Long, km: Int, today: Long): List<Reminder> = listOf(
        Reminder(carId = carId, title = "Замена масла и масляного фильтра", category = "Моторное масло",
            intervalKm = 10_000, intervalDays = 365, lastKm = km, lastDate = today),
        Reminder(carId = carId, title = "Воздушный фильтр", category = "Воздушный фильтр",
            intervalKm = 20_000, intervalDays = 730, lastKm = km, lastDate = today),
        Reminder(carId = carId, title = "Салонный фильтр", category = "Салонный фильтр",
            intervalKm = 15_000, intervalDays = 365, lastKm = km, lastDate = today),
        Reminder(carId = carId, title = "Свечи зажигания", category = "Свечи зажигания",
            intervalKm = 30_000, lastKm = km, lastDate = today),
        Reminder(carId = carId, title = "Тормозная жидкость", category = "Тормозная жидкость",
            intervalDays = 730, lastKm = km, lastDate = today, warnDays = 14),
        Reminder(carId = carId, title = "Проверка тормозных колодок", category = "Тормозные колодки",
            intervalKm = 15_000, lastKm = km, lastDate = today),
        Reminder(carId = carId, title = "Антифриз", category = "Антифриз",
            intervalKm = 60_000, intervalDays = 1095, lastKm = km, lastDate = today, warnDays = 14),
        Reminder(carId = carId, title = "Масло КПП", category = "Масло КПП",
            intervalKm = 60_000, lastKm = km, lastDate = today, warnKm = 1000),
        Reminder(carId = carId, title = "Ремень / цепь ГРМ", category = "ГРМ (ремень / цепь)",
            intervalKm = 60_000, lastKm = km, lastDate = today, warnKm = 2000),
        Reminder(carId = carId, title = "Сезонная смена шин", category = "Шины / шиномонтаж",
            intervalDays = 182, lastKm = km, lastDate = today, warnDays = 10),
        Reminder(carId = carId, title = "Продление ОСАГО", category = "Страховка ОСАГО / КАСКО",
            intervalDays = 365, lastKm = km, lastDate = today, warnDays = 14),
    )
}
