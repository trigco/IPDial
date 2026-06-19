package com.ipdial.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconHelper {
    private const val MAIN_ACTIVITY = "com.ipdial.MainActivity"
    
    private val ALIASES = mapOf(
        "Default" to MAIN_ACTIVITY,
        "Green"   to "com.ipdial.MainActivityGreen",
        "Blue"    to "com.ipdial.MainActivityBlue",
        "Red"     to "com.ipdial.MainActivityRed"
    )

    fun setAppIcon(context: Context, aliasName: String) {
        val pm = context.packageManager
        val packageName = context.packageName
        
        val targetAlias = ALIASES[aliasName] ?: MAIN_ACTIVITY

        // In Android, at least one activity with ACTION_MAIN/CATEGORY_LAUNCHER must be enabled.
        // To avoid crashes and "double icons", we enable ONLY the target and disable ALL others.
        
        val componentsToDisable = ALIASES.values.toMutableSet()
        componentsToDisable.remove(targetAlias)

        // 1. Enable the target first to ensure there's always an entry point
        pm.setComponentEnabledSetting(
            ComponentName(packageName, targetAlias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // 2. Disable all other variants
        componentsToDisable.forEach { component ->
            pm.setComponentEnabledSetting(
                ComponentName(packageName, component),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
