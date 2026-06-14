package com.gswxxn.restoresplashscreen.data

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 应用内导航路由（基于 navigation3 的 NavKey）
 *
 * 主页对应 hyperx 提供的 [dev.lackluster.hyperx.navigation.HyperXRoute.Main]，
 * 这里仅定义各子页面的路由。
 */
@Serializable
sealed interface Route : NavKey {
    @Serializable data object About : Route
    @Serializable data object Basic : Route
    @Serializable data object Scope : Route
    @Serializable data object Icon : Route
    @Serializable data object Bottom : Route
    @Serializable data object Background : Route
    @Serializable data object Display : Route
    @Serializable data object Developer : Route

    @Serializable data object CustomScope : Route
    @Serializable data object IgnoreAppIcon : Route
    @Serializable data object HideIcon : Route
    @Serializable data object RemoveBranding : Route
    @Serializable data object BackgroundExcept : Route
    @Serializable data object BgIndividual : Route
    @Serializable data object MinDuration : Route
    @Serializable data object ForceSplash : Route

    @Serializable data class ColorPicker(val pkgName: String = "") : Route
}
