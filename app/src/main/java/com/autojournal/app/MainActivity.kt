package com.autojournal.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autojournal.app.notify.Notifications
import com.autojournal.app.ui.AppRoot
import com.autojournal.app.ui.theme.AutoJournalTheme
import com.autojournal.app.ui.theme.LocalExtras
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** Команда из уведомления, ярлыка или виджета: что открыть. */
    private val command = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) command.value = commandFrom(intent)
        val repo = (application as App).repo
        setContent {
            val style by repo.style.collectAsStateWithLifecycle()
            val cmd by command.collectAsStateWithLifecycle()
            AutoJournalTheme(style) {
                val dark = LocalExtras.current.isDark
                LaunchedEffect(dark) {
                    val bar = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
                }
                AppRoot(command = cmd, onCommandHandled = { command.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        command.value = commandFrom(intent)
    }

    private fun commandFrom(intent: Intent?): String? = when (intent?.action) {
        ACTION_NEW_REFUEL -> CMD_REFUEL
        ACTION_NEW_SERVICE -> CMD_SERVICE
        ACTION_ODOMETER -> CMD_ODOMETER
        else -> intent?.getStringExtra(Notifications.EXTRA_OPEN)
    }

    companion object {
        const val ACTION_NEW_REFUEL = "com.autojournal.app.NEW_REFUEL"
        const val ACTION_NEW_SERVICE = "com.autojournal.app.NEW_SERVICE"
        const val ACTION_ODOMETER = "com.autojournal.app.ODOMETER"
        const val CMD_REFUEL = "new_refuel"
        const val CMD_SERVICE = "new_service"
        const val CMD_ODOMETER = "odometer"
    }
}
