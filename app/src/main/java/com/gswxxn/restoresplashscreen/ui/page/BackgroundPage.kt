package com.gswxxn.restoresplashscreen.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.Route
import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.ui.component.DropDownPreference
import com.gswxxn.restoresplashscreen.ui.component.HeaderCard
import com.gswxxn.restoresplashscreen.ui.component.SwitchPreference
import com.gswxxn.restoresplashscreen.ui.component.TextPreference
import com.gswxxn.restoresplashscreen.ui.page.data.BGColorModes
import com.gswxxn.restoresplashscreen.ui.page.data.ChangeBGColorTypes
import com.gswxxn.restoresplashscreen.utils.RemotePreferenceStore
import com.gswxxn.restoresplashscreen.utils.DeviceUtils.isHyperOS
import dev.lackluster.hyperx.navigation.LocalNavigator
import org.koin.compose.koinInject
import dev.lackluster.hyperx.navigation.Navigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.DropDownEntry
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.PreferenceGroup
import com.highcapable.kavaref.extension.toClassOrNull

/**
 * 背景 界面
 */
@Composable
fun BackgroundPage() {
    val navigator = LocalNavigator.current
    HyperXPage(
        title = stringResource(R.string.background_settings),
    ) {
        item {
            HeaderCard(imageResID = R.drawable.demo_background, title = "BACKGROUND")

            SettingItems(navigator)
        }
    }
}

/**
 * 分组设置
 */
@Composable
private fun SettingItems(navigator: Navigator) {
    val store = koinInject<RemotePreferenceStore>()
    val ignoreDarkMode = remember { mutableStateOf(store.get(Preferences.Background.IGNORE_DARK_MODE)) }

    PreferenceGroup(position = if (isHyperOS) ItemPosition.Middle else ItemPosition.Last) {
        GeneralSettingItems(navigator = navigator, ignoreDarkMode = ignoreDarkMode)
    }

    if (isHyperOS) {
        PreferenceGroup(position = ItemPosition.Last) {
            MIUISettingsGroup(ignoreDarkMode = ignoreDarkMode)
        }
    }
}

/**
 * 替换背景颜色的通用设置
 */
@Composable
private fun GeneralSettingItems(
    navigator: Navigator,
    ignoreDarkMode: MutableState<Boolean>
) {
    val store = koinInject<RemotePreferenceStore>()

    val colorMode = remember { mutableIntStateOf(store.get(Preferences.Background.BG_COLOR_MODE)) }
    val changeBGColorType = remember { mutableIntStateOf(store.get(Preferences.Background.CHANG_BG_COLOR_TYPE)) }

    val shouldShowColorMode = changeBGColorType.intValue == ChangeBGColorTypes.FromIcon.ordinal ||
            changeBGColorType.intValue == ChangeBGColorTypes.FromMonet.ordinal

    // 替换背景颜色
    DropDownPreference(
        title = stringResource(R.string.change_bg_color),
        entries = ChangeBGColorTypes.entries.mapIndexed { index, type -> DropDownEntry(value = index, title = stringResource(type.stringID)) },
        key = Preferences.Background.CHANG_BG_COLOR_TYPE,
        onSelectedIndexChange = { changeBGColorType.intValue = it }
    )

    AnimatedVisibility(
        visible = shouldShowColorMode,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // 颜色模式
        DropDownPreference(
            title = stringResource(R.string.color_mode),
            summary = if (isHyperOS) stringResource(R.string.color_mode_tips) else null,
            entries = BGColorModes.entries.mapIndexed { index, mode -> DropDownEntry(value = index, title = stringResource(mode.stringID)) },
            key = Preferences.Background.BG_COLOR_MODE,
            selectedIndex = colorMode
        ) {
            if (isHyperOS && colorMode.intValue == BGColorModes.FollowSystem.ordinal) {
                store.put(Preferences.Background.IGNORE_DARK_MODE, true)
                ignoreDarkMode.value = true
            }
        }
    }
    AnimatedVisibility(
        visible = changeBGColorType.intValue == ChangeBGColorTypes.FromCustom.ordinal,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // 自定义背景颜色
        TextPreference(
            title = stringResource(R.string.set_custom_bg_color)
        ) {
            navigator.push(Route.ColorPicker(""))
        }
    }
    AnimatedVisibility(
        visible = changeBGColorType.intValue != ChangeBGColorTypes.NotChangeBGColor.ordinal,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column {
            // 跳过已主动设置背景颜色的应用
            SwitchPreference(
                title = stringResource(R.string.skip_app_with_bg_color),
                key = Preferences.Background.SKIP_APP_WITH_BG_COLOR
            )
            // 配置应用列表
            TextPreference(title = stringResource(R.string.change_bg_color_list)) {
                navigator.push(Route.BackgroundExcept)
            }
        }
    }
    // 单独配置应用背景颜色
    TextPreference(title = stringResource(R.string.configure_bg_colors_individually)) {
        navigator.push(Route.BgIndividual)
    }
}

/**
 * 仅在小米设备上生效的设置项
 */
@Composable
private fun MIUISettingsGroup(ignoreDarkMode: MutableState<Boolean>) {
    // 忽略深色模式
    SwitchPreference(
        title = stringResource(R.string.ignore_dark_mode),
        summary = stringResource(R.string.ignore_dark_mode_tips),
        key = Preferences.Background.IGNORE_DARK_MODE,
        checked = ignoreDarkMode
    )

    if ("android.app.TaskSnapshotHelperImpl".toClassOrNull() != null) {
        // 移除截图背景
        SwitchPreference(
            title = stringResource(R.string.remove_bg_drawable),
            summary = stringResource(R.string.remove_bg_drawable_tips),
            key = Preferences.Background.REMOVE_BG_DRAWABLE
        )
    }
}
