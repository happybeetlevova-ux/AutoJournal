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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autojournal.app.data.Contact
import com.autojournal.app.data.total
import com.autojournal.app.domain.Categories
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.Routes
import com.autojournal.app.ui.components.*
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import com.autojournal.app.util.Intents

private fun contactIcon(type: String): ImageVector = when (type) {
    "АЗС" -> Icons.Filled.LocalGasStation
    "Магазин запчастей" -> Icons.Filled.Store
    "Мойка" -> Icons.Filled.LocalCarWash
    "Эвакуатор" -> Icons.Filled.LocalShipping
    "Страховая" -> Icons.Filled.Security
    "Мастер" -> Icons.Filled.Person
    else -> Icons.Filled.Build
}

@Composable
fun ContactsScreen(vm: MainViewModel, nav: NavController) {
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }

    if (contacts.isEmpty()) {
        EmptyState(Icons.Filled.Contacts, "Контактов пока нет",
            "Сохраняйте СТО, мастеров, магазины запчастей, эвакуатор и страховую — звонок в одно касание.")
        return
    }
    val shown = contacts.filter {
        query.isBlank() || listOf(it.name, it.type, it.phone, it.address, it.note)
            .any { f -> f.contains(query.trim(), ignoreCase = true) }
    }
    LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            TextInput(query, { query = it }, "Поиск",
                trailing = { Icon(Icons.Filled.Search, contentDescription = null) })
        }
        items(shown, key = { it.id }) { c ->
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .clickable { nav.navigate(Routes.contact(c.id)) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(contactIcon(c.type), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOf(c.type, c.phone).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (c.address.isNotBlank()) {
                            Text(c.address, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (c.address.isNotBlank()) {
                        IconButton(onClick = { Intents.map(ctx, c.address) }) {
                            Icon(Icons.Filled.Map, contentDescription = "На карте")
                        }
                    }
                    if (c.phone.isNotBlank()) {
                        FilledIconButton(onClick = { Intents.dial(ctx, c.phone) }) {
                            Icon(Icons.Filled.Phone, contentDescription = "Позвонить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactEditScreen(vm: MainViewModel, nav: NavController, id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val cars by vm.cars.collectAsStateWithLifecycle()
    var existing by remember { mutableStateOf<Contact?>(null) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("СТО") }
    var phone by remember { mutableStateOf("") }
    var phone2 by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.repo.db.contacts().get(id)?.let { c ->
                existing = c
                name = c.name; type = c.type; phone = c.phone; phone2 = c.phone2
                address = c.address; note = c.note
            }
        }
    }
    val history by remember(id) { vm.repo.servicesByContact(id) }.collectAsStateWithLifecycle(emptyList())

    val save: () -> Unit = save@{
        if (name.isBlank()) { error = "Укажите название или имя"; return@save }
        vm.io {
            saveContact(Contact(id = existing?.id ?: 0L, name = name.trim(), type = type.trim(),
                phone = phone.trim(), phone2 = phone2.trim(), address = address.trim(), note = note.trim()))
        }
        onBack()
    }

    EditScaffold(
        title = if (id == 0L) "Новый контакт" else name.ifBlank { "Контакт" },
        onBack = onBack, onSave = save,
        onDelete = existing?.let { c -> { vm.io { deleteContact(c) }; onBack() } },
        deleteText = "Удалить контакт? Записи обслуживания останутся.",
    ) {
        TextInput(name, { name = it }, "Название / имя")
        OptionInput(type, { type = it }, "Тип", Categories.contactTypes)
        TextInput(phone, { phone = it }, "Телефон", keyboardType = KeyboardType.Phone,
            trailing = if (phone.isNotBlank()) {
                { IconButton(onClick = { Intents.dial(ctx, phone) }) { Icon(Icons.Filled.Phone, "Позвонить") } }
            } else null)
        TextInput(phone2, { phone2 = it }, "Доп. телефон", keyboardType = KeyboardType.Phone,
            trailing = if (phone2.isNotBlank()) {
                { IconButton(onClick = { Intents.dial(ctx, phone2) }) { Icon(Icons.Filled.Phone, "Позвонить") } }
            } else null)
        TextInput(address, { address = it }, "Адрес",
            trailing = if (address.isNotBlank()) {
                { IconButton(onClick = { Intents.map(ctx, address) }) { Icon(Icons.Filled.Map, "На карте") } }
            } else null)
        TextInput(note, { note = it }, "Заметка (часы работы, мастер, скидка…)", singleLine = false, minLines = 2)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (history.isNotEmpty()) {
            SectionCard(title = "Что здесь делали") {
                Text("Всего потрачено: ${Fmt.money(history.sumOf { it.total() })}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                history.forEach { s ->
                    val carName = cars?.firstOrNull { it.id == s.record.carId }?.name ?: ""
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .clickable {
                                vm.repo.setActiveCar(s.record.carId)
                                nav.navigate(Routes.service(s.record.id))
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.record.title.ifBlank { s.record.category }, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text("${Dates.format(s.record.date)} · $carName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Fmt.money(s.total()), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
