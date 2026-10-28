package com.autojournal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.autojournal.app.ui.theme.LocalExtras
import com.autojournal.app.util.Dates
import java.io.File

@Composable
fun cardBorder(): BorderStroke? = LocalExtras.current.cardBorder?.let { BorderStroke(1.dp, it) }

/** Фон экрана: для «Неона» — градиент ночного неба. */
fun Modifier.appBackground(brush: Brush?, color: Color): Modifier =
    if (brush != null) background(brush) else background(color)

/** Круговая шкала, как на приборной панели. progress 0..1. */
@Composable
fun Gauge(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    stroke: Dp = 11.dp,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val extras = LocalExtras.current
    val track = extras.gaugeTrack
    val glow = extras.glow
    val tick = if (extras.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.18f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val pad = sw / 2 + if (glow) sw * 0.6f else 0f
            val arcSize = Size(this.size.width - pad * 2, this.size.height - pad * 2)
            val tl = Offset(pad, pad)
            val start = 135f
            val sweep = 270f
            drawArc(track, start, sweep, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            val p = progress.coerceIn(0f, 1f)
            if (p > 0f) {
                if (glow) {
                    drawArc(color.copy(alpha = 0.18f), start, sweep * p, false, tl, arcSize,
                        style = Stroke(sw * 2.2f, cap = StrokeCap.Round))
                    drawArc(color.copy(alpha = 0.35f), start, sweep * p, false, tl, arcSize,
                        style = Stroke(sw * 1.5f, cap = StrokeCap.Round))
                }
                drawArc(color, start, sweep * p, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            }
            // Риски шкалы
            val r = arcSize.width / 2
            val c = Offset(this.size.width / 2, this.size.height / 2)
            for (i in 0..6) {
                val a = Math.toRadians((start + sweep * i / 6f).toDouble())
                val o = r - sw * 1.4f
                val inner = r - sw * 2.0f
                drawLine(
                    tick,
                    Offset(c.x + (o * Math.cos(a)).toFloat(), c.y + (o * Math.sin(a)).toFloat()),
                    Offset(c.x + (inner * Math.cos(a)).toFloat(), c.y + (inner * Math.sin(a)).toFloat()),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
                )
            }
        }
        center()
    }
}

@Composable
fun GaugePercent(progress: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface)
        Text("ресурса", style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Дата, которую можно не указывать. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionalDateInput(label: String, value: Long?, onChange: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value?.let { Dates.format(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("не указана") },
            singleLine = true,
            trailingIcon = if (value != null) {
                { IconButton(onClick = { onChange(null) }) { Icon(Icons.Filled.Close, contentDescription = "Очистить") } }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().padding(end = if (value != null) 48.dp else 0.dp).clickable { show = true })
    }
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value ?: Dates.today())
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = { onChange(state.selectedDateMillis); show = false }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Отмена") } },
        ) { DatePicker(state = state) }
    }
}

/** Полноэкранный просмотр фото (документы, чеки). */
@Composable
fun PhotoViewer(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onDismiss)) {
            AsyncImage(
                model = File(path), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White)
            }
        }
    }
}

/** Экран-список со своей шапкой и кнопкой «добавить». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScaffold(
    title: String,
    onBack: () -> Unit,
    fabText: String?,
    onFab: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                },
                actions = { actions() },
            )
        },
        floatingActionButton = {
            if (fabText != null) {
                ExtendedFloatingActionButton(
                    onClick = onFab,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(fabText) },
                )
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** Плитка-ссылка: иконка, заголовок, подпись. */
@Composable
fun LinkTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val border = cardBorder()
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(18.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconBadge(icon, tint, 34.dp)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1)
    }
}

/** Статус-плашка (бейдж). */
@Composable
fun StatusPill(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
