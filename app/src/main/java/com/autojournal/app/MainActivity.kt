package com.autojournal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.autojournal.app.notify.Notifications
import com.autojournal.app.ui.AppRoot
import com.autojournal.app.ui.theme.AutoJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openTab = intent?.getStringExtra(Notifications.EXTRA_OPEN)
        setContent {
            AutoJournalTheme {
                AppRoot(openTab = openTab)
            }
        }
    }
}
