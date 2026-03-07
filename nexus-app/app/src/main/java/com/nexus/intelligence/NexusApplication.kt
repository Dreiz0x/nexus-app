package com.nexus.intelligence

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexusApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Application-level initialization can go here
    }
}
