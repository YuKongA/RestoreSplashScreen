package com.gswxxn.restoresplashscreen.data.preference

import dev.lackluster.hyperx.ui.preference.core.PreferenceKey

/**
 * 模块远程 SharedPreferences 的键值定义。
 */
object Preferences {
    const val NAME = "config"
    const val VERSION = 1

    /** 备份时需要忽略的键，暂时为空。 */
    val BACKUP_BLACKLIST: List<String> by lazy { emptyList() }

    /** 日志 / 调试相关 */
    object Log {
        val ENABLE_LOG = PreferenceKey("enable_log", false)
        val ENABLE_LOG_TIMESTAMP = PreferenceKey("enable_log_timestamp", 0L)
    }

    /** 作用域相关 */
    object Scope {
        val ENABLE_CUSTOM_SCOPE = PreferenceKey("enable_custom_scope", false)
        val IS_CUSTOM_SCOPE_EXCEPTION_MODE = PreferenceKey("is_custom_scope_exception_mode", true)
        val IS_REMOVE_BRANDING_IMAGE_EXCEPTION_MODE = PreferenceKey("is_remove_branding_image_exception_mode", false)
        val IS_DEFAULT_STYLE_LIST_EXCEPTION_MODE = PreferenceKey("is_default_style_list_exception_mode", false)
        val IS_HIDE_SPLASH_SCREEN_ICON_EXCEPTION_MODE = PreferenceKey("is_hide_splash_screen_icon_exception_mode", false)
    }

    /** 图标相关 */
    object Icon {
        val REPLACE_TO_EMPTY_SPLASH_SCREEN = PreferenceKey("replace_to_empty_splash_screen", false)
        // 忽略应用主动设置的图标
        val ENABLE_DEFAULT_STYLE = PreferenceKey("enable_default_style", false)
        val ENABLE_HIDE_SPLASH_SCREEN_ICON = PreferenceKey("enable_hide_splash_screen_icon", false)
        val ENABLE_HIDE_ICON = PreferenceKey("enable_hide_icon", false)
        val ENABLE_REPLACE_ICON = PreferenceKey("enable_replace_icon", false)
        val ENABLE_USE_MIUI_LARGE_ICON = PreferenceKey("enable_use_miui_large_icon", false)
        val ENABLE_ADD_ICON_BLUR_BG = PreferenceKey("enable_add_blur_bg", false)
        val ICON_PACK_PACKAGE_NAME = PreferenceKey("icon_pack_package_name", "None")
        val SHRINK_ICON = PreferenceKey("shrink_icon", 0)
    }

    /** 背景相关 */
    object Background {
        val OVERALL_BG_COLOR = PreferenceKey("overall_bg_color", "#FFFFFF")
        val OVERALL_BG_COLOR_NIGHT = PreferenceKey("overall_bg_color_night", "#000000")
        val IGNORE_DARK_MODE = PreferenceKey("ignore_dark_mode", false)
        val REMOVE_BG_DRAWABLE = PreferenceKey("remove_bg_drawable", false)
        val SKIP_APP_WITH_BG_COLOR = PreferenceKey("skip_app_with_bg_color", true)
        val BG_COLOR_MODE = PreferenceKey("color_mode", 0)
        val CHANG_BG_COLOR_TYPE = PreferenceKey("change_bg_color_type", 0)
    }

    /** 显示 / 行为相关 */
    object Display {
        val REMOVE_BRANDING_IMAGE = PreferenceKey("remove_branding_image", false)
        val REDUCE_SPLASH_SCREEN = PreferenceKey("reduce_splash_screen", true)
        val FORCE_ENABLE_SPLASH_SCREEN = PreferenceKey("force_enable_splash_screen", false)
        val FORCE_SHOW_SPLASH_SCREEN = PreferenceKey("force_show_splash_screen", false)
        val DISABLE_SPLASH_SCREEN = PreferenceKey("disable_splash_screen", false)
        val ENABLE_HOT_START_COMPATIBLE = PreferenceKey("enable_hot_start_compatible", false)
        val ENABLE_DRAW_ROUND_CORNER = PreferenceKey("draw_round_corner", false)
        val MIN_DURATION = PreferenceKey("min_duration", 0)
    }

    /** 应用列表（StringSet） */
    object AppList {
        val CUSTOM_SCOPE_LIST = PreferenceKey("custom_scope_list", emptySet<String>())
        // 忽略应用主动设置的图标 应用列表
        val DEFAULT_STYLE_LIST = PreferenceKey("default_style_list", emptySet<String>())
        val HIDE_SPLASH_SCREEN_ICON_LIST = PreferenceKey("hide_splash_screen_icon_list", emptySet<String>())
        // 自适应背景颜色排除列表
        val BG_EXCEPT_LIST = PreferenceKey("bg_except_list", emptySet<String>())
        val REMOVE_BRANDING_IMAGE_LIST = PreferenceKey("remove_branding_image_list", emptySet<String>())
        val FORCE_SHOW_SPLASH_SCREEN_LIST = PreferenceKey("force_show_splash_screen_list", emptySet<String>())
        val MIN_DURATION_LIST = PreferenceKey("min_duration_list", emptySet<String>())
        val MIN_DURATION_CONFIG_MAP = PreferenceKey("min_duration_config_map", emptySet<String>())
        val INDIVIDUAL_BG_COLOR_APP_MAP = PreferenceKey("individual_bg_color_app_map", emptySet<String>())
        val INDIVIDUAL_BG_COLOR_APP_MAP_DARK = PreferenceKey("individual_bg_color_app_map_dark", emptySet<String>())
    }

    /** 开发者设置 */
    object Dev {
        val ENABLE_DEV_SETTINGS = PreferenceKey("enable_dev_settings", false)
        val DEV_ICON_ROUND_CORNER_RATE = PreferenceKey("dev_icon_round_corner", 25)
    }

    /** 模块应用（UI）设置 */
    object Module {
        val MODULE_APP_BLUR = PreferenceKey("module_blur", true)
        val SPLIT_VIEW = PreferenceKey("module_split", false)
        val SP_VERSION = PreferenceKey("sp_version", VERSION)
    }
}
