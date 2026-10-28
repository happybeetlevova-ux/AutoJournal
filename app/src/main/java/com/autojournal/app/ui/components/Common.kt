package com.autojournal.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.autojournal.app.data.Car
import com.autojournal.app.domain.Categories
import com.autojournal.app.util.Dates
import com.autojournal.app.util.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ---------------- Каркас экрана редактирования ----------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScaffold(
    title: String,
    onBack: () -> Unit,
    onSave: (() -> Unit)?,
    onDelete: (() -> Unit)? = null,
    deleteText: String = "Удалить запись?",
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { confirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                        }
                    }
                    if (onSave != null) TextButton(onClick = onSave) { Text("Сохранить") }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .consumeWindowInsets(pad)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            if (onSave != null) {
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Сохранить")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirm && onDelete != null) {
        ConfirmDialog(
            title = deleteText,
            text = "Действие нельзя отменить.",
            confirmText = "Удалить",
            onConfirm = { confirm = false; onDelete() },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

// ---------------- Поля ввода ----------------

@Composable
fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    suffix: String? = null,
    supporting: String? = null,
    isError: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        suffix = if (suffix != null) { { Text(suffix) } } else null,
        supportingText = if (supporting != null) { { Text(supporting) } } else null,
        trailingIcon = trailing,
    )
}

@Composable
fun NumberInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    decimal: Boolean = true,
    supporting: String? = null,
    isError: Boolean = false,
) {
    TextInput(
        value = value,
        onChange = { s -> onChange(s.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }) },
        label = label,
        modifier = modifier,
        keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        suffix = suffix,
        supporting = supporting,
        isError = isError,
    )
}

/** Поле с выпадающим списком вариантов. editable = можно ввести своё значение. */
@Composable
fun OptionInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    supporting: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val shown = if (editable && value.isNotBlank()) {
        options.filter { it.contains(value.trim(), ignoreCase = true) && it != value }
    } else options
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it); expanded = true },
            readOnly = !editable,
            label = { Text(label) },
            singleLine = true,
            supportingText = if (supporting != null) { { Text(supporting) } } else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Выбрать")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (!editable) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
            )
        }
        DropdownMenu(
            expanded = expanded && shown.isNotEmpty(),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = !editable),
            modifier = Modifier.heightIn(max = 320.dp),
        ) {
            shown.forEach { o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onChange(o); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInput(label: String, value: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = Dates.format(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { show = true })
    }
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(onChange)
                    show = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Отмена") } },
        ) {
            DatePicker(state = state)
        }
    }
}

// ---------------- Фото ----------------

@Composable
fun PhotoBox(
    path: String?,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    placeholder: ImageVector = Icons.Filled.DirectionsCar,
    label: String = "Добавить фото",
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var viewer by remember { mutableStateOf(false) }
    if (viewer && path != null) PhotoViewer(path) { viewer = false }

    fun importUri(uri: Uri) {
        scope.launch {
            val p = withContext(Dispatchers.IO) { Photos.import(ctx, uri) }
            if (p != null) onChange(p)
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importUri(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = cameraUri
        if (ok && u != null) importUri(u)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { menu = true },
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    placeholder, contentDescription = null, modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Сделать фото") },
                    leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                    onClick = {
                        menu = false
                        val u = Photos.newCameraUri(ctx)
                        cameraUri = u
                        camera.launch(u)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Выбрать из галереи") },
                    leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                    onClick = {
                        menu = false
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
                if (path != null) {
                    DropdownMenuItem(
                        text = { Text("Открыть") },
                        leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                        onClick = { menu = false; viewer = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Убрать фото") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menu = false; onChange(null) },
                    )
                }
            }
        }
    }
}

@Composable
fun CarAvatar(car: Car?, size: Dp) {
    val p = car?.photoPath
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (p != null) {
            AsyncImage(
                model = File(p), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.DirectionsCar, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

// ---------------- Мелкие элементы ----------------

@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = cardBorder(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null || action != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (title != null) {
                        Text(
                            title, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                        )
                    }
                    if (action != null) action()
                }
            }
            content()
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun LabelValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)

fun categoryIcon(cat: String): ImageVector = when {
    cat == Categories.FUEL -> Icons.Filled.LocalGasStation
    cat.contains("масло", true) || cat.contains("жидкость", true) || cat == "Антифриз" -> Icons.Filled.Opacity
    cat.contains("фильтр", true) -> Icons.Filled.FilterList
    cat.contains("Свечи") -> Icons.Filled.FlashOn
    cat.contains("Тормоз") -> Icons.Filled.DiscFull
    cat.contains("Аккумулятор") -> Icons.Filled.BatteryChargingFull
    cat.contains("Шины") -> Icons.Filled.TripOrigin
    cat.contains("Мойка") -> Icons.Filled.LocalCarWash
    cat.contains("Страховка") -> Icons.Filled.Security
    cat.contains("Техосмотр") || cat.contains("Диагностика") -> Icons.Filled.Speed
    cat.contains("Штраф") -> Icons.Filled.Gavel
    cat.contains("Парковка") -> Icons.Filled.LocalParking
    cat.contains("Налог") -> Icons.Filled.Receipt
    cat.contains("Кузов") -> Icons.Filled.Brush
    else -> Icons.Filled.Build
}

/** Стабильный цвет для категории (для иконок и диаграмм). */
fun categoryColor(cat: String): Color {
    if (cat == Categories.FUEL) return Color(0xFF1E88E5)
    val palette = listOf(
        Color(0xFFE07A10), Color(0xFF2E9D46), Color(0xFF8E44AD), Color(0xFFD93025),
        Color(0xFF00897B), Color(0xFF6D4C41), Color(0xFFC2185B), Color(0xFF5C6BC0),
    )
    return palette[(cat.hashCode() and 0x7fffffff) % palette.size]
}
