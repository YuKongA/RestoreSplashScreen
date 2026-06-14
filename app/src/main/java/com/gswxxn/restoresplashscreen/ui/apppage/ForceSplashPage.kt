package com.gswxxn.restoresplashscreen.ui.apppage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.ui.component.AppListPage

/**
 * 实验功能 - 强制显示遮罩 - 配置应用列表
 */
@Composable
fun ForceSplashPage() {
    AppListPage(
        stringResource(R.string.force_show_splash_screen_title),
        DataConst.FORCE_SHOW_SPLASH_SCREEN_LIST
    )
}
