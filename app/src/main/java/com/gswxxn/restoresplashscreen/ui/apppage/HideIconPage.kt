package com.gswxxn.restoresplashscreen.ui.apppage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.ui.component.AppListPage
import com.gswxxn.restoresplashscreen.ui.component.SwitchPreference
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.lackluster.hyperx.ui.preference.PreferenceGroup

/**
 * 图标 - 不显示图标
 */
@Composable
fun HideIconPage() {
    val context = LocalContext.current
    var exceptionMode by remember {
        mutableStateOf(context.prefs().get(DataConst.IS_HIDE_SPLASH_SCREEN_ICON_EXCEPTION_MODE))
    }
    val exceptionSummary = stringResource(
        R.string.exception_mode_message,
        if (exceptionMode)
            stringResource(R.string.not_chosen)
        else
            stringResource(R.string.chosen)
    )
    AppListPage(
        stringResource(R.string.hide_splash_screen_icon_title),
        DataConst.HIDE_SPLASH_SCREEN_ICON_LIST
    ) {
        item {
            PreferenceGroup {
                SwitchPreference(
                    title = stringResource(R.string.exception_mode),
                    summary = exceptionSummary,
                    prefsData = DataConst.IS_HIDE_SPLASH_SCREEN_ICON_EXCEPTION_MODE,
                    onCheckedChange = { exceptionMode = it }
                )
            }
        }
    }
}
