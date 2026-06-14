package com.gswxxn.restoresplashscreen.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.data.Route
import com.gswxxn.restoresplashscreen.ui.component.HeaderCard
import com.gswxxn.restoresplashscreen.ui.component.SwitchPreference
import com.gswxxn.restoresplashscreen.ui.component.TextPreference
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toast
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.navigation.Navigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.PreferenceGroup

/**
 * 底部 界面
 */
@Composable
fun BottomPage() {
    val navigator = LocalNavigator.current
    HyperXPage(
        title = stringResource(R.string.bottom_settings),
    ) {
        item {
            HeaderCard(
                imageResID = R.drawable.demo_branding,
                title = "BRANDING\nIMAGE",
                maxLines = 2
            )
            PreferenceGroup(position = ItemPosition.Last) { RemoveBrandingImageSettingsGroup(navigator) }
        }
    }
}

/**
 * 移除底部图片
 */
@Composable
private fun RemoveBrandingImageSettingsGroup(navigator: Navigator) {
    val context = LocalContext.current
    val prefs = context.prefs()
    val removeBrandingImage = remember { mutableStateOf(prefs.get(DataConst.REMOVE_BRANDING_IMAGE)) }

    // 移除底部图片
    SwitchPreference(
        title = stringResource(R.string.remove_branding_image),
        summary = stringResource(R.string.remove_branding_image_tips),
        prefsData = DataConst.REMOVE_BRANDING_IMAGE,
        checked = removeBrandingImage
    ) { newValue ->
        if (newValue) {
            context.toast(R.string.custom_scope_message)
        }
    }
    AnimatedVisibility(
        visible = removeBrandingImage.value,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // 配置移除列表
        TextPreference(title = stringResource(R.string.remove_branding_image_list)) {
            navigator.push(Route.RemoveBranding)
        }
    }
}
