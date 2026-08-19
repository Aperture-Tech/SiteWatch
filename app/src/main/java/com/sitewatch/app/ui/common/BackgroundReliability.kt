package com.sitewatch.app.ui.common

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Helpers for the two things that most often make periodic checks "not work in
 * the background": aggressive battery optimization killing the app, and the
 * notification permission being denied (checks run, but no alert is shown).
 *
 * We intentionally do NOT declare the sensitive
 * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission (which triggers extra Play
 * review). Instead we send the user to the system settings screen to grant the
 * exemption themselves.
 */

/** True if the app is exempt from battery optimization (background work runs reliably). */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/** Opens the system battery-optimization list so the user can mark SiteWatch "Don't optimize". */
fun openBatteryOptimizationSettings(context: Context) {
    val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    try {
        context.startActivity(listIntent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(appDetailsIntent(context))
    }
}

/** True on pre-Android-13, or when POST_NOTIFICATIONS has been granted. */
fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

/** Opens this app's system notification settings. */
fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(appDetailsIntent(context))
    }
}

private fun appDetailsIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
