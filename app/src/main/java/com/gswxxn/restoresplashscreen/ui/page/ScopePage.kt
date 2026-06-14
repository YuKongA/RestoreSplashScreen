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
 * 作用域 界面
 */
@Composable
fun ScopePage() {
    val navigator = LocalNavigator.current
    HyperXPage(
        title = stringResource(R.string.custom_scope_settings),
    ) {
        item {
            HeaderCard(imageResID = R.drawable.demo_scope, title = "SCOPE")

            PreferenceGroup(position = ItemPosition.Last) {
                SettingItems(navigator)
            }
        }
    }
}

/**
 * 作用阈设置项
 */
@Composable
private fun SettingItems(navigator: Navigator) {
    val context = LocalContext.current
    val prefs = context.prefs()

    val customScope = remember { mutableStateOf(prefs.get(DataConst.ENABLE_CUSTOM_SCOPE)) }

    // 自定义模块作用域
    SwitchPreference(
        title = stringResource(R.string.custom_scope),
        prefsData = DataConst.ENABLE_CUSTOM_SCOPE,
        checked = customScope
    ) { newValue ->
        if (newValue) {
            context.toast(R.string.custom_scope_message)
        }
    }
    AnimatedVisibility(
        visible = customScope.value,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column {
            // 将作用域外的应用替换位空白启动遮罩
            SwitchPreference(
                title = stringResource(R.string.replace_to_empty_splash_screen),
                summary = stringResource(R.string.replace_to_empty_splash_screen_tips),
                prefsData = DataConst.REPLACE_TO_EMPTY_SPLASH_SCREEN
            )
            // 配置应用列表
            TextPreference(title = stringResource(R.string.exception_mode_list)) {
                navigator.push(Route.CustomScope)
            }
        }
    }
}
