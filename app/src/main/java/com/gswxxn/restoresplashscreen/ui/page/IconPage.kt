package com.gswxxn.restoresplashscreen.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.data.Route
import com.gswxxn.restoresplashscreen.ui.component.DropDownPreference
import com.gswxxn.restoresplashscreen.ui.component.HeaderCard
import com.gswxxn.restoresplashscreen.ui.component.SwitchPreference
import com.gswxxn.restoresplashscreen.ui.component.TextPreference
import com.gswxxn.restoresplashscreen.ui.page.data.ShrinkIconType
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toast
import com.gswxxn.restoresplashscreen.utils.IconPackManager
import com.gswxxn.restoresplashscreen.utils.YukiHelper
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.navigation.Navigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.DropDownEntry
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.PreferenceGroup

/**
 * 图标 界面
 */
@Composable
fun IconPage() {
    val navigator = LocalNavigator.current
    HyperXPage(
        title = stringResource(R.string.icon_settings),
    ) {
        item {
            HeaderCard(imageResID = R.drawable.demo_icon, title = "ICON")

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
        CommonSettingsGroup()
    }
    PreferenceGroup {
        DefaultIconSettingsGroup(navigator)
    }
    PreferenceGroup(position = ItemPosition.Last) {
        HideSplashIconSettingsGroup(navigator)
    }
}

/**
 * 通用设置
 */
@Composable
private fun CommonSettingsGroup() {
    val context = LocalContext.current
    val prefs = context.prefs()

    // 图标包列表预处理
    val selectedIconPackIndex = remember { mutableIntStateOf(0) }
    val availableIconPackItems = remember {
        mutableStateListOf(
            DropDownEntry(value = 0, title = "None", summary = "None")
        )
    }
    LaunchedEffect(Unit) {
        availableIconPackItems.addAll(
            IconPackManager(context).getAvailableIconPacks()
                .filter { it.key != "None" }
                .map { (packageName, iconPackName) ->
                    DropDownEntry(value = availableIconPackItems.size, title = iconPackName, summary = packageName)
                }
        )
        selectedIconPackIndex.intValue = availableIconPackItems.indexOfFirst {
            it.summary == prefs.get(DataConst.ICON_PACK_PACKAGE_NAME)
        }.takeIf { it != -1 } ?: run {
            prefs.edit { put(DataConst.ICON_PACK_PACKAGE_NAME, "None") }
            context.toast(R.string.icon_pack_is_removed)
            0
        }
    }
    // 绘制图标圆角
    SwitchPreference(
        title = stringResource(R.string.draw_round_corner),
        prefsData = DataConst.ENABLE_DRAW_ROUND_CORNER
    )
    // 缩小图标
    var shrinkIcon by remember { mutableIntStateOf(prefs.get(DataConst.SHRINK_ICON)) }
    DropDownPreference(
        title = stringResource(R.string.shrink_icon),
        entries = ShrinkIconType.entries.mapIndexed { index, type -> DropDownEntry(value = index, title = stringResource(type.stringID)) },
        prefsData = DataConst.SHRINK_ICON,
        onSelectedIndexChange = { shrinkIcon = it }
    )
    AnimatedVisibility(
        visible = shrinkIcon != ShrinkIconType.NotShrinkIcon.ordinal,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // 为缩小的图标添加模糊背景
        SwitchPreference(
            title = stringResource(R.string.add_icon_blur_bg),
            prefsData = DataConst.ENABLE_ADD_ICON_BLUR_BG
        )
    }
    // 替换图标获取方式
    SwitchPreference(
        title = stringResource(R.string.replace_icon),
        summary = stringResource(R.string.replace_icon_tips),
        prefsData = DataConst.ENABLE_REPLACE_ICON
    )

    // 使用 MIUI 大图标
    if (YukiHelper.atLeastMIUI14) {
        SwitchPreference(
            title = stringResource(R.string.use_miui_large_icon),
            prefsData = DataConst.ENABLE_USE_MIUI_LARGE_ICON
        )
    }

    // 使用图标包
    DropDownPreference(
        title = stringResource(R.string.use_icon_pack),
        entries = availableIconPackItems,
        selectedIndex = selectedIconPackIndex
    ) {
        prefs.edit {
            put(DataConst.ICON_PACK_PACKAGE_NAME, availableIconPackItems[it].summary ?: "None")
        }
    }
}

/**
 * 忽略应用主动设置的图标
 */
@Composable
private fun DefaultIconSettingsGroup(navigator: Navigator) {
    val context = LocalContext.current
    val prefs = context.prefs()

    // 忽略应用主动设置的图标
    val ignoreAppIcon = remember { mutableStateOf(prefs.get(DataConst.ENABLE_DEFAULT_STYLE)) }
    SwitchPreference(
        title = stringResource(R.string.default_style),
        summary = stringResource(R.string.default_style_tips),
        prefsData = DataConst.ENABLE_DEFAULT_STYLE,
        checked = ignoreAppIcon
    ) { newValue ->
        if (newValue) {
            context.toast(R.string.custom_scope_message)
        }
    }
    AnimatedVisibility(
        visible = ignoreAppIcon.value,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // 配置应用列表
        TextPreference(title = stringResource(R.string.default_style_list)) {
            navigator.push(Route.IgnoreAppIcon)
        }
    }
}

/**
 * 不显示图标
 */
@Composable
private fun HideSplashIconSettingsGroup(navigator: Navigator) {
    val context = LocalContext.current
    val prefs = context.prefs()

    val hideSplashIcon = remember { mutableStateOf(prefs.get(DataConst.ENABLE_HIDE_SPLASH_SCREEN_ICON)) }
    // 不显示图标
    SwitchPreference(
        title = stringResource(R.string.hide_splash_screen_icon),
        prefsData = DataConst.ENABLE_HIDE_SPLASH_SCREEN_ICON,
        checked = hideSplashIcon
    ) { newValue ->
        if (newValue) {
            context.toast(R.string.custom_scope_message)
        }
    }
    AnimatedVisibility(
        visible = hideSplashIcon.value,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // 配置应用列表
        TextPreference(
            title = stringResource(R.string.default_style_list),
            onClick = { navigator.push(Route.HideIcon) }
        )
    }
}
