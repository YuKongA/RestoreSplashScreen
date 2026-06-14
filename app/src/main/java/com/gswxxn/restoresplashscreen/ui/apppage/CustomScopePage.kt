package com.gswxxn.restoresplashscreen.ui.apppage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.ui.component.AppListPage
import com.gswxxn.restoresplashscreen.ui.component.SwitchPreference
import com.gswxxn.restoresplashscreen.utils.RemotePreferenceStore
import dev.lackluster.hyperx.ui.preference.PreferenceGroup
import org.koin.compose.koinInject

/**
 * 作用域 - 自定义模块作用域 - 配置应用列表
 */
@Composable
fun CustomScopePage() {
    val store = koinInject<RemotePreferenceStore>()
    var exceptionMode by remember {
        mutableStateOf(store.get(Preferences.Scope.IS_CUSTOM_SCOPE_EXCEPTION_MODE))
    }
    val exceptionSummary = stringResource(
        R.string.custom_scope_exception_mode_message,
        if (exceptionMode)
            stringResource(R.string.will_not)
        else
            stringResource(R.string.will_only)
    )
    AppListPage(
        stringResource(R.string.custom_scope_title),
        Preferences.AppList.CUSTOM_SCOPE_LIST
    ) {
        item {
            PreferenceGroup {
                SwitchPreference(
                    title = stringResource(R.string.exception_mode),
                    summary = exceptionSummary,
                    key = Preferences.Scope.IS_CUSTOM_SCOPE_EXCEPTION_MODE,
                    onCheckedChange = { exceptionMode = it }
                )
            }
        }
    }
}
