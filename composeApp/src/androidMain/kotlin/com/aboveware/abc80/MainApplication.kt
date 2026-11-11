package com.aboveware.abc80

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setAndroidContext(this)
    }
}