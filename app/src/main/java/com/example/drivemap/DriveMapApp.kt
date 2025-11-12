package com.example.drivemap

import android.app.Application
import com.example.drivemap.data.AppContainer

class DriveMapApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
