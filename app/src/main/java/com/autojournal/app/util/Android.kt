package com.autojournal.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object Photos {
    private fun dir(ctx: Context) = File(ctx.filesDir, "photos").apply { mkdirs() }

    /** Копирует выбранное фото во внутреннее хранилище приложения. */
    fun import(ctx: Context, uri: Uri): String? = runCatching {
        val f = File(dir(ctx), UUID.randomUUID().toString() + ".jpg")
        ctx.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            f.outputStream().use { input.copyTo(it) }
        }
        f.absolutePath
    }.getOrNull()

    fun newCameraUri(ctx: Context): Uri {
        val d = File(ctx.cacheDir, "camera").apply { mkdirs() }
        val f = File(d, "shot_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
    }
}

object Intents {
    private fun start(ctx: Context, intent: Intent) {
        try {
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "Нет приложения для этого действия", Toast.LENGTH_SHORT).show()
        }
    }

    fun dial(ctx: Context, phone: String) {
        val clean = phone.filter { it.isDigit() || it == '+' }
        if (clean.isEmpty()) return
        start(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
    }

    fun map(ctx: Context, address: String) {
        if (address.isBlank()) return
        start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address))))
    }

    fun shareText(ctx: Context, title: String, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        start(ctx, Intent.createChooser(send, title))
    }
}
