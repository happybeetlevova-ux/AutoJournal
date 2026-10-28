package com.autojournal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autojournal.app.data.TireSet
import com.autojournal.app.data.mileage
import com.autojournal.app.domain.Categories
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.Routes
import com.autojournal.app.ui.components.*
import com.autojournal.app.ui.theme.LocalExtras
import com.autojournal.app.util.Fmt

@Composable
fun TiresScreen(vm: MainViewModel, nav: NavController) {
    val tires by vm.tires.collectAsStateWithLifecycle()
    val km by vm.currentKm.collectAsStateWithLifecycle()
    val car by vm.activeCar.collectAsStateWithLifecycle()
    var installFor by remember { mutableStateOf<TireSet?>(null) }

    ListScaffold(
        title = "Шины · ${car?.name ?: ""}",
        onBack = { nav.popBackStack() },
        fabText = "Комплект",
        onFab = { nav.navigate("tire/0") },
    ) {
        if (tires.isEmpty()) {
            item {
                EmptyState(Icons.Filled.TripOrigin, "Комплектов пока нет",
                    "Добавьте летний и зимний комплект: модель, размер, давление, где хранятся. " +
                        "Приложение посчитает пробег каждого комплекта по сезонам.")
            }
        }
        items(tires, key = { it.id }) { t ->
            TireCard(
                t, km,
                onOpen = { nav.navigate("tire/${t.id}") },
                onInstall = { installFor = t },
                onRemove = { vm.io { removeTireSet(t, km) } },
            )
        }
    }

    installFor?.let { t ->
        var addRecord by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { installFor = null },
            title = { Text("Поставить «${t.name}»?") },
            text = {
                Column {
                    Text("Пробег сейчас: ${Fmt.km(km)}. С этого момента пробег будет идти этому комплекту, " +
                        "а снятый уйдёт на хранение.")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = addRecord, onCheckedChange = { addRecord = it })
                        Text("Добавить запись «Шиномонтаж» в сервис")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.io { installTireSet(t, km) }
                    installFor = null
                    if (addRecord) nav.navigate(Routes.service(0, "Шины / шиномонтаж"))
                }) { Text("Поставить") }
            },
            dismissButton = { TextButton(onClick = { installFor = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun TireCard(t: TireSet, km: Int, onOpen: () -> Unit, onInstall: () -> Unit, onRemove: () -> Unit) {
    val e = LocalExtras.current
    val winter = t.season == "Зима"
    val tint = if (winter) androidx.compose.ui.graphics.Color(0xFF4FA3FF) else e.soon
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = cardBorder(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(if (winter) Icons.Filled.AcUnit else Icons.Filled.WbSunny, tint)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    val sub = listOf(t.brand, t.size, if (t.studded) "шипы" else "", t.year?.toString() ?: "")
                        .filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Text(sub, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (t.installed) StatusPill("На машине", e.ok) else StatusPill("Хранение", e.off)
            }
            Row {
                LabelValue("Пробег комплекта", Fmt.km(t.mileage(km)), Modifier.weight(1f))
                val pressure = listOfNotNull(t.pressureFront, t.pressureRear).joinToString(" / ") { Fmt.numTrim(it, 1) }
                LabelValue("Давление", if (pressure.isNotBlank()) "$pressure атм" else "—", Modifier.weight(1f))
            }
            if (t.storage.isNotBlank() && !t.installed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("Хранятся: ${t.storage}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (t.installed) {
                OutlinedButton(onClick = onRemove) { Text("Снять на хранение") }
            } else {
                FilledTonalButton(onClick = onInstall) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Поставить на машину")
                }
            }
        }
    }
}

@Composable
fun TireEditScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    val car by vm.activeCar.collectAsStateWithLifecycle()
    var existing by remember { mutableStateOf<TireSet?>(null) }
    var name by remember { mutableStateOf("") }
    var season by remember { mutableStateOf("Зима") }
    var brand by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var studded by remember { mutableStateOf(false) }
    var year by remember { mutableStateOf("") }
    var pf by remember { mutableStateOf("") }
    var pr by remember { mutableStateOf("") }
    var tread by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var purchase by remember { mutableStateOf<Long?>(null) }
    var startKm by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) vm.repo.db.tires().get(id)?.let { t ->
            existing = t; name = t.name; season = t.season; brand = t.brand; size = t.size; studded = t.studded
            year = t.year?.toString() ?: ""; pf = t.pressureFront?.let { Fmt.input(it) } ?: ""
            pr = t.pressureRear?.let { Fmt.input(it) } ?: ""; tread = t.treadMm?.let { Fmt.input(it) } ?: ""
            storage = t.storage; price = if (t.price > 0) Fmt.input(t.price) else ""; purchase = t.purchaseDate
            startKm = if (t.accumulatedKm > 0) t.accumulatedKm.toString() else ""; photo = t.photoPath; note = t.note
        }
    }

    val save: () -> Unit = save@{
        val c = car ?: return@save
        val finalName = name.trim().ifBlank { listOf(season, brand).filter { it.isNotBlank() }.joinToString(" ") }
        if (finalName.isBlank()) { error = "Укажите название"; return@save }
        val base = existing ?: TireSet(carId = c.id, name = finalName)
        val t = base.copy(
            name = finalName, season = season, brand = brand.trim(), size = size.trim().uppercase(),
            studded = studded, year = Fmt.parseInt(year), pressureFront = Fmt.parse(pf), pressureRear = Fmt.parse(pr),
            treadMm = Fmt.parse(tread), storage = storage.trim(), price = Fmt.parse(price) ?: 0.0,
            purchaseDate = purchase, accumulatedKm = Fmt.parseInt(startKm) ?: 0, photoPath = photo, note = note.trim(),
        )
        vm.io { saveTireSet(t) }
        onBack()
    }

    EditScaffold(
        title = if (id == 0L) "Новый комплект" else name.ifBlank { "Комплект" },
        onBack = onBack, onSave = save,
        onDelete = existing?.let { t -> { vm.io { deleteTireSet(t) }; onBack() } },
        deleteText = "Удалить комплект шин?",
    ) {
        PhotoBox(photo, { photo = it }, height = 150.dp, placeholder = Icons.Filled.TripOrigin, label = "Фото шин")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextInput(name, { name = it }, "Название", Modifier.weight(1.3f))
            Box(Modifier.weight(1f)) { OptionInput(season, { season = it }, "Сезон", Categories.tireSeasons, editable = false) }
        }
        TextInput(brand, { brand = it }, "Марка и модель (Nokian Hakkapeliitta 10)")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextInput(size, { size = it }, "Размер (205/55 R16)", Modifier.weight(1.4f),
                capitalization = KeyboardCapitalization.Characters)
            NumberInput(year, { year = it.take(4) }, "Год", Modifier.weight(1f), decimal = false)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = studded, onCheckedChange = { studded = it })
            Spacer(Modifier.width(12.dp))
            Text("Шипованные")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(pf, { pf = it }, "Давление перед", Modifier.weight(1f), suffix = "атм")
            NumberInput(pr, { pr = it }, "Давление зад", Modifier.weight(1f), suffix = "атм")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(tread, { tread = it }, "Протектор", Modifier.weight(1f), suffix = "мм")
            NumberInput(startKm, { startKm = it }, "Уже пройдено", Modifier.weight(1f), suffix = "км", decimal = false)
        }
        TextInput(storage, { storage = it }, "Где хранятся (гараж, шинный отель…)")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(price, { price = it }, "Цена комплекта", Modifier.weight(1f), suffix = "₽")
            OptionalDateInput("Куплены", purchase, { purchase = it }, Modifier.weight(1f))
        }
        TextInput(note, { note = it }, "Заметка", singleLine = false, minLines = 2)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
