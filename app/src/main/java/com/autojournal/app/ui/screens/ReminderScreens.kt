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
import com.autojournal.app.data.Reminder
import com.autojournal.app.domain.Categories
import com.autojournal.app.domain.DueState
import com.autojournal.app.domain.ReminderStatus
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.Routes
import com.autojournal.app.ui.components.*
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt

@Composable
fun RemindersScreen(vm: MainViewModel, nav: NavController) {
    val car by vm.activeCar.collectAsStateWithLifecycle()
    val statuses by vm.reminderStatuses.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    val kpd by vm.kmPerDay.collectAsStateWithLifecycle()
    var doneFor by remember { mutableStateOf<ReminderStatus?>(null) }
    val today = Dates.today()

    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard {
                Row {
                    LabelValue("Пробег", Fmt.km(km), Modifier.weight(1f))
                    LabelValue("В среднем", kpd?.let { Fmt.numTrim(it, 0) + " км/день" } ?: "—", Modifier.weight(1f))
                }
                val over = statuses.count { it.state == DueState.OVERDUE }
                val soon = statuses.count { it.state == DueState.SOON }
                Text(
                    when {
                        over > 0 -> "Просрочено: $over, скоро: $soon"
                        soon > 0 -> "Скоро подходит срок: $soon"
                        statuses.isEmpty() -> "Добавьте напоминания — уведомление придёт заранее"
                        else -> "Всё в порядке"
                    },
                    color = when {
                        over > 0 -> statusColor(DueState.OVERDUE)
                        soon > 0 -> statusColor(DueState.SOON)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        if (statuses.isEmpty()) {
            item {
                EmptyState(Icons.Filled.NotificationsActive, "Напоминаний нет",
                    "Можно добавить типовой регламент: масло, фильтры, свечи, тормозная жидкость, ГРМ, шины, ОСАГО. Интервалы потом легко поменять.")
                Button(
                    onClick = { car?.let { c -> vm.io { addDefaultReminders(c.id, km) } } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Добавить типовые напоминания") }
            }
        }
        items(statuses, key = { it.reminder.id }) { st ->
            ReminderCard(
                st, today,
                onClick = { nav.navigate(Routes.reminder(st.reminder.id)) },
                onDone = { doneFor = st },
                onShop = { nav.navigate("shopping") },
            )
        }
    }

    doneFor?.let { st ->
        AlertDialog(
            onDismissRequest = { doneFor = null },
            title = { Text(st.reminder.title) },
            text = {
                Text("Отметить выполненным на пробеге ${Fmt.km(km)} сегодня?\n\n" +
                    "Лучше добавить запись в «Сервис» — тогда сохранятся цена работ, запчасти и кто делал, " +
                    "а напоминание обновится само.")
            },
            confirmButton = {
                TextButton(onClick = {
                    nav.navigate(Routes.service(0, st.reminder.category)); doneFor = null
                }) { Text("Добавить запись") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.io { markReminderDone(st.reminder, km, Dates.today()) }; doneFor = null
                }) { Text("Только отметить") }
            },
        )
    }
}

@Composable
private fun ReminderCard(
    st: ReminderStatus,
    today: Long,
    onClick: () -> Unit,
    onDone: () -> Unit,
    onShop: () -> Unit,
) {
    val r = st.reminder
    val color = statusColor(st.state)
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = cardBorder(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(categoryIcon(r.category), color)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(st.describe(), color = color, style = MaterialTheme.typography.bodySmall)
                }
                if (r.enabled) {
                    FilledTonalButton(onClick = onDone, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Сделано")
                    }
                }
            }
            if (st.state != DueState.OFF) {
                LinearProgressIndicator(
                    progress = { st.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = color, trackColor = color.copy(alpha = 0.18f),
                )
            }
            val interval = listOfNotNull(
                r.intervalKm?.let { "каждые ${Fmt.km(it)}" },
                r.intervalDays?.let { "каждые ${Fmt.days(it)}" },
            ).joinToString(" или ")
            val last = listOfNotNull(r.lastKm?.let { Fmt.km(it) }, r.lastDate?.let { Dates.format(it) })
                .joinToString(", ")
            Text(
                listOf(interval, if (last.isNotBlank()) "последний раз: $last" else "")
                    .filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            st.predictedDateText(today)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (st.state == DueState.SOON || st.state == DueState.OVERDUE) {
                TextButton(onClick = onShop, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Что купить к замене")
                }
            }
        }
    }
}

@Composable
fun ReminderEditScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    val car by vm.activeCar.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()

    var existing by remember { mutableStateOf<Reminder?>(null) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var intKm by remember { mutableStateOf("") }
    var intDays by remember { mutableStateOf("") }
    var intMonths by remember { mutableStateOf(false) }
    var lastKm by remember { mutableStateOf("") }
    var lastDate by remember { mutableLongStateOf(Dates.today()) }
    var warnKm by remember { mutableStateOf("500") }
    var warnDays by remember { mutableStateOf("7") }
    var enabled by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var prefilled by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.repo.db.reminders().get(id)?.let { r ->
                existing = r
                title = r.title; category = r.category
                intKm = r.intervalKm?.toString() ?: ""
                val d = r.intervalDays
                if (d != null && d % 30 == 0 && d >= 30) { intMonths = true; intDays = (d / 30).toString() }
                else { intMonths = false; intDays = d?.toString() ?: "" }
                lastKm = r.lastKm?.toString() ?: ""
                lastDate = r.lastDate ?: Dates.today()
                warnKm = r.warnKm.toString(); warnDays = r.warnDays.toString(); enabled = r.enabled
            }
            prefilled = true
        }
    }
    LaunchedEffect(km) {
        if (id == 0L && !prefilled) { lastKm = if (km > 0) km.toString() else ""; prefilled = true }
    }

    val save: () -> Unit = save@{
        val c = car ?: return@save
        val ik = Fmt.parseInt(intKm)
        val idRaw = Fmt.parseInt(intDays)
        val idays = idRaw?.let { if (intMonths) it * 30 else it }
        when {
            title.isBlank() && category.isBlank() -> { error = "Укажите название"; return@save }
            (ik == null || ik <= 0) && (idays == null || idays <= 0) -> {
                error = "Задайте интервал по пробегу и/или по времени"; return@save
            }
        }
        vm.io {
            saveReminder(
                Reminder(
                    id = existing?.id ?: 0L, carId = c.id,
                    title = title.trim().ifBlank { category.trim() },
                    category = category.trim().ifBlank { "Прочее" },
                    intervalKm = ik?.takeIf { it > 0 }, intervalDays = idays?.takeIf { it > 0 },
                    lastKm = Fmt.parseInt(lastKm), lastDate = lastDate,
                    warnKm = Fmt.parseInt(warnKm) ?: 500, warnDays = Fmt.parseInt(warnDays) ?: 7,
                    enabled = enabled, lastNotified = null,
                )
            )
        }
        onBack()
    }

    EditScaffold(
        title = if (id == 0L) "Новое напоминание" else "Напоминание",
        onBack = onBack, onSave = save,
        onDelete = existing?.let { r -> { vm.io { deleteReminder(r) }; onBack() } },
        deleteText = "Удалить напоминание?",
    ) {
        OptionInput(category, {
            category = it
            if (title.isBlank()) title = it
        }, "Категория", Categories.service,
            supporting = "Запись в «Сервисе» с этой категорией сбросит счётчик")
        TextInput(title, { title = it }, "Название")
        SectionCard(title = "Интервал — что наступит раньше") {
            NumberInput(intKm, { intKm = it }, "Каждые", suffix = "км", decimal = false)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInput(intDays, { intDays = it }, "Каждые", Modifier.weight(1f),
                    suffix = if (intMonths) "мес." else "дн.", decimal = false)
                FilterChip(selected = !intMonths, onClick = { intMonths = false }, label = { Text("дни") })
                FilterChip(selected = intMonths, onClick = { intMonths = true }, label = { Text("месяцы") })
            }
        }
        SectionCard(title = "Последний раз") {
            NumberInput(lastKm, { lastKm = it }, "На пробеге", suffix = "км", decimal = false,
                supporting = "Сейчас: ${Fmt.km(km)}")
            DateInput("Дата", lastDate, { lastDate = it })
        }
        SectionCard(title = "Предупредить заранее") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberInput(warnKm, { warnKm = it }, "За", Modifier.weight(1f), suffix = "км", decimal = false)
                NumberInput(warnDays, { warnDays = it }, "За", Modifier.weight(1f), suffix = "дн.", decimal = false)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = enabled, onCheckedChange = { enabled = it })
            Spacer(Modifier.width(12.dp))
            Text("Уведомлять")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
