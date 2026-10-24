package com.autojournal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.navigation.NavController
import com.autojournal.app.data.Contact
import com.autojournal.app.data.ServicePart
import com.autojournal.app.data.ServiceRecord
import com.autojournal.app.data.ServiceWithParts
import com.autojournal.app.data.partsTotal
import com.autojournal.app.data.total
import com.autojournal.app.domain.Categories
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.Routes
import com.autojournal.app.ui.components.*
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import com.autojournal.app.util.Intents

@Composable
fun ServiceScreen(vm: MainViewModel, nav: NavController) {
    val services by vm.services.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    if (services.isEmpty()) {
        EmptyState(Icons.Filled.Build, "Записей пока нет",
            "Фиксируйте замены масла, фильтров, ремонт: стоимость работ, запчасти с артикулами и кто делал.")
        return
    }
    val cats = remember(services) { services.map { it.record.category }.distinct() }
    val shown = services.filter { s ->
        (filter == null || s.record.category == filter) &&
            (query.isBlank() || listOf(s.record.title, s.record.category, s.record.note, s.record.place)
                .plus(s.parts.map { it.name + " " + it.partNumber })
                .any { it.contains(query.trim(), ignoreCase = true) })
    }
    val year = Dates.toLocal(Dates.today()).year
    val yearTotal = services.filter { Dates.toLocal(it.record.date).year == year }.sumOf { it.total() }

    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SectionCard {
                Row {
                    LabelValue("Обслуживание за $year", Fmt.money(yearTotal), Modifier.weight(1f))
                    LabelValue("Всего записей", services.size.toString(), Modifier.weight(1f))
                }
            }
        }
        item {
            TextInput(query, { query = it }, "Поиск: работа, запчасть, артикул",
                trailing = { Icon(Icons.Filled.Search, contentDescription = null) })
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Все") })
                }
                items(cats) { c ->
                    FilterChip(selected = filter == c, onClick = { filter = if (filter == c) null else c },
                        label = { Text(c) })
                }
            }
        }
        items(shown, key = { it.record.id }) { s ->
            ServiceItem(s, contacts) { nav.navigate(Routes.service(s.record.id)) }
        }
    }
}

