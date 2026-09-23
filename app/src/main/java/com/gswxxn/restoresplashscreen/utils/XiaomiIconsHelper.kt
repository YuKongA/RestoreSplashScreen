package com.gswxxn.restoresplashscreen.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.UserHandle
import android.provider.Settings
import com.gswxxn.restoresplashscreen.hook.SystemUIHooker
import com.gswxxn.restoresplashscreen.hook.base.HookManager
import com.gswxxn.restoresplashscreen.hook.systemui.IconHookHandler.getActivityIconOrApp
import com.gswxxn.restoresplashscreen.hook.utils.ReflectCache
import com.gswxxn.restoresplashscreen.hook.utils.getValueFrom
import com.gswxxn.restoresplashscreen.hook.utils.setValueTo
import com.gswxxn.restoresplashscreen.hook.utils.toTyped
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.toClass
import com.highcapable.kavaref.extension.toClassOrNull

/**
 * 用于从小米桌面检索大图标的辅助类
 *
 * 仅在首次调用 hasLargeIcon / getLargeIconSize / getLargeIconDrawable 时才触发
 */
class XiaomiIconsHelper(private val context: Context, private val classLoader: ClassLoader) {
    private val miuiHomeContext by lazy {
        context.createPackageContext(
            "com.miui.home",
            Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
        )
    }

    /**
     * HyperOS 4 起, 大图标与 MAML 图标实现已从 com.miui.home 迁移到
     * miui.systemui.plugin, 需要通过插件包的 ClassLoader 加载。
     */
    private val systemUiPluginContext by lazy {
        context.createPackageContext(
            "miui.systemui.plugin",
            Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
        )
    }

    /** 按 SystemUI -> SystemUI 插件 -> 小米桌面 的顺序查找宿主类。 */
    private fun loadHostClass(name: String): Class<Any>? =
        name.toClassOrNull(loader = classLoader)
            ?: runCatching { name.toClassOrNull(loader = systemUiPluginContext.classLoader) }.getOrNull()
            ?: runCatching { name.toClassOrNull(loader = miuiHomeContext.classLoader) }.getOrNull()

    private val largeIconsHelperClazz by lazy {
        loadHostClass("com.miui.maml.util.LargeIconsHelper")
            ?: "com.miui.maml.util.LargeIconsHelper".toClass(loader = miuiHomeContext.classLoader)
    }

    private val dependencyClazz by lazy {
        "com.miui.systemui.MiuiDependency".toClassOrNull(loader = classLoader)
            ?: "com.android.systemui.Dependency".toClassOrNull(loader = classLoader)
    }

    private val mDependencyGet by lazy {
        dependencyClazz?.resolve()?.optional()?.firstMethodOrNull {
            name = "get"
            parameterCount = 1
            parameters(Class::class)
        }?.self
    }

    private val interfacesImplManagerClazz by lazy {
        "com.miui.systemui.interfacesmanager.InterfacesImplManager".toClassOrNull(loader = classLoader)
    }

    private val mImplManagerGet by lazy {
        interfacesImplManagerClazz?.resolve()?.optional()?.firstMethodOrNull {
            name = "getImpl"
            parameterCount = 1
            parameters(Class::class)
        }?.self
    }

    private val appIconsManagerClazz by lazy {
        "com.miui.systemui.graphics.AppIconsManager".toClass(loader = classLoader)
    }

    private val loadAppIcon by lazy {
        appIconsManagerClazz.getDeclaredMethod(
            "loadAppIcon",
            classOf<String>(), classOf<Int>(), classOf<ApplicationInfo>(), classOf<PackageManager>()
        )
    }

    private val appIconsManager by lazy {
        mDependencyGet?.invoke(null, appIconsManagerClazz)
            ?: mImplManagerGet?.invoke(null, appIconsManagerClazz)
    }

    private val appIconsHelperClazz by lazy {
        loadHostClass("com.miui.maml.util.AppIconsHelper")
    }

    private val getIconDrawableMethod by lazy {
        appIconsHelperClazz?.resolve()?.optional()?.firstMethodOrNull {
            name = "getIconDrawable"
            parameters(Context::class, PackageItemInfo::class, PackageManager::class)
        }?.self
    }

    private val drawableUtilsClazz by lazy {
        "com.miui.utils.DrawableUtils".toClassOrNull(loader = classLoader)
    }

