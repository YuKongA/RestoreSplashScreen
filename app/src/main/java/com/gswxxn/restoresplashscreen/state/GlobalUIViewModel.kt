package com.gswxxn.restoresplashscreen.state

import androidx.lifecycle.ViewModel
import com.gswxxn.restoresplashscreen.repository.GlobalPreferencesRepository

class GlobalUIViewModel(
    private val repo: GlobalPreferencesRepository
) : ViewModel() {
    val configFlow = repo.uiConfigFlow
}
