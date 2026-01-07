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
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.data.Pages
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
import dev.lackluster.hyperx.compose.activity.HyperXActivity
import dev.lackluster.hyperx.compose.activity.SafeSP
import dev.lackluster.hyperx.compose.base.HyperXApp
import dev.lackluster.hyperx.compose.navigation.miuixComposable
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
        HyperXApp(
            autoSplitView = splitEnabled,
            mainPageContent = { navController, adjustPadding, mode ->
                MainPage(navController, adjustPadding, mode)
            },
            emptyPageContent = {
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
            otherPageBuilder = { navController, adjustPadding, mode ->
                miuixComposable(Pages.ABOUT) { AboutPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.BASIC_SETTINGS) { BasicPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.SCOPE_SETTINGS) { ScopePage(navController, adjustPadding, mode) }
                miuixComposable(Pages.ICON_SETTINGS) { IconPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.BOTTOM_SETTINGS) { BottomPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.BACKGROUND_SETTINGS) { BackgroundPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.DISPLAY_SETTINGS) { DisplayPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.DEVELOPER_SETTINGS) { DevPage(navController, adjustPadding, mode) }

                miuixComposable(Pages.CONFIG_CUSTOM_SCOPE) { CustomScopePage(navController, adjustPadding, mode) }
                miuixComposable(Pages.CONFIG_IGNORE_APP_ICON) { IgnoreAppIconPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.CONFIG_HIDE_SPLASH_ICON) { HideIconPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.CONFIG_REMOVE_BRANDING) { RemoveBrandingPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.CONFIG_BACKGROUND_EXCEPT) { BackgroundExceptPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.CONFIG_BACKGROUND_INDIVIDUALLY) { BgIndividualPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.CONFIG_MIN_DURATION) { MinDurationPage(navController, adjustPadding, mode) }
                miuixComposable(Pages.CONFIG_FORCE_SHOW_SPLASH) { ForceSplashPage(navController, adjustPadding, mode) }

                miuixComposable("${Pages.CONFIG_COLOR_PICKER}?PkgName={PkgName}") {
                    val pkgName = it.arguments?.getString("PkgName") ?: ""
                    /* Todo: 传入 是否为 OVERALL_BG 设置, 不应为现在的依据包名为 “” 来判断*/
                    ColorPickerPage(navController, adjustPadding, pkgName, mode)
                }
            }
        )
    }
}