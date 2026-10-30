package com.tybit

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * The home-screen icon choices. Each is an `<activity-alias>` in the manifest
 * carrying the launcher intent filter; exactly one is enabled at a time.
 *
 * All of them are TyBIT's T on a plain adaptive icon — the launcher gives each
 * its own shape, so they sit with the phone's other icons whatever it uses.
 * The art is cut by branding/buat_ikon.py.
 */
enum class AppIcon(val alias: String, val iconRes: Int, val labelRes: Int) {
    Default("LauncherDefault", R.mipmap.ic_launcher, R.string.app_icon_default),
    Gold("LauncherGold", R.mipmap.ic_icon_gold, R.string.app_icon_gold),
    Midnight("LauncherMidnight", R.mipmap.ic_icon_midnight, R.string.app_icon_midnight),
    ClearGlass("LauncherClearGlass", R.mipmap.ic_icon_clear, R.string.app_icon_clear_glass),
    DarkGlass("LauncherDarkGlass", R.mipmap.ic_icon_clear_dark, R.string.app_icon_dark_glass);

    private fun component(context: Context) =
        ComponentName(context.packageName, "com.tybit.$alias")

    companion object {
        fun current(context: Context): AppIcon {
            val pm = context.packageManager
            return entries.firstOrNull {
                pm.getComponentEnabledSetting(it.component(context)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } ?: Default
        }

        /**
         * Puts the default icon back when no choice is enabled — what an update
         * that drops the alias the listener had picked leaves behind: Default
         * disabled, the picked one gone, and the app missing from the launcher
         * with nothing left to tap. Run after every update ([LauncherIconRepair])
         * and at every start.
         */
        fun ensureLauncherEntry(context: Context) {
            val pm = context.packageManager
            val picked = entries.any {
                it != Default && pm.getComponentEnabledSetting(it.component(context)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
            val default = Default.component(context)
            if (!picked && pm.getComponentEnabledSetting(default) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                    default,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }

        /** Enables [icon] before disabling the rest so there is never a moment with no launcher entry. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                icon.component(context),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(
                    it.component(context),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}

/**
 * Runs [AppIcon.ensureLauncherEntry] right after an update is installed. Android
 * delivers MY_PACKAGE_REPLACED without the app being opened, which matters
 * here: an app with no launcher entry is one nobody can open.
 */
class LauncherIconRepair : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) AppIcon.ensureLauncherEntry(context)
    }
}
