package com.autojournal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autojournal.app.data.Refuel
import com.autojournal.app.domain.Calc
import com.autojournal.app.domain.Categories
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.Routes
import com.autojournal.app.ui.components.*
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import kotlin.math.roundToLong

@Composable
fun FuelScreen(vm: MainViewModel, nav: NavController) {
    val refuels by vm.refuels.collectAsStateWithLifecycle()
    val stats = remember(refuels) { Calc.fuelStats(refuels) }

    if (refuels.isEmpty()) {
        EmptyState(Icons.Filled.LocalGasStation, "Заправок пока нет",
            "Записывайте каждую заправку — приложение посчитает реальный расход, цену километра и динамику цен.")
        return
    }
    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard {
                Row {
                    LabelValue("Средний расход",
                        stats.avgConsumption?.let { Fmt.num(it) + " л/100" } ?: "—", Modifier.weight(1f))
                    LabelValue("Средняя цена", stats.avgPrice?.let { Fmt.money(it) } ?: "—", Modifier.weight(1f))
                }
                Row {
                    LabelValue("Всего залито", Fmt.liters(stats.totalLiters), Modifier.weight(1f))
                    LabelValue("Потрачено", Fmt.money(stats.totalCost), Modifier.weight(1f))
                }
                if (stats.series.size >= 2) {
                    Text("Расход по заправкам, л/100 км", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LineChart(stats.series.takeLast(20), MaterialTheme.colorScheme.primary)
                } else {
                    Text("Расход считается между двумя заправками «до полного бака».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(refuels, key = { it.id }) { r ->
            RefuelItem(r, stats.perRefuel[r.id]) { nav.navigate(Routes.refuel(r.id)) }
        }
    }
}

@Composable
private fun RefuelItem(r: Refuel, consumption: Double?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(Icons.Filled.LocalGasStation, categoryColor(Categories.FUEL))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                listOf(Fmt.liters(r.liters), r.fuelGrade, r.station).filter { it.isNotBlank() }.joinToString(" · "),
                maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "${Dates.format(r.date)} · ${Fmt.km(r.odometer)} · ${Fmt.money(r.pricePerLiter)}/л" +
                    if (!r.fullTank) " · неполный" else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Fmt.money(r.total), fontWeight = FontWeight.SemiBold)
            if (consumption != null) {
                Text(Fmt.num(consumption) + " л/100", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun round2(v: Double) = (v * 100).roundToLong() / 100.0

@Composable
fun RefuelEditScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    val car by vm.activeCar.collectAsStateWithLifecycle()
    val refuels by vm.refuels.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val currentKm by vm.currentKm.collectAsStateWithLifecycle()

    var existing by remember { mutableStateOf<Refuel?>(null) }
    var date by remember { mutableLongStateOf(Dates.today()) }
    var odo by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var station by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var full by remember { mutableStateOf(true) }
    var missed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var prefilled by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.repo.db.refuels().get(id)?.let { r ->
                existing = r
                date = r.date; odo = r.odometer.toString(); liters = Fmt.input(r.liters)
                price = Fmt.input(r.pricePerLiter); total = Fmt.input(r.total); grade = r.fuelGrade
                station = r.station; note = r.note; full = r.fullTank; missed = r.missedPrevious
            }
            prefilled = true
        }
    }
    // Для новой записи — подставляем марку топлива и цену с прошлой заправки.
    LaunchedEffect(car, refuels) {
        if (id == 0L && !prefilled && car != null) {
            val last = refuels.firstOrNull()
            grade = last?.fuelGrade?.takeIf { it.isNotBlank() } ?: car?.fuelType.orEmpty()
            last?.let { price = Fmt.input(it.pricePerLiter) }
            prefilled = true
        }
    }

    fun recalcFromLiters() {
        val l = Fmt.parse(liters); val p = Fmt.parse(price)
        if (l != null && p != null) total = Fmt.input(round2(l * p))
    }

    val stations = remember(refuels, contacts) {
        (refuels.map { it.station } + contacts.filter { it.type == "АЗС" }.map { it.name })
            .filter { it.isNotBlank() }.distinct()
    }
    val odoInt = Fmt.parseInt(odo)
    val prevMax = refuels.filter { it.id != id }.maxOfOrNull { it.odometer } ?: 0
    val odoWarn = if (odoInt != null && odoInt < prevMax && date >= (refuels.firstOrNull()?.date ?: 0L))
        "Меньше пробега прошлой заправки (${Fmt.km(prevMax)})" else null

    val save: () -> Unit = save@{
        val c = car ?: return@save
        val l = Fmt.parse(liters)
        val p = Fmt.parse(price)
        val t = Fmt.parse(total)
        val o = odoInt
        when {
            o == null || o <= 0 -> error = "Укажите пробег"
            l == null || l <= 0.0 -> error = if (t != null && p != null && p > 0) null else "Укажите литры"
            else -> error = null
        }
        if (o == null || o <= 0) return@save
        val litersFinal = l ?: if (t != null && p != null && p > 0) round2(t / p) else return@save
        val priceFinal = p ?: if (t != null && litersFinal > 0) round2(t / litersFinal) else 0.0
        val totalFinal = t ?: round2(litersFinal * priceFinal)
        vm.io {
            saveRefuel(
                Refuel(
                    id = existing?.id ?: 0L, carId = c.id, date = date, odometer = o,
                    liters = litersFinal, pricePerLiter = priceFinal, total = totalFinal,
                    fullTank = full, missedPrevious = missed, station = station.trim(),
                    fuelGrade = grade.trim(), note = note.trim(),
                )
            )
        }
        onBack()
    }

    EditScaffold(
        title = if (id == 0L) "Новая заправка" else "Заправка",
        onBack = onBack,
        onSave = save,
        onDelete = existing?.let { r -> { vm.io { deleteRefuel(r) }; onBack() } },
    ) {
        DateInput("Дата", date, { date = it })
        NumberInput(odo, { odo = it }, "Пробег", suffix = "км", decimal = false,
            supporting = odoWarn ?: "Сейчас в журнале: ${Fmt.km(currentKm)}", isError = odoWarn != null)
        OptionInput(grade, { grade = it }, "Топливо", Categories.fuelGrades)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(liters, { liters = it; recalcFromLiters() }, "Литры", Modifier.weight(1f), suffix = "л")
            NumberInput(price, {
                price = it
                val l = Fmt.parse(liters); val p = Fmt.parse(it); val t = Fmt.parse(total)
                if (l != null && p != null) total = Fmt.input(round2(l * p))
                else if (t != null && p != null && p > 0) liters = Fmt.input(round2(t / p))
            }, "Цена за литр", Modifier.weight(1f), suffix = "₽")
        }
        NumberInput(total, {
            total = it
            val t = Fmt.parse(it); val p = Fmt.parse(price)
            if (t != null && p != null && p > 0) liters = Fmt.input(round2(t / p))
        }, "Сумма", suffix = "₽", supporting = "Введите любые два значения — третье посчитается само")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = full, onCheckedChange = { full = it })
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Полный бак")
                Text("Нужен для точного расчёта расхода", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = missed, onCheckedChange = { missed = it })
            Spacer(Modifier.width(12.dp))
            Text("Пропустил предыдущую заправку")
        }
        OptionInput(station, { station = it }, "АЗС", stations)
        TextInput(note, { note = it }, "Заметка", singleLine = false, minLines = 2)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
