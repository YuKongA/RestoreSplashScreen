package com.gswxxn.restoresplashscreen.ui

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.Route
import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.manager.XposedServiceManager
import com.gswxxn.restoresplashscreen.provider.AppPreferenceActions
import com.gswxxn.restoresplashscreen.repository.GlobalPreferencesRepository
import com.gswxxn.restoresplashscreen.state.GlobalUIViewModel
import com.gswxxn.restoresplashscreen.ui.apppage.BackgroundExceptPage
import com.gswxxn.restoresplashscreen.ui.apppage.BgIndividualPage
import com.gswxxn.restoresplashscreen.ui.apppage.CustomScopePage
import com.gswxxn.restoresplashscreen.ui.apppage.ForceSplashPage
import com.gswxxn.restoresplashscreen.ui.apppage.HideIconPage
import com.gswxxn.restoresplashscreen.ui.apppage.IgnoreAppIconPage
import com.gswxxn.restoresplashscreen.ui.apppage.MinDurationPage
import com.gswxxn.restoresplashscreen.ui.apppage.RemoveBrandingPage
import com.gswxxn.restoresplashscreen.ui.component.ColorPickerPage
import com.gswxxn.restoresplashscreen.ui.page.AboutPage
import com.gswxxn.restoresplashscreen.ui.page.BackgroundPage
import com.gswxxn.restoresplashscreen.ui.page.BasicPage
import com.gswxxn.restoresplashscreen.ui.page.BottomPage
import com.gswxxn.restoresplashscreen.ui.page.DevPage
import com.gswxxn.restoresplashscreen.ui.page.DisplayPage
import com.gswxxn.restoresplashscreen.ui.page.IconPage
import com.gswxxn.restoresplashscreen.ui.page.MainPage
import com.gswxxn.restoresplashscreen.ui.page.ScopePage
import dev.lackluster.hyperx.core.HyperXActivity
import dev.lackluster.hyperx.ui.layout.HyperXAppLayout
import dev.lackluster.hyperx.ui.preference.core.LocalPreferenceActions
import androidx.lifecycle.lifecycleScope
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : HyperXActivity() {
    companion object {
        val moduleActive: MutableState<Boolean> = mutableStateOf(false)
        val devMode: MutableState<Boolean> = mutableStateOf(false)
        val blurEnabled: MutableState<Boolean> = mutableStateOf(true)
        val splitEnabled: MutableState<Boolean> = mutableStateOf(true)

        val systemUIRestartNeeded = mutableStateOf(true)
        val androidRestartNeeded = mutableStateOf<Boolean?>(null)

        // Xposed 框架信息
        val xposedFrameworkName: MutableState<String> = mutableStateOf("Xposed")
        val xposedApiVersion: MutableState<Int> = mutableIntStateOf(0)
    }

    private val xposedServiceManager: XposedServiceManager by inject()
    private val globalPreferencesRepository: GlobalPreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            xposedServiceManager.serviceFlow.collect { service ->
                moduleActive.value = isModuleActivated(service)
                xposedFrameworkName.value = service?.frameworkName ?: "Xposed"
                xposedApiVersion.value = service?.apiVersion ?: 0
                // Xposed 服务绑定后才能读到真实的远程配置，在此刷新依赖持久化值的 UI 状态
                if (service != null) {
                    devMode.value = globalPreferencesRepository.get(Preferences.Dev.ENABLE_DEV_SETTINGS)
                    blurEnabled.value = globalPreferencesRepository.get(Preferences.Module.MODULE_APP_BLUR)
                    splitEnabled.value = globalPreferencesRepository.get(Preferences.Module.SPLIT_VIEW)
                }
                refreshRestartState()
            }
        }

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        refreshRestartState()
    }

    /**
     * 判断模块是否激活
     */
    private fun isModuleActivated(service: XposedService?): Boolean {
        if (service == null) return false
        val cap = service.frameworkProperties
        return service.apiVersion >= 101 &&
                (cap and XposedService.PROP_CAP_SYSTEM != 0L) &&
                (cap and XposedService.PROP_CAP_REMOTE != 0L)
    }

    /**
     * 刷新被 Hook 进程是否需要重启的状态
     */
    private fun refreshRestartState() {
        val state = xposedServiceManager.queryRestartState()
        systemUIRestartNeeded.value = state?.systemUI ?: false
        androidRestartNeeded.value = state?.android
    }

    @Composable
    override fun AppContent() {
        val globalUiVm: GlobalUIViewModel = koinViewModel()
        val uiConfig by globalUiVm.configFlow.collectAsState()

        val appPreferenceActions: AppPreferenceActions = koinInject()

        CompositionLocalProvider(
            LocalPreferenceActions provides appPreferenceActions
        ) {
            HyperXAppLayout(
                config = uiConfig,
                primaryContent = { MainPage() },
                emptyContent = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            modifier = Modifier.size(256.dp),
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.secondary)
                        )
                    }
                },
                customEntryProvider = { key ->
                    NavEntry(key) {
                        when (key) {
                            is Route.About -> AboutPage()
                            is Route.Basic -> BasicPage()
                            is Route.Scope -> ScopePage()
                            is Route.Icon -> IconPage()
                            is Route.Bottom -> BottomPage()
                            is Route.Background -> BackgroundPage()
                            is Route.Display -> DisplayPage()
                            is Route.Developer -> DevPage()
                            is Route.CustomScope -> CustomScopePage()
                            is Route.IgnoreAppIcon -> IgnoreAppIconPage()
                            is Route.HideIcon -> HideIconPage()
                            is Route.RemoveBranding -> RemoveBrandingPage()
                            is Route.BackgroundExcept -> BackgroundExceptPage()
                            is Route.BgIndividual -> BgIndividualPage()
                            is Route.MinDuration -> MinDurationPage()
                            is Route.ForceSplash -> ForceSplashPage()
                            is Route.ColorPicker -> ColorPickerPage(key.pkgName)
                            else -> {}
                        }
                    }
                }
            )
        }
    }
}
