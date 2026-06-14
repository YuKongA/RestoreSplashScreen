package com.gswxxn.restoresplashscreen.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.data.Route
import com.gswxxn.restoresplashscreen.ui.component.HeaderCard
import com.gswxxn.restoresplashscreen.ui.component.SwitchPreference
import com.gswxxn.restoresplashscreen.ui.component.TextPreference
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toast
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.navigation.Navigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.PreferenceGroup

/**
 * 显示设置 界面
 */
@Composable
fun DisplayPage() {
    val navigator = LocalNavigator.current
    HyperXPage(
        title = stringResource(R.string.display_settings),
    ) {
        item {
            HeaderCard(imageResID = R.drawable.demo_display, title = "DISPLAY")

            SettingItems(navigator)
        }
    }
}

/**
 * 分组设置
 */
@Composable
private fun SettingItems(navigator: Navigator) {
    PreferenceGroup {
        // 遮罩最小持续时间
        TextPreference(
            title = stringResource(R.string.min_duration),
            summary = stringResource(R.string.min_duration_tips),
            onClick = { navigator.push(Route.MinDuration) }
        )
    }
    PreferenceGroup {
        ForceShowSplashScreenSettingsGroup(navigator)
    }
    PreferenceGroup(position = ItemPosition.Last) {
        OtherDisplaySettingsGroup()
    }
}

/**
 * 强制显示遮罩
 */
@Composable
private fun ForceShowSplashScreenSettingsGroup(navigator: Navigator) {
    val context = LocalContext.current
    val prefs = context.prefs()
    val forceShowSplash = remember { mutableStateOf(prefs.get(DataConst.FORCE_SHOW_SPLASH_SCREEN)) }
    // 强制显示遮罩
    SwitchPreference(
        title = stringResource(R.string.force_show_splash_screen),
        summary = stringResource(R.string.force_show_splash_screen_tips),
        prefsData = DataConst.FORCE_SHOW_SPLASH_SCREEN,
        checked = forceShowSplash
    ) { newValue ->
        if (newValue) {
            context.toast(R.string.custom_scope_message)
        }
    }
    AnimatedVisibility(
        visible = forceShowSplash.value,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column {
            // 配置应用列表
            TextPreference(title = stringResource(R.string.force_show_splash_screen_list)) {
                navigator.push(Route.ForceSplash)
            }
            // 减少不必要的启动遮罩
            SwitchPreference(
                title = stringResource(R.string.reduce_splash_screen),
                summary = stringResource(R.string.reduce_splash_screen_tips),
                prefsData = DataConst.REDUCE_SPLASH_SCREEN
            )
        }
    }
}

/**
 * 其他显示设置组
 */
@Composable
private fun OtherDisplaySettingsGroup() {
    val prefs = LocalContext.current.prefs()

    val forceDisableSplash = remember { mutableStateOf(prefs.get(DataConst.DISABLE_SPLASH_SCREEN)) }
    val forceEnableSplash = remember { mutableStateOf(prefs.get(DataConst.FORCE_ENABLE_SPLASH_SCREEN)) }

    // 互斥设置
    AnimatedVisibility(
        visible = !forceDisableSplash.value,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column {
            // 强制开启启动遮罩
            SwitchPreference(
                title = stringResource(R.string.force_enable_splash_screen),
                summary = stringResource(R.string.force_enable_splash_screen_tips),
                prefsData = DataConst.FORCE_ENABLE_SPLASH_SCREEN,
                checked = forceEnableSplash
            )
            // 将启动遮罩适用于热启动
            AnimatedVisibility(
                visible = forceEnableSplash.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SwitchPreference(
                    title = stringResource(R.string.hot_start_compatible),
                    summary = stringResource(R.string.hot_start_compatible_tips),
                    prefsData = DataConst.ENABLE_HOT_START_COMPATIBLE,
                    enabled = forceEnableSplash.value
                )
            }
        }
    }
    // 彻底关闭 Splash Screen
    SwitchPreference(
        title = stringResource(R.string.disable_splash_screen),
        summary = stringResource(R.string.disable_splash_screen_tips),
        prefsData = DataConst.DISABLE_SPLASH_SCREEN,
        checked = forceDisableSplash
    )
}
