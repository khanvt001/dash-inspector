package com.dashinspector

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TestApplication : Application() {

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Initialize DashInspector for runtime debugging

        scope.launch {
            DashInspector.init(this@TestApplication)
        }
    }
}