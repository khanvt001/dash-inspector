package com.dashinspector

import android.app.Application

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val dashInspector = DashInspector()
        dashInspector.initialize(this)
    }
}