package com.gswxxn.restoresplashscreen.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toast
import com.highcapable.yukihookapi.YukiHookAPI
import dev.lackluster.hyperx.ui.component.ImageIcon
import dev.lackluster.hyperx.ui.preference.TextPreference as HyperXTextPreference

/**
 * TextPreference 在用户点击后先判断模块是否激活, 如未激活则发出 toast
 */
@Composable
fun TextPreference(
    icon: ImageIcon? = null,
    title: String,
    summary: String? = null,
    value: String? = null,
    ignoreModuleActiveStatus: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    HyperXTextPreference(
        title = title,
        icon = icon,
        summary = summary,
        value = value,
        onClick = {
            if (!YukiHookAPI.Status.isXposedModuleActive && !ignoreModuleActiveStatus) {
                context.toast(R.string.make_sure_active)
            } else {
                onClick?.invoke()
            }
        }
    )
}
