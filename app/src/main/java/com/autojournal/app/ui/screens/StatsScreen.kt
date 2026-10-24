package com.autojournal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autojournal.app.domain.Calc
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.components.*
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import com.autojournal.app.util.Intents

private enum class Period(val label: String) { MONTH("30 дней"), YEAR("Год"), ALL("Всё время") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val car by vm.activeCar.collectAsStateWithLifecycle()
    val refuels by vm.refuels.collectAsStateWithLifecycle()
    val services by vm.services.collectAsStateWithLifecycle()
    val readings by vm.readings.collectAsStateWithLifecycle()
    var period by remember { mutableStateOf(Period.YEAR) }

    val today = Dates.today()
    val from = when (period) {
        Period.MONTH -> today - 30 * Dates.DAY
        Period.YEAR -> today - 365 * Dates.DAY
        Period.ALL -> Long.MIN_VALUE
    }
    val all = remember(refuels, services) { Calc.expenses(refuels, services) }
    val inPeriod = all.filter { it.date >= from }
    val total = inPeriod.sumOf { it.amount }
    val fuelTotal = inPeriod.filter { it.isFuel }.sumOf { it.amount }
    val byCat = inPeriod.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
    val periodReadings = readings.filter { it.first >= from }
    val km = if (periodReadings.size >= 2) periodReadings.maxOf { it.second } - periodReadings.minOf { it.second } else 0
    val fuelStats = remember(refuels) { Calc.fuelStats(refuels) }

    // Последние 12 месяцев
    val thisMonth = Dates.yearMonth(today)
    val months = (11 downTo 0).map { thisMonth.minusMonths(it.toLong()) }
    val monthly = months.map { ym -> all.filter { Dates.yearMonth(it.date) == ym }.sumOf { it.amount } }

    fun report(): String = buildString {
        appendLine("АвтоЖурнал — ${car?.name ?: ""}")
        appendLine("Период: ${period.label}")
        appendLine("Всего расходов: ${Fmt.money(total)}")
        appendLine("Топливо: ${Fmt.money(fuelTotal)}; обслуживание и прочее: ${Fmt.money(total - fuelTotal)}")
        if (km > 0) appendLine("Пробег за период: ${Fmt.km(km)}; 1 км = ${Fmt.money(total / km)}")
        fuelStats.avgConsumption?.let { appendLine("Средний расход: ${Fmt.num(it)} л/100 км") }
        appendLine()
        byCat.forEach { appendLine("• ${it.key}: ${Fmt.money(it.value)}") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                actions = {
                    IconButton(onClick = { Intents.shareText(ctx, "Отчёт по автомобилю", report()) }) {
                        Icon(Icons.Filled.Share, "Поделиться отчётом")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Period.entries.forEachIndexed { i, p ->
                    SegmentedButton(
                        selected = period == p, onClick = { period = p },
                        shape = SegmentedButtonDefaults.itemShape(i, Period.entries.size),
                    ) { Text(p.label) }
                }
            }
            SectionCard {
                Row {
                    LabelValue("Всего", Fmt.money(total), Modifier.weight(1f))
                    LabelValue("Пробег", if (km > 0) Fmt.km(km) else "—", Modifier.weight(1f))
                }
                Row {
                    LabelValue("Топливо", Fmt.money(fuelTotal), Modifier.weight(1f))
                    LabelValue("Сервис и прочее", Fmt.money(total - fuelTotal), Modifier.weight(1f))
                }
                Row {
                    LabelValue("Стоимость 1 км", if (km > 0) Fmt.money(total / km) else "—", Modifier.weight(1f))
                    LabelValue("Средний расход",
                        fuelStats.avgConsumption?.let { Fmt.num(it) + " л/100" } ?: "—", Modifier.weight(1f))
                }
            }
            SectionCard(title = "Расходы по месяцам") {
                BarChart(monthly, months.map { Dates.monthShort(it).take(3) }, MaterialTheme.colorScheme.primary)
                Text("${Dates.monthLong(thisMonth)}: ${Fmt.money(monthly.last())}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (fuelStats.series.size >= 2) {
                SectionCard(title = "Расход топлива, л/100 км") {
                    LineChart(fuelStats.series.takeLast(20), MaterialTheme.colorScheme.secondary)
                    Text("мин ${Fmt.num(fuelStats.series.min())} · макс ${Fmt.num(fuelStats.series.max())}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SectionCard(title = "Куда уходят деньги") {
                if (byCat.isEmpty()) {
                    Text("За этот период записей нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                byCat.forEach { (cat, sum) ->
                    val frac = if (total > 0) (sum / total).toFloat() else 0f
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(categoryIcon(cat), categoryColor(cat), 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(cat, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(Fmt.money(sum), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(6.dp))
                            Text("${(frac * 100).toInt()}%", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = categoryColor(cat), trackColor = categoryColor(cat).copy(alpha = 0.15f),
                        )
                    }
                }
            }
        }
    }
}
