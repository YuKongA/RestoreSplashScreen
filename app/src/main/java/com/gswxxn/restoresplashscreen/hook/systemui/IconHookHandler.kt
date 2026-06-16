package com.gswxxn.restoresplashscreen.hook.systemui

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Outline
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.hook.SystemUIHooker
import com.gswxxn.restoresplashscreen.hook.base.BaseHookHandler
import com.gswxxn.restoresplashscreen.hook.systemui.GenerateHookHandler.currentActivityInfo
import com.gswxxn.restoresplashscreen.hook.systemui.GenerateHookHandler.currentApplicationInfo
import com.gswxxn.restoresplashscreen.hook.systemui.GenerateHookHandler.currentComponentName
import com.gswxxn.restoresplashscreen.hook.systemui.GenerateHookHandler.currentPackageName
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.getDevPrefs
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.printLog
import com.gswxxn.restoresplashscreen.hook.utils.ReflectCache
import com.gswxxn.restoresplashscreen.hook.utils.getValueFrom
import com.gswxxn.restoresplashscreen.hook.utils.toTyped
import com.gswxxn.restoresplashscreen.ui.page.data.BGColorModes
import com.gswxxn.restoresplashscreen.ui.page.data.ChangeBGColorTypes
import com.gswxxn.restoresplashscreen.ui.page.data.ShrinkIconType
import com.gswxxn.restoresplashscreen.utils.CommonUtils.dp2px
import com.gswxxn.restoresplashscreen.utils.CommonUtils.isDarkMode
import com.gswxxn.restoresplashscreen.utils.DeviceUtils.isHyperOS
import com.gswxxn.restoresplashscreen.utils.GraphicUtils
import com.gswxxn.restoresplashscreen.utils.IconPackManager
import com.gswxxn.restoresplashscreen.utils.MIUIIconsHelper
import com.gswxxn.restoresplashscreen.wrapper.NoStrokeAdaptiveIconDrawable
import com.gswxxn.restoresplashscreen.wrapper.TransparentAdaptiveIconDrawable
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.toClass

/**
 * 此对象用于处理图标 Hook
 */
object IconHookHandler : BaseHookHandler() {
    var currentIconDominantColor: Int? = null
    private var currentIsNeedShrinkIcon = false

    /**
     * currentUseBigHyperOSLagerIcon 有三种状态:
     *
     * null: 当前没有使用小米大图标
     * true: 当前使用 1x2 或 2x1 或 2x2 的图标
     * false: 当前使用 1x1 的图标
     *
     */
    private var currentUseBigHyperOSLagerIcon: Boolean? = null
    private var currentIconDrawable: Drawable? = null

    /** 当前线程是否正处于 makeSplashScreenContentView 的同步执行区间内 */
    private val isInMakeSplashScreenContentView = ThreadLocal<Boolean>()

    /** com.android.internal.R.dimen.starting_surface_icon_size 资源 id (进程内恒定, 解析一次) */
    private val startingSurfaceIconSizeResId by lazy {
        $$"com.android.internal.R$dimen".toClass(loader = appClassLoader).resolve()
            .firstField { name = "starting_surface_icon_size" }.getValueFrom<Any, Int>(null)!!
    }

    private val iconPackManager by lazy { IconPackManager(appContext!!, prefs.get(Preferences.Icon.ICON_PACK_PACKAGE_NAME)) }
    private val miuiIcons by lazy { MIUIIconsHelper(appContext!!, appClassLoader) }

    /**
     * 重置当前应用的属性
     */
    fun resetCache() {
        currentIconDominantColor = null
        currentIsNeedShrinkIcon = false
        currentUseBigHyperOSLagerIcon = null
        currentIconDrawable = null
    }

