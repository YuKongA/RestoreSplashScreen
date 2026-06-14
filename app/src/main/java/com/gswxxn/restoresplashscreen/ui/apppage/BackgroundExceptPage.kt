package com.gswxxn.restoresplashscreen.ui.apppage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.ui.component.AppListPage

/**
 * 背景 - 替换背景颜色 - 排除列表
 */
@Composable
fun BackgroundExceptPage() {
    AppListPage(
        stringResource(R.string.background_except_title),
        DataConst.BG_EXCEPT_LIST
    )
}