    private val getFancyChildOrSelf by lazy {
        drawableUtilsClazz?.resolve()?.optional()?.firstMethodOrNull {
            name = "getFancyChildOrSelf"
            parameterCount = 2
            parameters(Drawable::class, Boolean::class)
        }?.self
    }

    /** 当前是否启用 MIUI 完美图标 */
    val isSupportMIUIModeIcon by lazy {
        Settings.System.getInt(context.contentResolver, "key_miui_mod_icon_enable", 0) == 1 || getFancyChildOrSelf != null
    }

    // 以下成员进程内恒定, 解析一次复用; hasLargeIcon / getLargeIconSize / getLargeIconDrawable
    // 每次应用启动都会调用, 不能在方法体内重复做全表反射扫描

    private val userHandleCurrent by lazy {
        classOf<UserHandle>().resolve().firstField { name = "CURRENT" }.getValueFrom<UserHandle, Any>(null)
    }

    private val hasLargeIconMethod by lazy {
        largeIconsHelperClazz.resolve().optional().firstMethodOrNull {
            name = "hasLargeIcon"
            parameters(String::class, String::class, String::class, UserHandle::class)
        }?.toTyped<Boolean>()
    }

    private val getLargeIconConfigFileMethod by lazy {
        largeIconsHelperClazz.resolve().optional().firstMethodOrNull {
            name = "getLargeIconConfigFile"
            parameters(String::class, Boolean::class)
        }?.toTyped<Any>()
    }

    private val getLargeIconDrawableMethod by lazy {
        largeIconsHelperClazz.resolve().optional().firstMethodOrNull {
            name = "getLargeIconDrawable"
            parameters(Context::class, String::class, String::class, String::class, String::class, Long::class, UserHandle::class)
        }?.toTyped<Any>()
    }

    private val sManagerListField by lazy {
        largeIconsHelperClazz.resolve().optional().firstFieldOrNull { name = "sManagerList" }
    }

    private var hooksInstalled = false

    @SuppressLint("DiscouragedApi")
    private fun ensureHooksInstalled() {
        if (hooksInstalled) return
        hooksInstalled = true
        try {
            // 防止获取到 System UI 的 Resources
            HookManager(true) {
                loadHostClass("miuix.pickerwidget.date.CalendarFormatSymbols")
                    ?.resolve()?.optional()?.firstMethodOrNull { name = "getWeekDays" }?.self
            }.addReplaceHook({ true }) {
                val resources = miuiHomeContext.resources
                val id = resources.getIdentifier("week_days", "array", "com.miui.home")
                resources.getStringArray(id)
            }.startHook(SystemUIHooker.module)

            // 为获取完美图标时设置一个缓存时间, 避免获取费时图标(如天气)时, 经常显示不出数据的问题 原调用为固定值 0.
            HookManager(true) {
                appIconsHelperClazz
                    ?.resolve()?.optional()?.firstMethodOrNull { name = "getFancyIconDrawable" }?.self
            }.addBeforeHook({ true }) {
                val packageName = args(args.indexOfFirst { it is String }).string()
                val cacheTimeIndex = args.indexOfFirst { it is Long }
                args(cacheTimeIndex).set(getCacheTime(packageName))
            }.startHook(SystemUIHooker.module)

            // 只获取本地天气数据, 不获取网络数据; 参考 https://zhuti.designer.xiaomi.com/docs/blog/weatherApi.html
            HookManager(true) {
                loadHostClass("com.miui.maml.data.ContentProviderBinder")
                    ?.resolve()?.optional()?.firstMethodOrNull { name = "getUriText" }?.self
            }.addAfterHook({ true }) {
                if (result == "content://weather/actualWeatherData/1")
                    result = "content://weather/actualWeatherData/2"
            }.startHook(SystemUIHooker.module)

            // 由于大图标的变更通知不到系统界面, 所以只能每次都重新读取配置
            HookManager(true) {
                largeIconsHelperClazz
                    .resolve().optional().firstMethodOrNull { name = "hasLargeIcon" }?.self
            }.addBeforeHook({ true }) {
                sManagerListField?.setValueTo(null, null)
            }.startHook(SystemUIHooker.module)
        } catch (t: Throwable) {
            XMLog.e(t, "MIUIIconsHelper")
        }
    }

