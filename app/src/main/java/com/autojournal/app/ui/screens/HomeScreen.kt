package com.autojournal.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.autojournal.app.R
import com.autojournal.app.data.Car
import com.autojournal.app.data.total
import com.autojournal.app.domain.Calc
import com.autojournal.app.domain.Categories
import com.autojournal.app.domain.DueState
import com.autojournal.app.domain.ReminderStatus
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.Routes
import com.autojournal.app.ui.components.*
import com.autojournal.app.ui.theme.AppStyle
import com.autojournal.app.ui.theme.BigNumber
import com.autojournal.app.ui.theme.LocalExtras
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
    val documents by vm.documents.collectAsStateWithLifecycle()
    val tires by vm.tires.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()

    val fuel = remember(refuels) { Calc.fuelStats(refuels) }
    val expenses = remember(refuels, services) { Calc.expenses(refuels, services) }
    val today = Dates.today()
    val thisMonth = Dates.yearMonth(today)
    val monthSpent = expenses.filter { Dates.yearMonth(it.date) == thisMonth }.sumOf { it.amount }
    val costPerKm = Calc.costPerKm(readings, expenses.sumOf { it.amount })
    val active = statuses.filter { it.state != DueState.OFF }

    val c = car ?: return
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { CarHero(c, km, onEdit = { nav.navigate(Routes.car(c.id)) }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Filled.LocalGasStation, "Заправка", Modifier.weight(1f)) { nav.navigate(Routes.refuel(0)) }
                QuickAction(Icons.Filled.Build, "Сервис", Modifier.weight(1f)) { nav.navigate(Routes.service(0)) }
                QuickAction(Icons.Filled.Speed, "Пробег", Modifier.weight(1f)) { nav.navigate("odometer") }
                QuickAction(Icons.Filled.BarChart, "Отчёт", Modifier.weight(1f)) { nav.navigate("stats") }
            }
        }
        active.firstOrNull()?.let { top ->
            item { NextReplacementCard(top, today) { nav.navigate("reminders") } }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Средний расход", fuel.avgConsumption?.let { Fmt.num(it) } ?: "—", "л/100 км",
                    Icons.Filled.LocalGasStation, Modifier.weight(1f))
                StatTile("Расходы, ${Dates.monthShort(thisMonth)}", Fmt.money(monthSpent), null,
                    Icons.Filled.AttachMoney, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Стоимость 1 км", costPerKm?.let { Fmt.money(it) } ?: "—", null,
                    Icons.Filled.Timeline, Modifier.weight(1f))
                StatTile("Цена топлива", fuel.avgPrice?.let { Fmt.money(it) } ?: "—", "за литр",
                    Icons.Filled.Opacity, Modifier.weight(1f))
            }
        }
        item {
            val extras = LocalExtras.current
            val nearestDoc = documents.filter { it.expires != null }.minByOrNull { it.expires!! }
            val docSub = when {
                documents.isEmpty() -> "Добавить СТС, ОСАГО"
                nearestDoc != null -> {
                    val d = Math.floorDiv(nearestDoc.expires!! - today, Dates.DAY).toInt()
                    if (d < 0) "${nearestDoc.type}: истёк" else "${nearestDoc.type}: ${Fmt.days(d)}"
                }
                else -> "${documents.size} шт."
            }
            val onCar = tires.firstOrNull { it.installed }
            val shopCount = active.count { it.state != DueState.OK }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LinkTile(Icons.Filled.Description, "Документы", docSub, extras.heroAccent.takeIf { extras.style != AppStyle.PREMIUM } ?: MaterialTheme.colorScheme.secondary,
                    Modifier.weight(1f)) { nav.navigate("documents") }
                LinkTile(Icons.Filled.TripOrigin, "Шины", onCar?.name ?: if (tires.isEmpty()) "Комплекты" else "Не выбраны",
                    extras.ok, Modifier.weight(1f)) { nav.navigate("tires") }
                LinkTile(Icons.Filled.ShoppingCart, "Покупки", if (shopCount > 0) "к ТО: $shopCount" else "Список к ТО",
                    extras.soon, Modifier.weight(1f)) { nav.navigate("shopping") }
            }
        }
        if (active.size > 1) {
            item {
                SectionCard(title = "Дальше по плану", action = {
                    TextButton(onClick = { nav.navigate("reminders") }) { Text("Все") }
                }) {
                    active.drop(1).take(3).forEach { ReminderRow(it, today) { nav.navigate("reminders") } }
                }
            }
        } else if (active.isEmpty()) {
            item {
                SectionCard(title = "Замены") {
                    Text("Напоминаний пока нет. Добавьте типовой регламент на вкладке «Замены».",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { RecentCard(vm, nav) }
    }
}

@Composable
private fun CarHero(car: Car, km: Int, onEdit: () -> Unit) {
    val extras = LocalExtras.current
    val p = car.photoPath
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(shape)
            .background(Brush.linearGradient(extras.heroColors))
            .then(if (extras.glow) Modifier.border(1.dp, extras.heroAccent.copy(alpha = 0.45f), shape) else Modifier)
            .clickable(onClick = onEdit),
    ) {
        if (p != null) {
            AsyncImage(
                model = File(p), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
        } else {
            val neon = extras.style == AppStyle.NEON
            Image(
                painter = painterResource(if (neon) R.drawable.car_outline else R.drawable.car_silhouette),
                contentDescription = null,
                colorFilter = if (neon) null else ColorFilter.tint(extras.heroText.copy(alpha = 0.10f)),
                modifier = Modifier.width(230.dp).align(Alignment.TopEnd).padding(top = 18.dp).offset(x = 24.dp),
            )
            Text(
                "Нажмите, чтобы добавить фото", color = extras.heroText.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopStart).padding(18.dp),
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            val sub = listOfNotNull(
                listOf(car.make, car.model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
                car.year?.toString(),
            ).joinToString(" · ").ifBlank { car.name }
            Text(
                sub.uppercase(), color = if (p != null) Color.White.copy(alpha = 0.9f) else extras.heroAccent,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Fmt.km(km).removeSuffix(" км"), color = extras.heroText, style = BigNumber)
                Text(" км", color = extras.heroText.copy(alpha = 0.75f), fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            if (car.plate.isNotBlank()) {
                Box(
                    Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color.White).padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(car.plate, color = Color(0xFF111111), style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun NextReplacementCard(st: ReminderStatus, today: Long, onClick: () -> Unit) {
    val color = statusColor(st.state)
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = cardBorder(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Gauge(st.progress, color, size = 104.dp) { GaugePercent(st.progress, color) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Ближайшая замена", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(st.reminder.title, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(st.describe(), color = color, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                st.predictedDateText(today)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val border = cardBorder()
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(18.dp)) else Modifier)
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
private fun StatTile(label: String, value: String, unit: String?, icon: ImageVector, modifier: Modifier) {
    val border = cardBorder()
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(18.dp)) else Modifier)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (unit != null) {
                Text(" $unit", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
fun statusColor(state: DueState): Color {
    val e = LocalExtras.current
    return when (state) {
        DueState.OVERDUE -> e.overdue
        DueState.SOON -> e.soon
        DueState.OK -> e.ok
        DueState.OFF -> e.off
    }
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
                drawStopIndicator = {},
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
        }).sortedWith(compareByDescending<Ev> { it.date }.thenByDescending { it.km }).take(5)
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
