package com.gswxxn.restoresplashscreen.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.hook.base.HookManager
import com.gswxxn.restoresplashscreen.ui.MainActivity
import com.gswxxn.restoresplashscreen.ui.component.SwitchPreference
import com.gswxxn.restoresplashscreen.utils.YukiHelper.getHookInfo
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.navigation.Navigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.EditTextInputType
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.PreferenceGroup
import dev.lackluster.hyperx.ui.preference.SeekBarPreference
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    PreferenceGroup(title = stringResource(R.string.icon_settings)) { IconSettingItems() }
    PreferenceGroup(title = stringResource(R.string.hook_info), position = ItemPosition.Last) { HookInfo() }
}

/**
 * 通用设置
 */
@Composable
private fun GeneralSettingItems(navigator: Navigator) {
    SwitchPreference(
        title = stringResource(R.string.dev_settings),
        prefsData = DataConst.ENABLE_DEV_SETTINGS,
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
    val prefs = LocalContext.current.prefs()
    val roundCornerRate = remember {
        mutableFloatStateOf(prefs.get(DataConst.DEV_ICON_ROUND_CORNER_RATE).toFloat())
    }
    SeekBarPreference(
        title = stringResource(R.string.dev_icon_round_corner_rate),
        value = roundCornerRate.floatValue,
        onValueChange = { roundCornerRate.floatValue = it },
        onValueChangeFinished = {
            prefs.edit { put(DataConst.DEV_ICON_ROUND_CORNER_RATE, roundCornerRate.floatValue.roundToInt()) }
        },
        defaultValue = DataConst.DEV_ICON_ROUND_CORNER_RATE.value.toFloat(),
        min = 0f,
        max = 50f,
        dialogInputType = EditTextInputType.Number,
        valueFormatter = { "%d%% / 50%%".format(it.roundToInt()) }
    )
}

/**
 * 显示 Hook 信息
 */
@Composable
private fun HookInfo() {
    val context = LocalContext.current
    val hookInfos = remember { mutableStateListOf<Map.Entry<String, HookManager.HookInfo>>() }

    LaunchedEffect(Unit) {
        hookInfos.clear()
        context.getHookInfo("com.android.systemui") { newHookInfos ->
            hookInfos.addAll(newHookInfos.entries.sortedWith(compareBy({ !it.value.isAbnormal }, { it.key })))
        }
    }

    if (hookInfos.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dev_hook_info_empty),
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface
            )
        }
    } else hookInfos.forEach { (key, hookInfo) ->
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = key,
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = if (hookInfo.isAbnormal) Color.Red else MiuixTheme.colorScheme.onSurface
            )
            Text(
                text = "createCondition: ${hookInfo.createCondition}\n" +
                        "isMemberFound: ${hookInfo.isMemberFound}\n" +
                        "hasBeforeHooks: ${hookInfo.hasBeforeHooks}\n" +
                        "isBeforeHookExecuted: ${hookInfo.isBeforeHookExecuted}\n" +
                        "hasAfterHooks: ${hookInfo.hasAfterHooks}\n" +
                        "isAfterHookExecuted: ${hookInfo.isAfterHookExecuted}\n" +
                        "hasReplaceHook: ${hookInfo.hasReplaceHook}\n" +
                        "isReplaceHookExecuted: ${hookInfo.isReplaceHookExecuted}",
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}