    /**
     * 检查给定的程序是否存在大图标
     *
     * @param packageName 要检查的程序包的名称。
     * @return 如果程序包有大图标则返回 `true`，否则返回 `false`。
     */
    fun hasLargeIcon(packageName: String) = try {
        ensureHooksInstalled()
        hasLargeIconMethod?.invoke(null, packageName, null, "desktop", userHandleCurrent) ?: false
    } catch (e: Throwable) {
        XMLog.e(t = e) { "Failed to get hasLargeIcon for package $packageName" }
        false
    }

    /**
     * 获取指定程序包的大图标的尺寸。
     *
     * @param packageName 需要获取大图标尺寸的程序包的名称。
     * @return 如果成功获取大图标的尺寸则返回该尺寸，否则在捕获异常后返回null。
     */
    fun getLargeIconSize(packageName: String) = try {
        ensureHooksInstalled()
        val iconsConfigs = getLargeIconConfigFileMethod?.invoke(null, "desktop", false)?.let { configFile ->
            ReflectCache.invokeMethod<HashMap<String, Any>>(configFile, "getIconsConfigs")
        }
        iconsConfigs?.get(packageName)?.let { config ->
            ReflectCache.getField<String>(config, "size")
        }
    } catch (e: Throwable) {
        XMLog.e(t = e) { "Failed to get large icon size for package $packageName" }
        null
    }

    /**
     * 获取指定包名的大图标
     *
     * @param packageName 应用程序的包名
     * @return 原始大图标可绘制对象，如果未找到则为 null
     */
    fun getLargeIconDrawable(packageName: String) = try {
        ensureHooksInstalled()
        getLargeIconDrawableMethod?.invoke(
            null, miuiHomeContext, packageName, null, "desktop", null, 0L, userHandleCurrent
        )?.let { largeIcon ->
            ReflectCache.invokeMethod<Drawable>(largeIcon, "getDrawable")
                ?.let(::resolveDynamicDrawable)
        }
    } catch (e: Throwable) {
        XMLog.e(t = e) { "Failed to get large icon drawable for package $packageName" }
        null
    }

    /**
     * 从指定的应用程序包中获取完美的图标 Drawable
     *
     * @param packageName 要获取图标的应用程序包的包名。
     * @param userId 应用程序的用户 ID。
     * @param applicationInfo 应用程序的 ApplicationInfo 对象。
     * @return 如果成功获取到完美图标，则返回一个 BitmapDrawable；如果发生错误，则回退到默认方式获取。
     */
    fun getFancyIconDrawable(
        packageName: String,
        userId: Int,
        applicationInfo: ApplicationInfo?
    ): Drawable? {
        ensureHooksInstalled()
        val pm = SystemUIHooker.appContext?.packageManager ?: return null
        val rawDrawable = getRawFancyIconDrawable(applicationInfo, pm)?.let(::resolveDynamicDrawable)
        val managerDrawable = runCatching {
            loadAppIcon.invoke(
                appIconsManager,
                packageName,
                userId,
                applicationInfo,
                pm
            ) as? Drawable
        }.getOrNull()?.let(::resolveDynamicDrawable)

        // 首次调用会初始化 MAML/RendererCore 缓存。AppIconsManager 返回位图时再取一次，
        // 避免冷启动第一次仍拿到尚未建立完成的静态图标。
        val warmedRawDrawable = if (managerDrawable is BitmapDrawable) {
            getRawFancyIconDrawable(applicationInfo, pm)?.let(::resolveDynamicDrawable)
                ?: rawDrawable
        } else {
            rawDrawable
        }

        // AppIconsManager 会把部分主题图标转为低分辨率 BitmapDrawable。
        // 原始 Drawable 可保留矢量/MAML 图层, 同时让无描边处理继续生效。
        return when {
            managerDrawable is BitmapDrawable && warmedRawDrawable != null -> warmedRawDrawable
            managerDrawable != null -> managerDrawable
            warmedRawDrawable != null -> warmedRawDrawable
            else -> getActivityIconOrApp(pm)
        }
    }

    private fun getRawFancyIconDrawable(
        applicationInfo: ApplicationInfo?,
        packageManager: PackageManager
    ): Drawable? = runCatching {
        if (applicationInfo == null) null
        else getIconDrawableMethod?.invoke(null, context, applicationInfo, packageManager) as? Drawable
    }.getOrNull()

