package com.core.voidapp

import android.app.Application
import com.core.voidapp.data.VoidRepository

class VoidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VoidRepository.init(this)
    }
}
