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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.autojournal.app.data.CarDocument
import com.autojournal.app.domain.Categories
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.components.*
import com.autojournal.app.ui.theme.LocalExtras
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import java.io.File

private fun docIcon(type: String): ImageVector = when (type) {
    "ОСАГО", "КАСКО" -> Icons.Filled.Security
    "Водительское удостоверение" -> Icons.Filled.Badge
    "Диагностическая карта" -> Icons.Filled.Speed
    "Гарантия" -> Icons.Filled.Verified
    else -> Icons.Filled.Description
}

@Composable
fun DocumentsScreen(vm: MainViewModel, nav: NavController) {
    val docs by vm.documents.collectAsStateWithLifecycle()
    val car by vm.activeCar.collectAsStateWithLifecycle()
    var viewer by remember { mutableStateOf<String?>(null) }
    ListScaffold(
        title = "Документы · ${car?.name ?: ""}",
        onBack = { nav.popBackStack() },
        fabText = "Документ",
        onFab = { nav.navigate("document/0") },
    ) {
        if (docs.isEmpty()) {
            item {
                EmptyState(Icons.Filled.Description, "Документов пока нет",
                    "Сфотографируйте СТС, ПТС, полис ОСАГО, права — они всегда будут под рукой. " +
                        "За 30 и за 7 дней до окончания срока придёт уведомление.")
            }
        }
        items(docs, key = { it.id }) { d ->
            DocumentCard(d, onOpen = { nav.navigate("document/${d.id}") }, onPhoto = { viewer = it })
        }
    }
    viewer?.let { PhotoViewer(it) { viewer = null } }
}

@Composable
private fun DocumentCard(d: CarDocument, onOpen: () -> Unit, onPhoto: (String) -> Unit) {
    val e = LocalExtras.current
    val today = Dates.today()
    val exp = d.expires
    val (statusText, color) = if (exp == null) {
        "Бессрочно" to e.off
    } else {
        val days = Math.floorDiv(exp - today, Dates.DAY).toInt()
        when {
            days < 0 -> "Истёк ${Dates.format(exp)}" to e.overdue
            days <= 30 -> "Осталось ${Fmt.days(days)}" to e.soon
            else -> "До ${Dates.format(exp)}" to e.ok
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = cardBorder(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(docIcon(d.type), color)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.type, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (d.number.isNotBlank()) {
                        Text(d.number, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                StatusPill(statusText, color)
            }
            val photos = listOfNotNull(d.photo1, d.photo2)
            if (photos.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.forEach { p ->
                        AsyncImage(
                            model = File(p), contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(12.dp))
                                .clickable { onPhoto(p) },
                        )
                    }
                    if (photos.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DocumentEditScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    val car by vm.activeCar.collectAsStateWithLifecycle()
    var existing by remember { mutableStateOf<CarDocument?>(null) }
    var type by remember { mutableStateOf("ОСАГО") }
    var number by remember { mutableStateOf("") }
    var issued by remember { mutableStateOf<Long?>(null) }
    var expires by remember { mutableStateOf<Long?>(null) }
    var photo1 by remember { mutableStateOf<String?>(null) }
    var photo2 by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id != 0L) vm.repo.db.documents().get(id)?.let { d ->
            existing = d; type = d.type; number = d.number; issued = d.issued; expires = d.expires
            photo1 = d.photo1; photo2 = d.photo2; note = d.note
        }
    }
    val save: () -> Unit = save@{
        val c = car ?: return@save
        val d = CarDocument(
            id = existing?.id ?: 0L, carId = existing?.carId ?: c.id, type = type.trim().ifBlank { "Другое" },
            number = number.trim(), issued = issued, expires = expires, photo1 = photo1, photo2 = photo2,
            note = note.trim(), notifiedStage = existing?.notifiedStage ?: 0,
        )
        vm.io { saveDocument(d) }
        onBack()
    }
    EditScaffold(
        title = if (id == 0L) "Новый документ" else type,
        onBack = onBack, onSave = save,
        onDelete = existing?.let { d -> { vm.io { deleteDocument(d) }; onBack() } },
        deleteText = "Удалить документ?",
    ) {
        OptionInput(type, { type = it }, "Документ", Categories.documentTypes)
        TextInput(number, { number = it }, "Серия и номер", capitalization = KeyboardCapitalization.Characters)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionalDateInput("Выдан", issued, { issued = it }, Modifier.weight(1f))
            OptionalDateInput("Действует до", expires, { expires = it }, Modifier.weight(1f))
        }
        if (type == "ОСАГО" || type == "КАСКО" || type == "Диагностическая карта") {
            val base = issued
            if (base != null && expires == null) {
                AssistChip(
                    onClick = { expires = Dates.fromLocal(Dates.toLocal(base).plusYears(1).minusDays(1)) },
                    label = { Text("Срок: 1 год от даты выдачи") },
                    leadingIcon = { Icon(Icons.Filled.Event, contentDescription = null) },
                )
            }
        }
        Text("Фото: лицевая и оборотная сторона", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhotoBox(photo1, { photo1 = it }, Modifier.weight(1f), height = 150.dp,
                placeholder = Icons.Filled.Description, label = "Сторона 1")
            PhotoBox(photo2, { photo2 = it }, Modifier.weight(1f), height = 150.dp,
                placeholder = Icons.Filled.Description, label = "Сторона 2")
        }
        TextInput(note, { note = it }, "Заметка (страховая, телефон, что покрывает)", singleLine = false, minLines = 2)
        Text("Уведомление придёт за 30 и за 7 дней до окончания срока.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
