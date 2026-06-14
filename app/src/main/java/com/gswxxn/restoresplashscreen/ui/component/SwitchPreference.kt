package com.gswxxn.restoresplashscreen.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.ui.MainActivity
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toast
import dev.lackluster.hyperx.ui.component.ImageIcon
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey
import dev.lackluster.hyperx.ui.preference.core.rememberPreferenceState
import dev.lackluster.hyperx.ui.preference.SwitchPreference as HyperXSwitchPreference

/**
 * 开关可组合函数, 通过 [rememberPreferenceState] 管理远程 SharedPreferences, 并在模块未激活时提示用户
 */
@Composable
fun SwitchPreference(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    key: PreferenceKey<Boolean>? = null,
    enabled: Boolean = true,
    checked: MutableState<Boolean>? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val boundState = key?.let { rememberPreferenceState(it) }
    val currentChecked = checked
        ?: boundState
        ?: remember { mutableStateOf(false) }

    HyperXSwitchPreference(
        title = title,
        summary = summary,
        icon = icon,
        checked = currentChecked.value,
        enabled = enabled,
        onCheckedChange = { newValue ->
            if (!MainActivity.moduleActive.value) {
                context.toast(R.string.make_sure_active)
            } else {
                // 持久化（boundState 与 currentChecked 为同一对象时不重复写入）
                if (boundState != null && boundState !== currentChecked) {
                    boundState.value = newValue
                }
                currentChecked.value = newValue
                onCheckedChange?.invoke(newValue)
            }
        },
    )
}
