package com.autojournal.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.autojournal.app.data.Backup
import com.autojournal.app.data.Car
import com.autojournal.app.domain.Categories
import com.autojournal.app.ui.MainViewModel
import com.autojournal.app.ui.Routes
import com.autojournal.app.ui.components.*
import com.autojournal.app.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(vm: MainViewModel, nav: NavController) {
    val cars by vm.cars.collectAsStateWithLifecycle()
    val active by vm.activeCar.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Гараж") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.car(0)) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Автомобиль") },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = ListPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(cars.orEmpty(), key = { it.id }) { c ->
                GarageCard(
                    car = c,
                    isActive = c.id == active?.id,
                    onSelect = { vm.repo.setActiveCar(c.id); nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.car(c.id)) },
                )
            }
        }
    }
}

@Composable
private fun GarageCard(car: Car, isActive: Boolean, onSelect: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onSelect),
        shape = RoundedCornerShape(22.dp),
        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.primaryContainer)) {
            val p = car.photoPath
            if (p != null) {
                AsyncImage(File(p), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.DirectionsCar, null, Modifier.size(90.dp).align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
            }
            if (isActive) {
                AssistChip(
                    onClick = {}, label = { Text("Выбран") },
                    leadingIcon = { Icon(Icons.Filled.CheckCircle, null) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color.White),
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                )
            }
            FilledTonalIconButton(onClick = onEdit, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.Edit, "Изменить")
            }
        }
        Column(Modifier.padding(14.dp)) {
            Text(car.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val sub = listOfNotNull(
                listOf(car.make, car.model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
                car.year?.toString(), car.plate.ifBlank { null }, car.fuelType.ifBlank { null },
            ).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun CarEditScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    var existing by remember { mutableStateOf<Car?>(null) }
    var name by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var fuel by remember { mutableStateOf("АИ-95") }
    var tank by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }
    var defaults by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.repo.db.cars().get(id)?.let { c ->
                existing = c
                name = c.name; make = c.make; model = c.model; year = c.year?.toString() ?: ""
                plate = c.plate; vin = c.vin; fuel = c.fuelType; tank = c.tankVolume?.let { Fmt.input(it) } ?: ""
                odo = if (c.initialOdometer > 0) c.initialOdometer.toString() else ""
                note = c.note; photo = c.photoPath
            }
        }
    }

    val save: () -> Unit = save@{
        val autoName = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ").trim()
        val finalName = name.trim().ifBlank { autoName }
        if (finalName.isBlank()) { error = "Укажите название или марку"; return@save }
        val car = Car(
            id = existing?.id ?: 0L, name = finalName, make = make.trim(), model = model.trim(),
            year = Fmt.parseInt(year), plate = plate.trim().uppercase(), vin = vin.trim().uppercase(),
            fuelType = fuel.trim(), tankVolume = Fmt.parse(tank), initialOdometer = Fmt.parseInt(odo) ?: 0,
            photoPath = photo, note = note.trim(),
        )
        val isNew = existing == null
        vm.io {
            val newId = saveCar(car, addDefaultReminders = isNew && defaults)
            if (isNew) setActiveCar(newId)
        }
        onBack()
    }

    EditScaffold(
        title = if (id == 0L) "Новый автомобиль" else "Автомобиль",
        onBack = onBack, onSave = save,
        onDelete = existing?.let { c -> { vm.io { deleteCar(c) }; onBack() } },
        deleteText = "Удалить автомобиль со всеми заправками, сервисом и напоминаниями?",
    ) {
        PhotoBox(photo, { photo = it }, height = 210.dp, label = "Фото автомобиля")
        TextInput(name, { name = it }, "Название (например: «Рабочая» или «Солярис»)")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextInput(make, { make = it }, "Марка", Modifier.weight(1f))
            TextInput(model, { model = it }, "Модель", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(year, { year = it.take(4) }, "Год", Modifier.weight(1f), decimal = false)
            TextInput(plate, { plate = it }, "Госномер", Modifier.weight(1.4f),
                capitalization = KeyboardCapitalization.Characters)
        }
        TextInput(vin, { vin = it.take(17) }, "VIN", capitalization = KeyboardCapitalization.Characters,
            supporting = "Пригодится для подбора запчастей")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { OptionInput(fuel, { fuel = it }, "Топливо", Categories.fuelGrades) }
            NumberInput(tank, { tank = it }, "Бак", Modifier.weight(1f), suffix = "л")
        }
        NumberInput(odo, { odo = it }, "Пробег сейчас", suffix = "км", decimal = false)
        TextInput(note, { note = it }, "Заметка (масло, шины, размеры щёток…)", singleLine = false, minLines = 2)
        if (existing == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = defaults, onCheckedChange = { defaults = it })
                Column {
                    Text("Добавить типовые напоминания")
                    Text("Масло, фильтры, свечи, тормозная жидкость, ГРМ, шины, ОСАГО",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun WelcomeScreen(vm: MainViewModel, onAddCar: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val msg = runCatching {
                val n = withContext(Dispatchers.IO) { Backup.restore(ctx, vm.repo.db, uri) }
                "Восстановлено автомобилей: $n"
            }.getOrElse { "Не удалось восстановить: ${it.message}" }
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(120.dp).clip(RoundedCornerShape(36.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DirectionsCar, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.height(24.dp))
            Text("АвтоЖурнал", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Заправки и расход, замены масла и расходников с напоминаниями, цены работ и запчастей, " +
                    "контакты мастеров — для одной или нескольких машин.",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onAddCar, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить автомобиль")
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { restore.launch(arrayOf("application/zip", "application/json", "*/*")) }) {
                Text("Восстановить из резервной копии")
            }
        }
    }
}
