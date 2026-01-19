package com.dashinspector

import android.app.Application

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize DashInspector for runtime debugging
        DashInspector.init(this)
    }
}