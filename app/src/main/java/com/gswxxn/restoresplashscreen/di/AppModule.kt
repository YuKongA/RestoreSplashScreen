package com.gswxxn.restoresplashscreen.di

import com.gswxxn.restoresplashscreen.manager.XposedServiceManager
import com.gswxxn.restoresplashscreen.provider.AppPreferenceActions
import com.gswxxn.restoresplashscreen.repository.GlobalPreferencesRepository
import com.gswxxn.restoresplashscreen.state.GlobalUIViewModel
import com.gswxxn.restoresplashscreen.utils.RemotePreferenceStore
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::XposedServiceManager) { createdAtStart() }

    singleOf(::RemotePreferenceStore)

    singleOf(::GlobalPreferencesRepository) { createdAtStart() }
    singleOf(::AppPreferenceActions)

    viewModelOf(::GlobalUIViewModel)
}
