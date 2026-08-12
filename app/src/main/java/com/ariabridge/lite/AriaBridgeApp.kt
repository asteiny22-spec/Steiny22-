package com.ariabridge.lite

import android.app.Application
import org.lsposed.hiddenapibypass.HiddenApiBypass

class AriaBridgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/sun/")
        } catch (_: Throwable) {
        }
    }
}
