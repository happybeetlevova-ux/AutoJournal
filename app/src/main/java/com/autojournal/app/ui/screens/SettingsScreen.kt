package com.autojournal.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autojournal.app.data.Backup
import com.autojournal.app.notify.Notifications
import com.autojournal.app.notify.ReminderScheduler
import com.autojournal.app.notify.ReminderWorker
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.components.ConfirmDialog
import com.autojournal.app.ui.components.SectionCard
import com.autojournal.app.ui.theme.AppStyle
import com.autojournal.app.ui.theme.extrasFor
import com.autojournal.app.util.AppIcon
import com.autojournal.app.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val car by vm.activeCar.collectAsStateWithLifecycle()
    val refuels by vm.refuels.collectAsStateWithLifecycle()
    val services by vm.services.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val style by vm.style.collectAsStateWithLifecycle()
    var canNotify by remember { mutableStateOf(Notifications.canNotify(ctx)) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var icon by remember { mutableStateOf(AppIcon.current(ctx)) }
    val prefs = vm.repo.prefs
    var autoUri by remember { mutableStateOf(prefs.getString(ReminderWorker.KEY_AUTO_URI, null)) }
    var busy by remember { mutableStateOf(false) }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = it
        if (!it) toast("Разрешите уведомления в настройках Android")
    }
    val exportZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val r = runCatching {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)!!.use { Backup.exportZip(vm.repo.db, it) }
                }
            }
            busy = false
            toast(if (r.isSuccess) "Копия сохранена (с фотографиями)" else "Ошибка: ${r.exceptionOrNull()?.message}")
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val c = car
        if (uri != null && c != null) scope.launch {
            val r = runCatching {
                val csv = Backup.exportCsv(c, refuels, services, contacts)
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)!!.use { it.write(csv.toByteArray()) }
                }
            }
            toast(if (r.isSuccess) "Таблица сохранена" else "Ошибка: ${r.exceptionOrNull()?.message}")
        }
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            prefs.edit().putString(ReminderWorker.KEY_AUTO_URI, uri.toString())
                .putLong(ReminderWorker.KEY_AUTO_LAST, 0L).apply()
            autoUri = uri.toString()
            scope.launch {
                busy = true
                val r = runCatching { withContext(Dispatchers.IO) { Backup.autoBackupToFolder(ctx, vm.repo.db, uri) } }
                busy = false
                if (r.isSuccess) {
                    prefs.edit().putLong(ReminderWorker.KEY_AUTO_LAST, System.currentTimeMillis()).apply()
                    toast("Готово: первая копия уже в папке, дальше — раз в неделю")
                } else toast("Не удалось записать в папку: ${r.exceptionOrNull()?.message}")
            }
        }
    }

    fun shareBackup() = scope.launch {
        busy = true
        val r = runCatching {
            withContext(Dispatchers.IO) {
                val dir = File(ctx.cacheDir, "share").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                val f = File(dir, Backup.fileName())
                f.outputStream().use { Backup.exportZip(vm.repo.db, it) }
                FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
            }
        }
        busy = false
        r.onSuccess { uri ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(send, "Сохранить копию в…"))
        }.onFailure { toast("Ошибка: ${it.message}") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            SectionCard(title = "Оформление") {
                AppStyle.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { s ->
                            StylePreview(s, selected = s == style, modifier = Modifier.weight(1f)) {
                                vm.repo.setStyle(s)
                            }
                        }
                    }
                }
            }

            SectionCard(title = "Иконка на рабочем столе") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AppIcon.entries.forEach { ic ->
                        Column(
                            Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                if (ic != icon) {
                                    AppIcon.set(ctx, ic)
                                    icon = ic
                                    toast("Иконка сменится через несколько секунд")
                                }
                            }.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(54.dp).clip(CircleShape)
                                    .then(
                                        if (ic == icon) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    ),
                            ) {
                                Image(painterResource(ic.background), null, Modifier.fillMaxSize().scale(1.5f))
                                Image(painterResource(ic.foreground), null, Modifier.fillMaxSize().scale(1.5f))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(ic.title, style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (ic == icon) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Text("Некоторые лаунчеры обновляют иконку с задержкой. Ярлык на рабочем столе, возможно, придётся вынести заново.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionCard(title = "Уведомления") {
                Text(
                    if (canNotify) "Включены. Проверка сроков замен и документов — дважды в день."
                    else "Выключены — напоминания о замене не придут.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!canNotify) {
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        )
                    }) { Text("Включить уведомления") }
                }
                OutlinedButton(onClick = { ReminderScheduler.runNow(ctx); toast("Проверяю напоминания…") }) {
                    Icon(Icons.Filled.NotificationsActive, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Проверить сейчас")
                }
            }

            SectionCard(title = "Автокопия") {
                if (autoUri == null) {
                    Text("Раз в неделю приложение само сохранит полную копию (с фото) в выбранную папку " +
                        "и оставит 5 последних. Выберите папку, например «Documents» или папку Яндекс Диска.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { pickFolder.launch(null) }) {
                        Icon(Icons.Filled.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Выбрать папку")
                    }
                } else {
                    val last = prefs.getLong(ReminderWorker.KEY_AUTO_LAST, 0L)
                    val err = prefs.getString(ReminderWorker.KEY_AUTO_ERROR, null)
                    Text("Включена: ${Uri.parse(autoUri ?: "").lastPathSegment?.substringAfter(':') ?: "папка"}",
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        if (last > 0) "Последняя копия: " +
                            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(last))
                        else "Копий пока не было",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (err != null) Text("Ошибка: $err", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickFolder.launch(null) }) { Text("Другая папка") }
                        TextButton(onClick = {
                            prefs.edit().remove(ReminderWorker.KEY_AUTO_URI).apply(); autoUri = null
                        }) { Text("Выключить") }
                    }
                }
                HorizontalDivider()
                Text("В Google Диск: нажмите «Отправить копию» и выберите «Диск». " +
                    "Кроме того, Android сам сохраняет данные приложения в ваш аккаунт Google и вернёт их на новом телефоне.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { shareBackup() }) {
                    Icon(Icons.Filled.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Отправить копию")
                }
            }

            SectionCard(title = "Резервная копия вручную") {
                Text("Все автомобили, заправки, сервис, документы, шины, контакты и фото в одном ZIP-файле.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { exportZip.launch(Backup.fileName()) }) {
                    Icon(Icons.Filled.Backup, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Сохранить копию")
                }
                OutlinedButton(onClick = { importFile.launch(arrayOf("application/zip", "application/json", "*/*")) }) {
                    Icon(Icons.Filled.Restore, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Восстановить из копии")
                }
            }

            SectionCard(title = "Экспорт в Excel") {
                Text("Таблица CSV по текущему автомобилю: заправки и обслуживание с ценами, запчастями и местами.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = {
                    val name = (car?.name ?: "auto").replace(Regex("[^\\p{L}\\p{N}]+"), "_")
                    exportCsv.launch("$name-${LocalDate.now()}.csv")
                }, enabled = car != null) {
                    Icon(Icons.Filled.TableChart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Экспорт CSV")
                }
            }

            SectionCard(title = "О приложении") {
                Text("АвтоЖурнал 2.0 — все данные хранятся на телефоне.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Сегодня: ${Dates.format(Dates.today())}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = "Восстановить данные?",
            text = "Текущие данные на телефоне будут заменены содержимым копии.",
            confirmText = "Восстановить",
            onDismiss = { pendingRestore = null },
            onConfirm = {
                pendingRestore = null
                scope.launch {
                    busy = true
                    val r = runCatching { withContext(Dispatchers.IO) { Backup.restore(ctx, vm.repo.db, uri) } }
                    busy = false
                    toast(r.fold({ "Восстановлено автомобилей: $it" }, { "Ошибка: ${it.message}" }))
                }
            },
        )
    }
}

/** Мини-превью темы: фон, карточка героя, акцент. */
@Composable
private fun StylePreview(s: AppStyle, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val e = extrasFor(s, false)
    val bg = when (s) {
        AppStyle.GARAGE -> Color(0xFF14161A)
        AppStyle.PREMIUM -> Color(0xFFF6F4EF)
        AppStyle.NEON -> Color(0xFF0A0C1E)
        AppStyle.CLASSIC -> Color(0xFFF7F9FC)
    }
    val tile = when (s) {
        AppStyle.GARAGE -> Color(0xFF1F2228)
        AppStyle.PREMIUM -> Color.White
        AppStyle.NEON -> Color(0xFF14173A)
        AppStyle.CLASSIC -> Color(0xFFEDF1F7)
    }
    val text = if (s == AppStyle.PREMIUM || s == AppStyle.CLASSIC) Color(0xFF16181C) else Color.White
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .border(BorderStroke(if (selected) 3.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(e.heroColors))
                .then(if (e.glow) Modifier.border(1.dp, e.heroAccent.copy(alpha = 0.6f), RoundedCornerShape(10.dp)) else Modifier)
                .padding(6.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Box(Modifier.width(40.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(e.heroAccent))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(6.dp)).background(tile)) {
                Box(Modifier.padding(5.dp).width(18.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(e.soon))
            }
            Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(6.dp)).background(tile)) {
                Box(Modifier.padding(5.dp).width(24.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(e.ok))
            }
        }
        Text(s.title, color = text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(s.description, color = text.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall,
            maxLines = 2, minLines = 2, textAlign = TextAlign.Start)
    }
}
