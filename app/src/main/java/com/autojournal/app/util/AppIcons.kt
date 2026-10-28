package com.autojournal.app.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.autojournal.app.R

/** Пять вариантов иконки на рабочем столе (activity-alias в манифесте). */
enum class AppIcon(val alias: String, val title: String, val background: Int, val foreground: Int) {
    CLASSIC("IconClassic", "Классика", R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground),
    GAUGE("IconGauge", "Спидометр", R.drawable.ic_icon_gauge_bg, R.drawable.ic_icon_gauge_fg),
    FRONT("IconFront", "Анфас", R.drawable.ic_icon_front_bg, R.drawable.ic_icon_front_fg),
    MINIMAL("IconMinimal", "Минимал", R.drawable.ic_icon_minimal_bg, R.drawable.ic_icon_minimal_fg),
    NEON("IconNeon", "Неон", R.drawable.ic_icon_neon_bg, R.drawable.ic_icon_neon_fg);

    fun component(ctx: Context) = ComponentName(ctx.packageName, "com.autojournal.app.$alias")

    companion object {
        fun current(ctx: Context): AppIcon {
            val pm = ctx.packageManager
            return entries.firstOrNull { icon ->
                when (pm.getComponentEnabledSetting(icon.component(ctx))) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == CLASSIC
                    else -> false
                }
            } ?: CLASSIC
        }

        /** Включает выбранную иконку и выключает остальные. Лаунчер обновится через несколько секунд. */
        fun set(ctx: Context, icon: AppIcon) {
            val pm = ctx.packageManager
            // Сначала включаем новую, чтобы у приложения ни на миг не пропала иконка.
            pm.setComponentEnabledSetting(
                icon.component(ctx), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(
                    it.component(ctx), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
