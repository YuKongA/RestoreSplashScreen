package com.gswxxn.restoresplashscreen

import android.app.Application
import com.gswxxn.restoresplashscreen.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RestoreSplashScreenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RestoreSplashScreenApp)
            modules(appModule)
        }
    }
}
