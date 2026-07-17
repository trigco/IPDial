package com.ipdial.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.QUICKBOOT_POWERON"
            )
        ) {
            // Pass delayStartForeground=true because FGS with phoneCall type is not allowed from BOOT_COMPLETED on Android 12+
            SipService.start(context, delayStartForeground = true)
        }
    }
}
