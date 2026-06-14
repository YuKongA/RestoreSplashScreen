package com.gswxxn.restoresplashscreen.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.data.Route
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
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.dataChannel
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.lackluster.hyperx.core.HyperXActivity
import dev.lackluster.hyperx.core.SafeSP
import dev.lackluster.hyperx.ui.layout.HyperXAppLayout
import dev.lackluster.hyperx.ui.layout.HyperXLayoutConfig
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : HyperXActivity() {
    companion object {
        val moduleActive: MutableState<Boolean> = mutableStateOf(false)
        val devMode: MutableState<Boolean> = mutableStateOf(false)
        val blurEnabled: MutableState<Boolean> = mutableStateOf(true)
        val splitEnabled: MutableState<Boolean> = mutableStateOf(true)

        val systemUIRestartNeeded = mutableStateOf(true)
        val androidRestartNeeded = mutableStateOf<Boolean?>(null)
    }

    @SuppressLint("WorldReadableFiles")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            SafeSP.setSP(getSharedPreferences("${packageName ?: "unknown"}_preferences", MODE_WORLD_READABLE))
        } finally {
        }

        devMode.value = prefs().get(DataConst.ENABLE_DEV_SETTINGS)
        blurEnabled.value = prefs().get(DataConst.MODULE_APP_BLUR)
        splitEnabled.value = prefs().get(DataConst.SPLIT_VIEW)

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        moduleActive.value = YukiHookAPI.Status.isXposedModuleActive
        dataChannel("com.android.systemui").checkingVersionEquals {
            systemUIRestartNeeded.value = !it
        }
        dataChannel("android").checkingVersionEquals {
            androidRestartNeeded.value = !it
        }
    }

    @Composable
    override fun AppContent() {
        HyperXAppLayout(
            config = HyperXLayoutConfig(
                isSplitScreenEnabled = splitEnabled.value,
                isBlurEnabled = blurEnabled.value
            ),
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
