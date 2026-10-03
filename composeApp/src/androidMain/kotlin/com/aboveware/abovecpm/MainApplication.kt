package com.aboveware.abovecpm

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setAndroidContext(this)
    }
}