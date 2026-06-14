package com.gswxxn.restoresplashscreen.ui.page

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.gswxxn.restoresplashscreen.BuildConfig
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.Pages
import com.gswxxn.restoresplashscreen.data.Route
import com.gswxxn.restoresplashscreen.ui.MainActivity
import com.gswxxn.restoresplashscreen.ui.MainActivity.Companion.androidRestartNeeded
import com.gswxxn.restoresplashscreen.ui.MainActivity.Companion.moduleActive
import com.gswxxn.restoresplashscreen.ui.MainActivity.Companion.systemUIRestartNeeded
import com.gswxxn.restoresplashscreen.ui.component.TextPreference
import com.gswxxn.restoresplashscreen.ui.page.data.ModulePreferenceRes
import com.gswxxn.restoresplashscreen.ui.page.data.ModuleStatusType
import com.gswxxn.restoresplashscreen.utils.CommonUtils.execShell
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toast
import com.highcapable.yukihookapi.YukiHookAPI.Status.Executor
import dev.lackluster.hyperx.navigation.HyperXRoute
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.navigation.Navigator
import dev.lackluster.hyperx.ui.component.ImageIcon
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.PreferenceGroup
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 主界面 Page
 */
@Composable
fun MainPage() {
    val navigator = LocalNavigator.current
    val dialogRestartVisibility = remember { mutableStateOf(false) }

    HyperXPage(
        title = stringResource(R.string.app_name),
        navigationIcon = { },
        actions = { PopUpMenu(navigator, dialogRestartVisibility) }
    ) {
        item {
            TopCard()

            SettingItems(navigator)

            Text(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                text = stringResource(R.string.main_activity_hint),
                fontSize = MiuixTheme.textStyles.subtitle.fontSize,
                color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }

    RestartDialog(dialogRestartVisibility)
}

/**
 * 首页顶部状态卡片
 */
@Composable
private fun TopCard() {
    val moduleStatusTypeRes = getModuleStatusType(
        moduleActive = moduleActive.value,
        androidRestartNeeded = androidRestartNeeded.value,
        systemUIRestartNeeded = systemUIRestartNeeded.value
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp, top = 12.dp),
        colors = CardDefaults.defaultColors(colorResource(moduleStatusTypeRes.cardBackground)),
        pressFeedbackType = PressFeedbackType.Sink,
        onLongPress = {
            // todo: 目前 execShell 并没有能力判断命令执行成功与否, 在未获取到 root 时, 卡片仍为可点击状态但没有任何提示
            if (moduleStatusTypeRes != ModuleStatusType.INACTIVE) {
                execShell("am broadcast -a android.telephony.action.SECRET_CODE -d android_secret_code://5776733 android")
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .padding(16.dp)
                    .size(28.dp),
                painter = painterResource(moduleStatusTypeRes.stateIconRes),
                colorFilter = ColorFilter.tint(color = MiuixTheme.colorScheme.onBackground),
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(moduleStatusTypeRes.stateTextRes),
                    fontSize = MiuixTheme.textStyles.title3.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackground
                )
                Text(
                    modifier = Modifier.padding(vertical = 2.dp),
                    text = stringResource(R.string.module_version, BuildConfig.VERSION_NAME),
                    fontSize = MiuixTheme.textStyles.body1.fontSize,
                    color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                )
                Crossfade(moduleActive.value, label = "moduleActive") { isActive ->
                    if (isActive) {
                        Text(
                            modifier = Modifier.padding(bottom = 16.dp),
                            text = stringResource(
                                R.string.xposed_framework_version,
                                Executor.name,
                                Executor.apiLevel
                            ),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * 全部设置项
 */
@Composable
private fun SettingItems(
    navigator: Navigator
) {
    PreferenceGroup {
        ModuleSettingPreference(ModulePreferenceRes.BasicSettings, navigator)
    }

    PreferenceGroup {
        ModuleSettingPreference(ModulePreferenceRes.CustomScopeSettings, navigator)
        ModuleSettingPreference(ModulePreferenceRes.IconSettings, navigator)
        ModuleSettingPreference(ModulePreferenceRes.BottomSettings, navigator)
        ModuleSettingPreference(ModulePreferenceRes.BackgroundSettings, navigator)
        ModuleSettingPreference(ModulePreferenceRes.DisplaySettings, navigator)
        if (MainActivity.devMode.value) {
            ModuleSettingPreference(ModulePreferenceRes.DevSettings, navigator)
        }
    }

    PreferenceGroup {
        val context = LocalContext.current
        ModuleSettingPreference(ModulePreferenceRes.FAQ) {
            with(context) {
                startActivity(
                    Intent(Intent.ACTION_VIEW, getString(R.string.faq_url).toUri())
                )
            }
        }
    }
}

/**
 * 右上角的弹出菜单
 */
@Composable
private fun PopUpMenu(
    navigator: Navigator,
    dialogRestartVisibility: MutableState<Boolean>
) {
    val hapticFeedback = LocalHapticFeedback.current
    val showTopPopup = remember { mutableStateOf(false) }
    // 菜单内容
    val contextMenuItems = listOf(
        stringResource(R.string.restart),
        stringResource(R.string.about)
    )

    // 弹出菜单
    OverlayListPopup(
        show = showTopPopup.value,
        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
        alignment = PopupPositionProvider.Align.TopEnd,
        onDismissRequest = {
            showTopPopup.value = false
        }
    ) {
        ListPopupColumn {
            contextMenuItems.forEachIndexed { index, string ->
                DropdownImpl(
                    text = string,
                    optionSize = contextMenuItems.size,
                    isSelected = false,
                    onSelectedIndexChange = {
                        when (it) {
                            0 -> {
                                dialogRestartVisibility.value = true
                            }

                            1 -> {
                                navigator.popUntil { route -> route is HyperXRoute.Main }
                                navigator.push(Route.About)
                            }
                        }
                        showTopPopup.value = false
                    },
                    index = index
                )
            }
        }
    }

    // 未弹出时的按钮
    IconButton(
        modifier = Modifier
            .padding(end = 21.dp)
            .size(40.dp),
        onClick = {
            showTopPopup.value = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        holdDownState = showTopPopup.value
    ) {
        Icon(
            imageVector = MiuixIcons.MoreCircle,
            contentDescription = "Menu"
        )
    }
}

/**
 * 重启提示的 Dialog
 */
@Composable
private fun RestartDialog(
    show: MutableState<Boolean>
) {
    val context = LocalContext.current
    OverlayDialog(
        title = stringResource(R.string.restart_title),
        summary = stringResource(R.string.restart_message),
        show = show.value,
        onDismissRequest = { show.value = false },
    ) {
        Column {
            // 重启手机 按钮
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.reboot),
                onClick = {
                    execShell("reboot")
                    Thread.sleep(300)
                    context.toast(R.string.no_root)
                }
            )
            Spacer(Modifier.height(12.dp))

            // 重启系统界面 按钮
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.restart_system_ui),
                onClick = {
                    execShell("pkill -f com.android.systemui && pkill -f com.gswxxn.restoresplashscreen")
                    Thread.sleep(300)
                    context.toast(R.string.no_root)
                }
            )
            Spacer(Modifier.height(12.dp))

            // 取消 按钮
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                text = stringResource(R.string.button_cancel),
                onClick = {
                    show.value = false
                }
            )
        }
    }
}

/**
 * 根据 [ModulePreferenceRes] 提供的资源来创建模块首页设置项
 *
 * @param modulePreferenceRes 模块偏好配置资源，包含图标资源、标题资源和导航目标
 * @param navigator 导航控制器，用于执行页面跳转。如果为 null，则不会触发导航
 * @param onClick 点击事件的回调函数。如果不为 null，则会在点击时执行此回调
 */
@Composable
fun ModuleSettingPreference(
    modulePreferenceRes: ModulePreferenceRes,
    navigator: Navigator? = null,
    onClick: (() -> Unit)? = null
) {
    TextPreference(
        icon = ImageIcon(resId = modulePreferenceRes.iconRes),
        title = stringResource(modulePreferenceRes.stringRes),
        ignoreModuleActiveStatus = true
    ) {
        onClick?.invoke()
        modulePreferenceRes.navigateTo?.toRoute()?.let { route ->
            navigator?.popUntil { it is HyperXRoute.Main }
            navigator?.push(route)
        }
    }
}

/**
 * 将旧的页面常量（[Pages]）映射为新的导航路由（[Route]）
 */
private fun String.toRoute(): Route? = when (this) {
    Pages.ABOUT -> Route.About
    Pages.BASIC_SETTINGS -> Route.Basic
    Pages.SCOPE_SETTINGS -> Route.Scope
    Pages.ICON_SETTINGS -> Route.Icon
    Pages.BOTTOM_SETTINGS -> Route.Bottom
    Pages.BACKGROUND_SETTINGS -> Route.Background
    Pages.DISPLAY_SETTINGS -> Route.Display
    Pages.DEVELOPER_SETTINGS -> Route.Developer
    Pages.CONFIG_CUSTOM_SCOPE -> Route.CustomScope
    Pages.CONFIG_IGNORE_APP_ICON -> Route.IgnoreAppIcon
    Pages.CONFIG_HIDE_SPLASH_ICON -> Route.HideIcon
    Pages.CONFIG_REMOVE_BRANDING -> Route.RemoveBranding
    Pages.CONFIG_BACKGROUND_EXCEPT -> Route.BackgroundExcept
    Pages.CONFIG_BACKGROUND_INDIVIDUALLY -> Route.BgIndividual
    Pages.CONFIG_MIN_DURATION -> Route.MinDuration
    Pages.CONFIG_FORCE_SHOW_SPLASH -> Route.ForceSplash
    else -> null
}

/**
 * 根据提供的条件确定模块现实的激活状态
 *
 * @param moduleActive 一个布尔值，表示模块是否处于激活状态
 * @param androidRestartNeeded 一个可空的布尔值，表示是否需要 Android 重启, 空为未获取到数据
 * @param systemUIRestartNeeded 一个可空的布尔值，表示是否需要系统 UI 重启, 空为未获取到数据
 * @return 返回表示当前模块状态的 [ModuleStatusType]
 */
private fun getModuleStatusType(
    moduleActive: Boolean,
    androidRestartNeeded: Boolean?,
    systemUIRestartNeeded: Boolean?
): ModuleStatusType = when {
    moduleActive && androidRestartNeeded == true -> ModuleStatusType.ACTIVE_ANDROID_RESTART
    moduleActive && systemUIRestartNeeded == true -> ModuleStatusType.ACTIVE_SYSTEM_UI_RESTART
    moduleActive -> ModuleStatusType.ACTIVE_NO_NEED_RESTART
    else -> ModuleStatusType.INACTIVE
}