@Composable
private fun ServiceItem(s: ServiceWithParts, contacts: List<Contact>, onClick: () -> Unit) {
    val r = s.record
    val place = contacts.firstOrNull { it.id == r.contactId }?.name ?: r.place
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(categoryIcon(r.category), categoryColor(r.category))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title.ifBlank { r.category }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge)
            Text(
                listOf(Dates.format(r.date), if (r.odometer > 0) Fmt.km(r.odometer) else "", place)
                    .filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (s.parts.isNotEmpty()) {
                Text(s.parts.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(Fmt.money(s.total()), fontWeight = FontWeight.SemiBold)
    }
}

private data class PartDraft(
    val key: Long,
    val name: String = "",
    val number: String = "",
    val price: String = "",
    val qty: String = "1",
)

@Composable
fun ServiceEditScreen(vm: MainViewModel, id: Long, presetCategory: String?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val car by vm.activeCar.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val currentKm by vm.currentKm.collectAsStateWithLifecycle()
    val services by vm.services.collectAsStateWithLifecycle()

    var existing by remember { mutableStateOf<ServiceRecord?>(null) }
    var date by remember { mutableLongStateOf(Dates.today()) }
    var odo by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(presetCategory ?: "") }
    var title by remember { mutableStateOf("") }
    var labor by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var saveToContacts by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }
    val parts = remember { mutableStateListOf<PartDraft>() }
    var error by remember { mutableStateOf<String?>(null) }
    var odoPrefilled by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.repo.db.services().get(id)?.let { s ->
                val r = s.record
                existing = r
                date = r.date; odo = if (r.odometer > 0) r.odometer.toString() else ""
                category = r.category; title = r.title; labor = if (r.laborCost > 0) Fmt.input(r.laborCost) else ""
                place = vm.repo.db.contacts().get(r.contactId ?: -1L)?.name ?: r.place
                note = r.note; photo = r.photoPath
                parts.clear()
                s.parts.forEachIndexed { i, p ->
                    parts += PartDraft(i.toLong(), p.name, p.partNumber, Fmt.input(p.price), Fmt.input(p.quantity))
                }
            }
            odoPrefilled = true
        }
    }
    LaunchedEffect(currentKm) {
        if (id == 0L && !odoPrefilled && currentKm > 0) {
            odo = currentKm.toString(); odoPrefilled = true
        }
    }

    val matched = contacts.firstOrNull { it.name.equals(place.trim(), ignoreCase = true) }
    val partsSum = parts.sumOf { (Fmt.parse(it.price) ?: 0.0) * (Fmt.parse(it.qty) ?: 1.0) }
    val laborSum = Fmt.parse(labor) ?: 0.0
    val pastPlaces = remember(services, contacts) {
        (contacts.map { it.name } + services.map { it.record.place }).filter { it.isNotBlank() }.distinct()
    }

    val save: () -> Unit = save@{
        val c = car ?: return@save
        if (category.isBlank()) { error = "Выберите категорию"; return@save }
        val o = Fmt.parseInt(odo) ?: 0
        val placeText = place.trim()
        val record = ServiceRecord(
            id = existing?.id ?: 0L, carId = c.id, date = date, odometer = o,
            category = category.trim(), title = title.trim(), laborCost = laborSum,
            contactId = matched?.id, place = if (matched == null) placeText else "",
            note = note.trim(), photoPath = photo,
        )
        val partList = parts.filter { it.name.isNotBlank() }.map {
            ServicePart(recordId = 0, name = it.name.trim(), partNumber = it.number.trim(),
                price = Fmt.parse(it.price) ?: 0.0, quantity = Fmt.parse(it.qty) ?: 1.0)
        }
        val createContact = matched == null && placeText.isNotBlank() && saveToContacts
        vm.io {
            var rec = record
            if (createContact) {
                val cid = saveContact(Contact(name = placeText, type = "СТО", phone = newPhone.trim()))
                rec = rec.copy(contactId = cid, place = "")
            }
            saveService(rec, partList)
        }
        onBack()
    }

    EditScaffold(
        title = if (id == 0L) "Новая запись" else "Обслуживание",
        onBack = onBack,
        onSave = save,
        onDelete = existing?.let { r -> { vm.io { deleteService(r) }; onBack() } },
    ) {
        OptionInput(category, { category = it }, "Категория", Categories.service,
            supporting = "Напоминание этой категории обновится автоматически")
        TextInput(title, { title = it }, "Что сделано (например: замена масла 5W-30)")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { DateInput("Дата", date, { date = it }) }
            NumberInput(odo, { odo = it }, "Пробег", Modifier.weight(1f), suffix = "км", decimal = false)
        }

        SectionCard(title = "Где делали") {
            OptionInput(place, { place = it }, "СТО, мастер или «сам»", pastPlaces)
            if (matched != null) {
                if (matched.phone.isNotBlank()) {
                    AssistChip(
                        onClick = { Intents.dial(ctx, matched.phone) },
                        label = { Text("Позвонить: ${matched.phone}") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    )
                }
            } else if (place.isNotBlank()) {
                TextInput(newPhone, { newPhone = it }, "Телефон", keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveToContacts, onCheckedChange = { saveToContacts = it })
                    Text("Сохранить в контакты")
                }
            }
        }

        SectionCard(title = "Стоимость") {
            NumberInput(labor, { labor = it }, "Работа", suffix = "₽")
            Text("Запчасти и материалы", style = MaterialTheme.typography.labelLarge)
            parts.forEachIndexed { i, p ->
                PartRow(
                    p,
                    onChange = { parts[i] = it },
                    onRemove = { parts.removeAt(i) },
                )
            }
            OutlinedButton(onClick = {
                parts += PartDraft((parts.maxOfOrNull { it.key } ?: -1L) + 1L)
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Добавить запчасть")
            }
            HorizontalDivider()
            Row {
                LabelValue("Работа", Fmt.money(laborSum), Modifier.weight(1f))
                LabelValue("Запчасти", Fmt.money(partsSum), Modifier.weight(1f))
                LabelValue("Итого", Fmt.money(laborSum + partsSum), Modifier.weight(1f))
            }
        }

        Text("Фото (чек, заказ-наряд, деталь)", style = MaterialTheme.typography.labelLarge)
        PhotoBox(photo, { photo = it }, height = 160.dp, placeholder = Icons.Filled.Receipt,
            label = "Сфотографировать чек")
        TextInput(note, { note = it }, "Заметка", singleLine = false, minLines = 2)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun PartRow(p: PartDraft, onChange: (PartDraft) -> Unit, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextInput(p.name, { onChange(p.copy(name = it)) }, "Название", Modifier.weight(1f))
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Удалить") }
            }
            TextInput(p.number, { onChange(p.copy(number = it)) }, "Артикул / бренд",
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberInput(p.price, { onChange(p.copy(price = it)) }, "Цена", Modifier.weight(1.4f), suffix = "₽")
                NumberInput(p.qty, { onChange(p.copy(qty = it)) }, "Кол-во", Modifier.weight(1f))
            }
        }
    }
}