    /** 开始 Hook */
    override fun onHook() {
        SystemUIHooker.Members.getWindowAttrs.addAfterHook {

            //忽略应用主动设置的图标
            val isDefaultStyle = prefs.get(Preferences.Icon.ENABLE_DEFAULT_STYLE) &&
                    if (prefs.get(Preferences.Scope.IS_DEFAULT_STYLE_LIST_EXCEPTION_MODE))
                        currentPackageName !in prefs.get(Preferences.AppList.DEFAULT_STYLE_LIST)
                    else
                        currentPackageName in prefs.get(Preferences.AppList.DEFAULT_STYLE_LIST)
            if (isDefaultStyle) {
                val attrs = args[1]!!
                ReflectCache.setField(attrs, "mSplashScreenIcon", null)
            }
            printLog { "getWindowAttrs():${if (isDefaultStyle) "" else " not"} ignore set icon" }
        }

        // 处理 Drawable 图标
        SystemUIHooker.Members.getIcon_IconProvider.addAfterHook {
            printLog { "getIcon_IconProvider(): current method is getIcon" }
            result = processIconDrawable(result as Drawable)
        }

        // 执行缩小图标
        SystemUIHooker.Members.createIconDrawable.addBeforeHook {
            if (currentUseBigHyperOSLagerIcon == true) {
                val size = ReflectCache.getField<Int>(instance!!, "mFinalIconSize") ?: 0
                ReflectCache.setField(instance!!, "mFinalIconSize", (size * 1.35).toInt())
                printLog { "createIconDrawable(): execute enlarge icon" }
            } else if (currentIsNeedShrinkIcon) {
                val size = ReflectCache.getField<Int>(instance!!, "mFinalIconSize") ?: 0
                ReflectCache.setField(instance!!, "mFinalIconSize", (size / 1.5).toInt())
                printLog { "createIconDrawable(): execute shrink icon" }
            }
        }

        // 模糊背景 + 圆角
        SystemUIHooker.Members.build_SplashScreenViewBuilder.addAfterHook {
            val splashScreenView = result as FrameLayout
            val iconView = ReflectCache.getField<ImageView>(splashScreenView, "mIconView")
                ?: return@addAfterHook

            // 创建模糊背景 View
            if (prefs.get(Preferences.Icon.SHRINK_ICON) != ShrinkIconType.NotShrinkIcon.ordinal &&
                prefs.get(Preferences.Icon.ENABLE_ADD_ICON_BLUR_BG) &&
                currentIsNeedShrinkIcon &&
                currentUseBigHyperOSLagerIcon != true
            ) {
                val blurIconSize = (appResources!!.getDimensionPixelSize(startingSurfaceIconSizeResId) / 1.5).toInt()
                val bgIconSize = blurIconSize * 4

                val blurBgDrawable = GraphicUtils.createShadowedIcon(
                    appContext,
                    currentIconDrawable,
                    blurIconSize,
                    blurIconSize * 4,
                    blurIconSize * getDevPrefs(Preferences.Dev.DEV_ICON_ROUND_CORNER_RATE) / 100f
                )
                if (blurBgDrawable != null) {
                    val iconBlurBGView = ImageView(appContext).apply {
                        setImageDrawable(blurBgDrawable)
                        setRenderEffect(
                            RenderEffect.createBlurEffect(
                                bgIconSize.toFloat() / 10,
                                bgIconSize.toFloat() / 10,
                                Shader.TileMode.DECAL
                            )
                        )
                        z = -1f
                    }
                    splashScreenView.addView(
                        iconBlurBGView,
                        FrameLayout.LayoutParams(bgIconSize, bgIconSize).apply { gravity = Gravity.CENTER }
                    )
                    iconView.alpha = 0.9f
                    printLog { "build_SplashScreenViewBuilder(): add icon blur bg" }
                }
            }

            // 绘制圆角（如果使用小米大图标, 则不绘制圆角）
            if (!prefs.get(Preferences.Display.ENABLE_DRAW_ROUND_CORNER) || currentUseBigHyperOSLagerIcon != true) {
                return@addAfterHook
            }
            val iconSize = ReflectCache.getField<Int>(instance!!, "mIconSize") ?: 0
            // 如果没有图标，则不绘制圆角
            if (iconSize == 0) return@addAfterHook
            val iconDrawable = ReflectCache.getField<Drawable>(instance!!, "mIconDrawable")
                ?: return@addAfterHook
            // 不为动态图标绘制圆角
            if ($$"android.window.SplashScreenView$IconAnimateListener".toClass(loader = appClassLoader) in iconDrawable.javaClass.interfaces) {
                return@addAfterHook
            }
            // 提前计算圆角参数
            val border = dp2px(appContext!!, 1.5f)
            val cornerRadius = iconSize.toFloat() * getDevPrefs(Preferences.Dev.DEV_ICON_ROUND_CORNER_RATE) / 100
            iconView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(border, border, view.width - border, view.height - border, cornerRadius)
                }
            }
            // 启用轮廓剪裁
            iconView.clipToOutline = true
            printLog { "build_SplashScreenViewBuilder(): draw icon round corner" }
        }

        // 标记 makeSplashScreenContentView 的同步执行区间
        SystemUIHooker.Members.makeSplashScreenContentView.addBeforeHook({ true }) {
            isInMakeSplashScreenContentView.set(true)
        }
        SystemUIHooker.Members.makeSplashScreenContentView.addAfterHook({ true }) {
            isInMakeSplashScreenContentView.set(false)
        }

        // 不使用自带的图标缩放, 防止在 HyperOS 上出现图标白边及图标错位
        SystemUIHooker.Members.normalizeAndWrapToAdaptiveIcon.addBeforeHook {
            if (isInMakeSplashScreenContentView.get() == true) {
                printLog { "normalizeAndWrapToAdaptiveIcon(): avoid shrink icon by system ui" }
                val boolShrinkNonAdaptiveIconsIndex = args.indexOfFirst { it is Boolean }
                if (boolShrinkNonAdaptiveIconsIndex != -1) {
                    args(boolShrinkNonAdaptiveIconsIndex).set(false)
                } else {
                    val normalizer = instance!!.javaClass.resolve().optional().firstMethodOrNull {
                        name = "getNormalizer"
                        superclass()
                    }?.toTyped<Any>()?.invoke(instance)
                    val scale = normalizer?.javaClass?.resolve()?.optional()?.firstMethodOrNull {
                        name = "getScale"
                        parameterCount = 4
                        superclass()
                    }?.toTyped<Float>()?.invoke(
                        normalizer,
                        args.first { it is Drawable },
                        args.first { it is RectF },
                        null,
                        null
                    ) ?: 0.92f
                    (args(args.indexOfFirst { it is FloatArray }).any() as FloatArray)[0] = scale
                    val oriDrawable = args.first { it is Drawable } as Drawable
                    val returnType = SystemUIHooker.Members.normalizeAndWrapToAdaptiveIcon.returnType
                    result = if (returnType == classOf<AdaptiveIconDrawable>())
                        TransparentAdaptiveIconDrawable(oriDrawable)
                    else
                        oriDrawable
                }
            }
        }

        SystemUIHooker.Members.createIconBitmap_BaseIconFactory.addBeforeHook {
            (args(0).any() as? Drawable)?.let { drawable ->
                printLog { "createIconBitmap_BaseIconFactory(): avoid shrink icon by system ui" }
                result = GraphicUtils.drawable2Bitmap(drawable, getIconSize(drawable))
            }
        }

        // 强制使图标背景被判断为复杂, 以防止安卓抹去简单的图标背景
        SystemUIHooker.Members.iconColor_constructor.addAfterHook {
            ReflectCache.setField(instance!!, "mIsBgComplex", true)
        }
    }

    /**
     * 处理图标 Drawable 的方法
     *
     * 实现功能:
     * - 替换获取图标方式
     * - 不显示 Splash Screen 图标
     * - 使用图标包
     * - 绘制图标圆角
     *
     * @param oriDrawable 原始 Drawable 对象
     * @return 处理后的 Drawable 对象
     */
    fun processIconDrawable(oriDrawable: Drawable): Drawable {
        val shrinkIconType = prefs.get(Preferences.Icon.SHRINK_ICON)

        val isHideSplashScreenIcon = prefs.get(Preferences.Icon.ENABLE_HIDE_SPLASH_SCREEN_ICON) &&
                if (prefs.get(Preferences.Scope.IS_HIDE_SPLASH_SCREEN_ICON_EXCEPTION_MODE))
                    currentPackageName !in prefs.get(Preferences.AppList.HIDE_SPLASH_SCREEN_ICON_LIST)
                else
                    currentPackageName in prefs.get(Preferences.AppList.HIDE_SPLASH_SCREEN_ICON_LIST)

        val iconSize = getIconSize(oriDrawable)

        // 不显示 Splash Screen 图标
        if (isHideSplashScreenIcon) {
            printLog { "getIcon(): draw TRANSPARENT icon" }
            return Color.TRANSPARENT.toDrawable()
        }

        // 检索图标优先级: 使用小米大图标 -> 使用图标包 -> 替换获取图标方式 -> 原始图标
        val iconDrawable = getHyperOSLargeIcon() ?: getIconFromIconPack() ?: replaceWayOfGetIcons() ?: oriDrawable

        // 判断是否需要缩小图标
        when (shrinkIconType) {
            ShrinkIconType.NotShrinkIcon.ordinal -> currentIsNeedShrinkIcon = false
            ShrinkIconType.ShrinkLowResolutionIcon.ordinal -> currentIsNeedShrinkIcon =
                if (iconDrawable !is AdaptiveIconDrawable) iconDrawable.intrinsicWidth < iconSize / 1.5 else false

            ShrinkIconType.ShrinkAllIcon.ordinal -> currentIsNeedShrinkIcon = true
        }
        printLog { "getIcon(): currentIsNeedShrinkIcon: $currentIsNeedShrinkIcon" }

        // 获取图标颜色: 仅当背景颜色取自图标时进行
        if (prefs.get(Preferences.Background.CHANG_BG_COLOR_TYPE) == ChangeBGColorTypes.FromIcon.ordinal) {
            val colorMode = prefs.get(Preferences.Background.BG_COLOR_MODE)
            val bitmap = GraphicUtils.drawable2Bitmap(iconDrawable, 112)
            currentIconDominantColor = GraphicUtils.getBgColor(
                bitmap,
                when (colorMode) {
                    BGColorModes.DarkColor.ordinal -> false
                    BGColorModes.FollowSystem.ordinal -> !isDarkMode(appContext!!)
                    else -> true
                }
            )
        }

        // 移除 HyperOS 为自适应图标强制添加的边缘描边
        val finalIconDrawable =
            if (isHyperOS && iconDrawable is AdaptiveIconDrawable && prefs.get(Preferences.Icon.ENABLE_REMOVE_ICON_STROKE)) {
                printLog { "getIcon(): remove icon stroke" }
                NoStrokeAdaptiveIconDrawable.from(iconDrawable)
            } else {
                iconDrawable
            }
        currentIconDrawable = finalIconDrawable
        return finalIconDrawable
    }

    /**
     * 获取 SplashScreen 图标的大小
     *
     * @param drawable 要获取大小的 Drawable 对象
     * @return 图标的大小
     */
    private fun getIconSize(drawable: Drawable): Int {
        val mIconSize = appResources!!.getDimensionPixelSize(startingSurfaceIconSizeResId)

        return if (drawable is AdaptiveIconDrawable) (mIconSize * 1.2 + 0.5).toInt()
        else mIconSize
    }

    /** 使用小米大图标 */
    private fun getHyperOSLargeIcon(): Drawable? {
        if (isHyperOS && prefs.get(Preferences.Icon.ENABLE_USE_MIUI_LARGE_ICON) && miuiIcons.hasLargeIcon(currentPackageName)) {
            printLog { "getIcon(): use MIUI Large Icon" }
            return miuiIcons.getLargeIconDrawable(currentPackageName)?.let {
                val largeIconSize = miuiIcons.getLargeIconSize(currentPackageName)
                printLog { "getIcon(): large icon size: $largeIconSize" }
                currentUseBigHyperOSLagerIcon = if (largeIconSize in arrayOf("1x1", "1x2", "2x1", "2x2")) largeIconSize != "1x1" else null

                // 转换成正方形图标
                if (largeIconSize in arrayOf("1x2", "2x1")) {
                    GraphicUtils.convertToSquareDrawable(it, appResources!!)
                } else {
                    it
                }
            }
        }
        return null
    }

    /** 使用图标包 */
    private fun getIconFromIconPack(): Drawable? {
        if (prefs.get(Preferences.Icon.ICON_PACK_PACKAGE_NAME) != "None") {
            printLog { "getIcon(): use Icon Pack" }
            return when {
                currentPackageName == "com.android.contacts" && currentComponentName.isNotEmpty() ->
                    iconPackManager.getIconByComponentName("ComponentInfo{com.android.contacts/$currentComponentName}")

                currentComponentName.isNotEmpty() ->
                    iconPackManager.getIconByComponentName("ComponentInfo{$currentPackageName/$currentComponentName}")
                        ?: iconPackManager.getIconByPackageName(currentPackageName)

                else -> iconPackManager.getIconByPackageName(currentPackageName)
            }
        }
        return null
    }

    /**
     * 替换获取图标方式
     *
     * 使用 Context.packageManager.getApplicationIcon() 的方式获取图标
     */
    private fun replaceWayOfGetIcons(): Drawable? {
        if (!prefs.get(Preferences.Icon.ENABLE_REPLACE_ICON)) return null

        printLog { "getIcon(): replace way of getting icon" }
        val pm = appContext!!.packageManager

        // 应用内通过 activity-alias + setComponentEnabledSetting 主动更换图标
        val launchedInfo = currentActivityInfo
        val isSwitchedAliasIcon = launchedInfo != null &&
                launchedInfo.targetActivity != null &&                   // 是 activity-alias
                launchedInfo.icon != 0 &&                                // alias 自带图标
                launchedInfo.icon != (currentApplicationInfo?.icon ?: 0) // 不同于默认应用图标

        return when {
            // 0、应用内主动更换的图标
            isSwitchedAliasIcon -> runCatching { launchedInfo.loadIcon(pm) }.getOrNull() ?: getActivityIconOrApp(pm)

            // 1、处理电话拨号界面（正常情况下已被阶段 0 覆盖）
            currentPackageName == "com.android.contacts" && currentComponentName.isNotEmpty() -> getActivityIconOrApp(pm)

            // 2、在 HyperOS 上尝试获取完美图标
            isHyperOS && miuiIcons.isSupportMIUIModeIcon -> miuiIcons.getFancyIconDrawable(
                currentPackageName,
                appUserId,
                currentApplicationInfo
            )

            // 3、优先使用 ComponentName 获取 Activity 图标, 失败时回退到 Application 图标
            else -> getActivityIconOrApp(pm)
        }
    }

    /**
     * 使用 ComponentName 获取 Activity 图标
     *
     * 失败时回退到 Application 图标
     * */
    fun getActivityIconOrApp(pm: PackageManager): Drawable {
        if (currentComponentName.isNotEmpty()) {
            try {
                return pm.getActivityIcon(ComponentName(currentPackageName, currentComponentName))
            } catch (_: Exception) {
                // 回退到 Application 图标
            }
        }
        return pm.getApplicationIcon(currentPackageName)
    }
}
