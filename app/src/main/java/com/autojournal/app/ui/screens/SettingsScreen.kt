package com.autojournal.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autojournal.app.data.Backup
import com.autojournal.app.notify.Notifications
import com.autojournal.app.notify.ReminderScheduler
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.components.ConfirmDialog
import com.autojournal.app.ui.components.SectionCard
import com.autojournal.app.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val car by vm.activeCar.collectAsStateWithLifecycle()
    val refuels by vm.refuels.collectAsStateWithLifecycle()
    val services by vm.services.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    var canNotify by remember { mutableStateOf(Notifications.canNotify(ctx)) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = it
        if (!it) toast("Разрешите уведомления в настройках Android")
    }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val r = runCatching {
                val json = Backup.exportJson(vm.repo.db)
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
                }
            }
            toast(if (r.isSuccess) "Резервная копия сохранена" else "Ошибка: ${r.exceptionOrNull()?.message}")
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
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
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
            SectionCard(title = "Уведомления") {
                Text(
                    if (canNotify) "Включены. Проверка сроков — дважды в день."
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
                OutlinedButton(onClick = {
                    ReminderScheduler.runNow(ctx); toast("Проверяю напоминания…")
                }) {
                    Icon(Icons.Filled.NotificationsActive, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Проверить сейчас")
                }
            }

            SectionCard(title = "Резервная копия") {
                Text("Все автомобили, заправки, сервис, контакты и напоминания в одном файле. " +
                    "Сохраните его в облако или на компьютер. Фото в копию не входят.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { exportJson.launch("autojournal-${LocalDate.now()}.json") }) {
                    Icon(Icons.Filled.Backup, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Сохранить копию")
                }
                OutlinedButton(onClick = { importJson.launch(arrayOf("application/json", "*/*")) }) {
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
                Text("АвтоЖурнал 1.0 — все данные хранятся только на телефоне.",
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
                    val r = runCatching {
                        val text = withContext(Dispatchers.IO) {
                            ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                        }
                        Backup.importJson(vm.repo.db, text)
                    }
                    toast(r.fold({ "Восстановлено автомобилей: $it" }, { "Ошибка: ${it.message}" }))
                }
            },
        )
    }
}
