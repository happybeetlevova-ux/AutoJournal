package com.autojournal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.autojournal.app.data.Car
import com.autojournal.app.data.total
import com.autojournal.app.domain.Calc
import com.autojournal.app.domain.Categories
import com.autojournal.app.domain.DueState
import com.autojournal.app.domain.ReminderStatus
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.Routes
import com.autojournal.app.ui.components.*
import com.autojournal.app.ui.theme.StatusColors
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import java.io.File

@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val car by vm.activeCar.collectAsStateWithLifecycle()
    val refuels by vm.refuels.collectAsStateWithLifecycle()
    val services by vm.services.collectAsStateWithLifecycle()
    val statuses by vm.reminderStatuses.collectAsStateWithLifecycle()
    val readings by vm.readings.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    var odoDialog by remember { mutableStateOf(false) }

    val fuel = remember(refuels) { Calc.fuelStats(refuels) }
    val expenses = remember(refuels, services) { Calc.expenses(refuels, services) }
    val today = Dates.today()
    val thisMonth = Dates.yearMonth(today)
    val monthSpent = expenses.filter { Dates.yearMonth(it.date) == thisMonth }.sumOf { it.amount }
    val costPerKm = Calc.costPerKm(readings, expenses.sumOf { it.amount })

    val c = car
    if (c != null) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { CarHero(c, km, onEdit = { nav.navigate(Routes.car(c.id)) }) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(Icons.Filled.LocalGasStation, "Заправка", Modifier.weight(1f)) {
                        nav.navigate(Routes.refuel(0))
                    }
                    QuickAction(Icons.Filled.Build, "Сервис", Modifier.weight(1f)) {
                        nav.navigate(Routes.service(0))
                    }
                    QuickAction(Icons.Filled.Speed, "Пробег", Modifier.weight(1f)) { odoDialog = true }
                    QuickAction(Icons.Filled.BarChart, "Отчёт", Modifier.weight(1f)) { nav.navigate("stats") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Средний расход",
                        fuel.avgConsumption?.let { Fmt.num(it) + " л/100" } ?: "—",
                        Icons.Filled.LocalGasStation, Modifier.weight(1f))
                    StatTile("Расходы за ${Dates.monthShort(thisMonth)}", Fmt.money(monthSpent),
                        Icons.Filled.AttachMoney, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Стоимость 1 км", costPerKm?.let { Fmt.money(it) } ?: "—",
                        Icons.Filled.Timeline, Modifier.weight(1f))
                    StatTile("Цена топлива", fuel.avgPrice?.let { Fmt.money(it) + "/л" } ?: "—",
                        Icons.Filled.Opacity, Modifier.weight(1f))
                }
            }
            item {
                SectionCard(title = "Ближайшие замены", action = {
                    TextButton(onClick = { nav.navigate("reminders") }) { Text("Все") }
                }) {
                    val top = statuses.filter { it.state != DueState.OFF }.take(4)
                    if (top.isEmpty()) {
                        Text("Напоминаний пока нет. Добавьте типовой регламент на вкладке «Замены».",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        top.forEach { ReminderRow(it, today) { nav.navigate("reminders") } }
                    }
                }
            }
            item {
                RecentCard(vm, nav)
            }
        }
    }

    if (odoDialog && c != null) {
        OdometerDialog(
            current = km,
            onDismiss = { odoDialog = false },
            onSave = { v -> vm.io { addOdometer(c.id, v) }; odoDialog = false },
        )
    }
}

@Composable
private fun CarHero(car: Car, km: Int, onEdit: () -> Unit) {
    val p = car.photoPath
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF2A7DE1), Color(0xFF0D3F86)))
            )
            .clickable(onClick = onEdit),
    ) {
        if (p != null) {
            AsyncImage(
                model = File(p), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.DirectionsCar, contentDescription = null,
                tint = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(170.dp).align(Alignment.CenterEnd).offset(x = 20.dp),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
                )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text(
                car.name, color = Color.White, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(
                listOf(car.make, car.model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
                car.year?.toString(),
                car.plate.ifBlank { null },
            ).joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(6.dp))
            Text(
                Fmt.km(km), color = Color.White, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (p == null) {
            Text(
                "Нажмите, чтобы добавить фото", color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.TopStart).padding(18.dp),
            )
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

fun statusColor(state: DueState): Color = when (state) {
    DueState.OVERDUE -> StatusColors.overdue
    DueState.SOON -> StatusColors.soon
    DueState.OK -> StatusColors.ok
    DueState.OFF -> StatusColors.off
}

@Composable
fun ReminderRow(st: ReminderStatus, today: Long, onClick: () -> Unit) {
    val color = statusColor(st.state)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(categoryIcon(st.reminder.category), color, 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(st.reminder.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(st.describe(), style = MaterialTheme.typography.bodySmall, color = color)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { st.progress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.18f),
            )
        }
    }
}

private data class Ev(val date: Long, val km: Int, val title: String, val amount: Double, val cat: String, val route: String)

@Composable
private fun RecentCard(vm: MainViewModel, nav: NavController) {
    val refuels by vm.refuels.collectAsStateWithLifecycle()
    val services by vm.services.collectAsStateWithLifecycle()
    val events = remember(refuels, services) {
        (refuels.map {
            Ev(it.date, it.odometer, "Заправка " + Fmt.liters(it.liters) +
                (if (it.station.isNotBlank()) " · ${it.station}" else ""), it.total, Categories.FUEL, Routes.refuel(it.id))
        } + services.map {
            Ev(it.record.date, it.record.odometer, it.record.title.ifBlank { it.record.category }, it.total(),
                it.record.category, Routes.service(it.record.id))
        }).sortedWith(compareByDescending<Ev> { it.date }.thenByDescending { it.km }).take(6)
    }
    SectionCard(title = "Последние записи") {
        if (events.isEmpty()) {
            Text("Добавьте первую заправку или обслуживание — кнопки выше.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        events.forEach { e ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { nav.navigate(e.route) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(categoryIcon(e.cat), categoryColor(e.cat), 38.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${Dates.format(e.date)} · ${Fmt.km(e.km)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(Fmt.money(e.amount), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun OdometerDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by remember { mutableStateOf("") }
    val v = Fmt.parseInt(value)
    val err = v != null && v < current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Текущий пробег") },
        text = {
            Column {
                Text("Сейчас в журнале: ${Fmt.km(current)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                NumberInput(value, { value = it }, "Пробег", suffix = "км", decimal = false,
                    isError = err, supporting = if (err) "Меньше текущего пробега" else null)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (v != null && !err) onSave(v) }, enabled = v != null && !err) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
