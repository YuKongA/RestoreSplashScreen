package com.gswxxn.restoresplashscreen.hook.systemui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.graphics.toColorInt
import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.hook.SystemUIHooker
import com.gswxxn.restoresplashscreen.hook.base.BaseHookHandler
import com.gswxxn.restoresplashscreen.hook.systemui.GenerateHookHandler.currentPackageName
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.getMapPrefs
import com.gswxxn.restoresplashscreen.utils.DeviceUtils.isHyperOS
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.printLog
import com.gswxxn.restoresplashscreen.hook.utils.getValueFrom
import com.gswxxn.restoresplashscreen.ui.page.data.BGColorModes
import com.gswxxn.restoresplashscreen.ui.page.data.ChangeBGColorTypes
import com.gswxxn.restoresplashscreen.utils.CommonUtils.isDarkMode
import com.gswxxn.restoresplashscreen.utils.GraphicUtils
import com.gswxxn.restoresplashscreen.wrapper.SplashScreenViewBuilderWrapper
import com.highcapable.kavaref.KavaRef.Companion.resolve

/**
 * 此对象用于处理 背景 Hook
 */
object BgHookHandler : BaseHookHandler() {
    private var mTmpAttrsInstance: Any? = null

    /**
     * 重置当前应用的属性
     */
    fun resetCache() {
        mTmpAttrsInstance = null
    }

    /** 开始 Hook */
    override fun onHook() {
        SystemUIHooker.Members.getBGColorFromCache.addAfterHook {
            mTmpAttrsInstance = instance!!.javaClass.resolve().firstField { name = "mTmpAttrs" }.getValueFrom<Any, Any>(instance)
        }
        SystemUIHooker.Members.build_SplashScreenViewBuilder.addBeforeHook {
            val builder = SplashScreenViewBuilderWrapper.getInstance(instance!!)

            // 设置背景颜色
            getColor()?.let { builder.setBackgroundColor(it) }
        }
    }

    /**
     * 此处实现功能：
     * - 替换背景颜色
     * - 单独配置应用背景颜色
     */
    private fun getColor(): Int? {
        val isDarkMode = isDarkMode(appContext!!)
        val bgColorMode = prefs.get(Preferences.Background.BG_COLOR_MODE)
        val bgColorType = prefs.get(Preferences.Background.CHANG_BG_COLOR_TYPE)
        val isInBGExceptList = currentPackageName in prefs.get(Preferences.AppList.BG_EXCEPT_LIST)
        val ignoreDarkMode = prefs.get(Preferences.Background.IGNORE_DARK_MODE) || !isHyperOS
        val individualBgColorAppMap = getMapPrefs(
            if (!isDarkMode) Preferences.AppList.INDIVIDUAL_BG_COLOR_APP_MAP
            else Preferences.AppList.INDIVIDUAL_BG_COLOR_APP_MAP_DARK
        )
        val skipAppWithBgColor = bgColorType != 0 &&
                currentPackageName !in individualBgColorAppMap.keys &&
                prefs.get(Preferences.Background.SKIP_APP_WITH_BG_COLOR) &&
                (mTmpAttrsInstance!!.javaClass.resolve().firstField { name = "mWindowBgColor" }.getValueFrom<Any, Int>(mTmpAttrsInstance)
                    ?: 0) != 0

        if (skipAppWithBgColor) {
            printLog { "SplashScreenViewBuilder(): skip set bg color cuz app has been set bg color" }
            return null
        }

        return if (currentPackageName in individualBgColorAppMap.keys) {
            printLog { "SplashScreenViewBuilder(): set individual background color, ${individualBgColorAppMap[currentPackageName]}" }
            individualBgColorAppMap[currentPackageName]?.toColorInt()
        } else if (!isInBGExceptList && (!isDarkMode || ignoreDarkMode))
            when (bgColorType) {
                // 从图标取色
                ChangeBGColorTypes.FromIcon.ordinal -> {
                    printLog { "SplashScreenViewBuilder(): get adaptive background color" }
                    IconHookHandler.currentIconDominantColor ?: mTmpAttrsInstance!!.javaClass.resolve()
                        .firstField { name = "mSplashScreenIcon" }
                        .getValueFrom<Any, Drawable>(mTmpAttrsInstance)?.let { drawable ->
                            val bitmap = GraphicUtils.drawable2Bitmap(drawable, 100)
                            GraphicUtils.getBgColor(
                                bitmap,
                                when (bgColorMode) {
                                    BGColorModes.DarkColor.ordinal -> false
                                    BGColorModes.FollowSystem.ordinal -> !isDarkMode
                                    else -> true
                                }
                            )
                        }
                }
                // 从壁纸取色
                ChangeBGColorTypes.FromMonet.ordinal -> {
                    printLog { "SplashScreenViewBuilder(): get monet background color" }
                    when (bgColorMode) {
                        BGColorModes.LightColor.ordinal -> monetLightPrimaryContainer(appContext!!)
                        BGColorModes.DarkColor.ordinal -> monetDarkSurface(appContext!!)
                        else -> if (!isDarkMode)
                            monetLightPrimaryContainer(appContext!!)
                        else
                            monetDarkSurface(appContext!!)
                    }
                }
                // 自定义颜色
                ChangeBGColorTypes.FromCustom.ordinal -> {
                    printLog { "SplashScreenViewBuilder(): set overall background color" }
                    prefs.get(if (isDarkMode) Preferences.Background.OVERALL_BG_COLOR_NIGHT else Preferences.Background.OVERALL_BG_COLOR)
                        .toColorInt()
                }

                else -> {
                    printLog { "SplashScreenViewBuilder(): not replace background color" }; null
                }
            } else {
            printLog { "SplashScreenViewBuilder(): skip set bg color cuz app in except list" }; null
        }
    }

    /** 系统 Monet 浅色 primaryContainer */
    private fun monetLightPrimaryContainer(ctx: Context): Int =
        ctx.resources.getColor(android.R.color.system_primary_container_light, ctx.theme)

    /** 系统 Monet 深色 surface */
    private fun monetDarkSurface(ctx: Context): Int =
        ctx.resources.getColor(android.R.color.system_surface_dark, ctx.theme)
}