    /**
     * 将桌面图标的静态外壳替换为其动态 MAML 内容。
     *
     * - [AnimatingDrawable] 默认只绘制 quietImage, 动态内容需取 getFancyDrawable()
     * - AdaptiveIconDrawable 的动态背景/前景层需要展开后再交给 AdaptiveIconDrawable 绘制
     * - [LargeIconDrawable] 是资源包装器, 需要先取出内部 Drawable
     */
    fun resolveDynamicDrawable(drawable: Drawable?): Drawable? {
        drawable ?: return null
        val resolved = when (drawable.javaClass.name) {
            "com.miui.maml.AnimatingDrawable" ->
                ReflectCache.invokeMethod<Drawable>(drawable, "getFancyDrawable") ?: drawable

            "com.miui.maml.LargeIconDrawable" ->
                ReflectCache.invokeMethod<Drawable>(drawable, "getDrawable")
                    ?.let(::resolveDynamicDrawable) ?: drawable

            else -> if (drawable is android.graphics.drawable.AdaptiveIconDrawable) {
                resolveDynamicAdaptiveDrawable(drawable)
            } else {
                drawable
            }
        }
        return resolved
    }

    /**
     * 激活并预热动态 MAML 图层。
     *
     * SplashScreen 不会像桌面 IconView 一样分发 Drawable 生命周期回调, 新建的 FancyDrawable
     * 首次绘制可能仍是未初始化状态。这里主动 resume 并绘制一次, 确保首启即为真实内容。
     */
    fun warmUpDynamicDrawable(drawable: Drawable, size: Int) {
        if (size <= 0 || !resumeDynamicDrawable(drawable)) return
        val previousBounds = Rect(drawable.bounds)
        runCatching {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            canvas.setBitmap(null)
            bitmap.recycle()
        }
        drawable.bounds = previousBounds
    }

    private fun resumeDynamicDrawable(drawable: Drawable?): Boolean {
        drawable ?: return false
        var resumed = false
        when {
            drawable.javaClass.name == "com.miui.maml.FancyDrawable" -> {
                ReflectCache.invokeMethod<Any>(drawable, "onResume")
                resumed = true
            }

            drawable is android.graphics.drawable.AdaptiveIconDrawable -> {
                resumed = resumeDynamicDrawable(drawable.background) or resumed
                resumed = resumeDynamicDrawable(drawable.foreground) or resumed
            }

            drawable is LayerDrawable -> {
                for (index in 0 until drawable.numberOfLayers) {
                    resumed = resumeDynamicDrawable(drawable.getDrawable(index)) or resumed
                }
            }
        }
        return resumed
    }

    private fun resolveDynamicAdaptiveDrawable(
        src: android.graphics.drawable.AdaptiveIconDrawable
    ): Drawable {
        val background = resolveDynamicDrawable(src.background) ?: src.background
        val foreground = resolveDynamicLayerDrawable(src.foreground) ?: src.foreground
        if (background === src.background && foreground === src.foreground) return src
        return android.graphics.drawable.AdaptiveIconDrawable(background, foreground)
    }

    private fun resolveDynamicLayerDrawable(src: Drawable?): Drawable? {
        if (src !is LayerDrawable) return resolveDynamicDrawable(src)

        val layers = Array(src.numberOfLayers) { index ->
            val layer = src.getDrawable(index)
            resolveDynamicDrawable(layer) ?: layer
        }
        if (layers.indices.all { layers[it] === src.getDrawable(it) }) return src

        return LayerDrawable(layers).apply {
            for (index in layers.indices) {
                runCatching { setLayerGravity(index, src.getLayerGravity(index)) }
                runCatching {
                    setLayerInset(
                        index,
                        src.getLayerInsetLeft(index),
                        src.getLayerInsetTop(index),
                        src.getLayerInsetRight(index),
                        src.getLayerInsetBottom(index)
                    )
                }
            }
        }
    }

    /**
     * 返回给定包名的缓存时间。
     *
     * @param packageName 要获取缓存时间的包名。
     * @return 缓存时间(毫秒)。
     */
    private fun getCacheTime(packageName: String) = when (packageName) {
        "com.miui.weather2" -> 3600000L
        "com.android.deskclock" -> 0L
        else -> 86400000L
    }
}
