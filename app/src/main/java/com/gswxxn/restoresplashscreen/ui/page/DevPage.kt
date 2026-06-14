package com.gswxxn.restoresplashscreen.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.ui.MainActivity
import com.gswxxn.restoresplashscreen.ui.component.SwitchPreference
import com.gswxxn.restoresplashscreen.utils.RemotePreferenceStore
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.navigation.Navigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.EditTextInputType
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.PreferenceGroup
import dev.lackluster.hyperx.ui.preference.SeekBarPreference
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * 开发者选项
 */
@Composable
fun DevPage() {
    val navigator = LocalNavigator.current
    HyperXPage(
        title = stringResource(R.string.dev_settings),
    ) {
        item {
            SettingItems(navigator)
        }
    }
}

/**
 * 分组设置
 */
@Composable
private fun SettingItems(navigator: Navigator) {
    PreferenceGroup(position = ItemPosition.First) { GeneralSettingItems(navigator = navigator) }
    PreferenceGroup(title = stringResource(R.string.icon_settings), position = ItemPosition.Last) { IconSettingItems() }
}

/**
 * 通用设置
 */
@Composable
private fun GeneralSettingItems(navigator: Navigator) {
    SwitchPreference(
        title = stringResource(R.string.dev_settings),
        key = Preferences.Dev.ENABLE_DEV_SETTINGS,
        onCheckedChange = {
            MainActivity.devMode.value = it
            navigator.pop()
        }
    )
}

/**
 * 图标设置
 */
@Composable
private fun IconSettingItems() {
    val store = koinInject<RemotePreferenceStore>()
    val roundCornerRate = remember {
        mutableFloatStateOf(store.get(Preferences.Dev.DEV_ICON_ROUND_CORNER_RATE).toFloat())
    }
    SeekBarPreference(
        title = stringResource(R.string.dev_icon_round_corner_rate),
        value = roundCornerRate.floatValue,
        onValueChange = { roundCornerRate.floatValue = it },
        onValueChangeFinished = {
            store.put(Preferences.Dev.DEV_ICON_ROUND_CORNER_RATE, roundCornerRate.floatValue.roundToInt())
        },
        defaultValue = Preferences.Dev.DEV_ICON_ROUND_CORNER_RATE.default.toFloat(),
        min = 0f,
        max = 50f,
        dialogInputType = EditTextInputType.Number,
        valueFormatter = { "%d%% / 50%%".format(it.roundToInt()) }
    )
}
