package com.gswxxn.restoresplashscreen.hook

import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.hook.base.HookManager
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.getField
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.printLog
import com.gswxxn.restoresplashscreen.hook.utils.toTyped
import com.gswxxn.restoresplashscreen.hook.utils.RemotePreferences.get
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.toClass
import io.github.libxposed.api.XposedModule

/**
 * Android 系统相关 Hook
 */
object AndroidHooker {
    /** 保存宿主（system_server）classLoader，供热重载后重新安装 Hook 使用 */
    @Volatile
    var classLoader: ClassLoader? = null
        private set

    fun init(module: XposedModule, classLoader: ClassLoader) {
        this.classLoader = classLoader

        val activityRecordClass = "com.android.server.wm.ActivityRecord".toClass(loader = classLoader)

        // launchedFromSystemSurface 进程内恒定, 解析一次复用; 下方 hook 每次 activity 启动都会执行,
        // 不能在 hook 体内做全表反射扫描
        val launchedFromSystemSurface = activityRecordClass.resolve().optional().firstMethodOrNull {
            name = "launchedFromSystemSurface"
            parameterCount = 0
        }?.toTyped<Boolean>()

        /**
         * 强制显示遮罩
         *
         * 类原始位置在 services.jar 中
         *
         * 此处在 evaluateStartingWindowTheme() 中被调用，最终将参数传递给 showStartingWindow()
         */
        HookManager {
            activityRecordClass.resolve().optional().firstMethodOrNull {
                name = "validateStartingWindowTheme"
                parameterCount = 3
            }?.self
        }.addBeforeHook({ true }) {
            val pkgName = args(1).string()
            // 惰性求值: 功能未启用 / 不在列表时, 不触发 launchedFromSystemSurface 反射调用
            val isForceShowSS = Preferences.Display.FORCE_SHOW_SPLASH_SCREEN.get()
                    && pkgName in Preferences.AppList.FORCE_SHOW_SPLASH_SCREEN_LIST.get()
                    && (!Preferences.Display.REDUCE_SPLASH_SCREEN.get()
                    || launchedFromSystemSurface?.invoke(instance) == true)

            if (isForceShowSS) resultTrue()
            printLog { "[Android] validateStartingWindowTheme():${if (isForceShowSS) "" else " not"} force show $pkgName splash screen" }
        }.startHook(module)

        // 彻底关闭 Splash Screen
        HookManager {
            activityRecordClass.resolve().optional().firstMethodOrNull {
                name = "showStartingWindow"
                parameterCount = 7
            }?.self
        }.addBeforeHook({ true }) {
            val currentPkgName = instance!!.getField<String>("packageName")

            val isDisableSS = Preferences.Display.DISABLE_SPLASH_SCREEN.get()
            printLog { "[Android] addStartingWindow():${if (isDisableSS) "" else " not"} disable $currentPkgName splash screen" }
            if (isDisableSS) resultNull()
        }.startHook(module)

        // 热启动时生成启动遮罩
        HookManager {
            activityRecordClass.resolve().optional().firstMethodOrNull {
                name = "getStartingWindowType"
                parameterCount = 7
            }?.self
        }.addBeforeHook({ true }) {
            val isHotStartCompatible = Preferences.Display.ENABLE_HOT_START_COMPATIBLE.get()
                    && Preferences.Display.FORCE_ENABLE_SPLASH_SCREEN.get()
                    && args(1).boolean()
            if (isHotStartCompatible) result = 2
            printLog { "[Android] getStartingWindowType():${if (isHotStartCompatible) "" else " not"} set result to 2" }
        }.startHook(module)
    }
}
