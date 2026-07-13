package com.gswxxn.restoresplashscreen.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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

    private val largeIconsHelperClazz by lazy {
        "com.miui.maml.util.LargeIconsHelper".toClass(loader = miuiHomeContext.classLoader)
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
                "miuix.pickerwidget.date.CalendarFormatSymbols".toClassOrNull(loader = miuiHomeContext.classLoader)
                    ?.resolve()?.optional()?.firstMethodOrNull { name = "getWeekDays" }?.self
            }.addReplaceHook({ true }) {
                val resources = miuiHomeContext.resources
                val id = resources.getIdentifier("week_days", "array", "com.miui.home")
                resources.getStringArray(id)
            }.startHook(SystemUIHooker.module)

            // 为获取完美图标时设置一个缓存时间, 避免获取费时图标(如天气)时, 经常显示不出数据的问题 原调用为固定值 0.
            HookManager(true) {
                "com.miui.maml.util.AppIconsHelper".toClassOrNull(loader = miuiHomeContext.classLoader)
                    ?.resolve()?.optional()?.firstMethodOrNull { name = "getFancyIconDrawable" }?.self
            }.addBeforeHook({ true }) {
                val packageName = args(args.indexOfFirst { it is String }).string()
                val cacheTimeIndex = args.indexOfFirst { it is Long }
                args(cacheTimeIndex).set(getCacheTime(packageName))
            }.startHook(SystemUIHooker.module)

            // 只获取本地天气数据, 不获取网络数据; 参考 https://zhuti.designer.xiaomi.com/docs/blog/weatherApi.html
            HookManager(true) {
                "com.miui.maml.data.ContentProviderBinder".toClassOrNull(loader = miuiHomeContext.classLoader)
                    ?.resolve()?.optional()?.firstMethodOrNull { name = "getUriText" }?.self
            }.addAfterHook({ true }) {
                if (result == "content://weather/actualWeatherData/1")
                    result = "content://weather/actualWeatherData/2"
            }.startHook(SystemUIHooker.module)

            // 由于大图标的变更通知不到系统界面, 所以只能每次都重新读取配置
            HookManager(true) {
                "com.miui.maml.util.LargeIconsHelper".toClassOrNull(loader = miuiHomeContext.classLoader)
                    ?.resolve()?.optional()?.firstMethodOrNull { name = "hasLargeIcon" }?.self
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
    ) = try {
        val pm = SystemUIHooker.appContext!!.packageManager
        loadAppIcon.invoke(
            appIconsManager,
            packageName,
            userId,
            applicationInfo,
            pm
        )
    } catch (_: Throwable) {
        val pm = SystemUIHooker.appContext!!.packageManager
        getActivityIconOrApp(pm)
    } as Drawable?

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
