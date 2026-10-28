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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.autojournal.app.data.Contact
import com.autojournal.app.data.ServiceWithParts
import com.autojournal.app.domain.DueState
import com.autojournal.app.domain.ReminderStatus
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.Routes
import com.autojournal.app.ui.components.*
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Fmt
import com.autojournal.app.util.Intents

/** Что покупать к ближайшему ТО: запчасти из прошлой такой же замены. */
private data class ShopGroup(val status: ReminderStatus, val last: ServiceWithParts?, val placeName: String?, val phone: String?)

private fun buildGroups(
    statuses: List<ReminderStatus>,
    services: List<ServiceWithParts>,
    contacts: List<Contact>,
    showAll: Boolean,
): List<ShopGroup> =
    statuses.filter { it.state != DueState.OFF && (showAll || it.state != DueState.OK) }.map { st ->
        val last = services.filter { it.record.category == st.reminder.category }
            .sortedByDescending { it.record.date }
            .firstOrNull { it.parts.isNotEmpty() }
            ?: services.filter { it.record.category == st.reminder.category }.maxByOrNull { it.record.date }
        val contact = contacts.firstOrNull { it.id == last?.record?.contactId }
        ShopGroup(st, last, contact?.name ?: last?.record?.place?.ifBlank { null }, contact?.phone?.ifBlank { null })
    }

@Composable
fun ShoppingScreen(vm: MainViewModel, nav: NavController) {
    val ctx = LocalContext.current
    val statuses by vm.reminderStatuses.collectAsStateWithLifecycle()
    val services by vm.services.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val car by vm.activeCar.collectAsStateWithLifecycle()
    var showAll by remember { mutableStateOf(false) }
    val checked = remember { mutableStateMapOf<String, Boolean>() }
    val groups = remember(statuses, services, contacts, showAll) { buildGroups(statuses, services, contacts, showAll) }

    fun shareText(): String = buildString {
        appendLine("Покупки к ТО — ${car?.name ?: ""}")
        groups.forEach { g ->
            appendLine()
            appendLine("■ ${g.status.reminder.title} (${g.status.describe().lowercase()})")
            val parts = g.last?.parts.orEmpty()
            if (parts.isEmpty()) appendLine("  — подобрать запчасти")
            parts.forEach { p ->
                append("  • ${p.name}")
                if (p.partNumber.isNotBlank()) append(" [${p.partNumber}]")
                if (p.quantity != 1.0) append(" × ${Fmt.numTrim(p.quantity)}")
                if (p.price > 0) append(" — было ${Fmt.money(p.price)}")
                appendLine()
            }
        }
    }

    ListScaffold(
        title = "Покупки к ТО",
        onBack = { nav.popBackStack() },
        fabText = null,
        onFab = {},
        actions = {
            IconButton(onClick = { Intents.shareText(ctx, "Покупки к ТО", shareText()) }, enabled = groups.isNotEmpty()) {
                Icon(Icons.Filled.Share, contentDescription = "Поделиться списком")
            }
        },
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (showAll) "Все напоминания" else "Только то, что скоро или просрочено",
                    Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(checked = showAll, onCheckedChange = { showAll = it })
            }
        }
        if (groups.isEmpty()) {
            item {
                EmptyState(Icons.Filled.ShoppingCart, "Пока ничего не нужно",
                    "Когда подойдёт срок замены, здесь появится список запчастей из прошлого раза — " +
                        "с артикулами, ценами и магазином. Включите «Все напоминания», чтобы посмотреть заранее.")
            }
        }
        items(groups, key = { it.status.reminder.id }) { g ->
            val color = statusColor(g.status.state)
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(categoryIcon(g.status.reminder.category), color, 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.status.reminder.title, fontWeight = FontWeight.SemiBold)
                        Text(g.status.describe(), color = color, style = MaterialTheme.typography.bodySmall)
                    }
                }
                val parts = g.last?.parts.orEmpty()
                if (parts.isEmpty()) {
                    Text(
                        if (g.last == null) "Прошлых записей нет — после замены добавьте запчасти в «Сервис», и в следующий раз список соберётся сам."
                        else "В прошлый раз запчасти не записаны.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("В прошлый раз (${Dates.format(g.last!!.record.date)}):", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    parts.forEach { p ->
                        val key = "${g.status.reminder.id}:${p.id}"
                        val done = checked[key] == true
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { checked[key] = !done },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = done, onCheckedChange = { checked[key] = it })
                            Column(Modifier.weight(1f)) {
                                Text(p.name + if (p.quantity != 1.0) " × ${Fmt.numTrim(p.quantity)}" else "",
                                    textDecoration = if (done) TextDecoration.LineThrough else null)
                                if (p.partNumber.isNotBlank()) {
                                    Text(p.partNumber, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (p.price > 0) Text(Fmt.money(p.price), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (g.placeName != null) {
                        Text("Где делали: ${g.placeName}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        g.phone?.let { ph ->
                            IconButton(onClick = { Intents.dial(ctx, ph) }) {
                                Icon(Icons.Filled.Phone, contentDescription = "Позвонить")
                            }
                        }
                    } else Spacer(Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate(Routes.service(0, g.status.reminder.category)) }) {
                        Text("Записать замену")
                    }
                }
            }
        }
    }
}
