package com.ipdial

import android.app.Application
import com.ipdial.service.SipService

class IPDialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Start SIP service on app launch
        SipService.start(this)
    }
}
