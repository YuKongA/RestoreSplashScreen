package com.gswxxn.restoresplashscreen.hook.systemui

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import com.gswxxn.restoresplashscreen.data.StartingWindowInfo
import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.hook.SystemUIHooker
import com.gswxxn.restoresplashscreen.hook.base.BaseHookHandler
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.getMapPrefs
import com.gswxxn.restoresplashscreen.hook.utils.HookExt.printLog
import com.gswxxn.restoresplashscreen.hook.utils.ReflectCache
import com.gswxxn.restoresplashscreen.utils.MLog
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.toClass
import io.github.libxposed.api.XposedInterface
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import kotlin.time.Duration.Companion.milliseconds

/**
 * 此对象用于处理 基础设置 和 实验功能 中的 Hook
 */
object GenerateHookHandler : BaseHookHandler() {
    var currentPackageName = ""
    var currentComponentName = ""
    var currentActivity = ""
    var currentApplicationInfo = null as ApplicationInfo?
    var currentActivityInfo = null as ActivityInfo?
    var exceptCurrentApp = false
    var isHooking = false

    /**
     * 重置当前应用信息的缓存
     */
    private fun resetCache() {
        currentPackageName = ""
        currentComponentName = ""
        currentActivity = ""
        currentApplicationInfo = null
        currentActivityInfo = null
        isHooking = false
        exceptCurrentApp = false

        IconHookHandler.resetCache()
        BgHookHandler.resetCache()
    }

    /** 开始 Hook */
    override fun onHook() {

        // Hook 起始位置, 获取应用信息
        SystemUIHooker.Members.makeSplashScreenContentView.addBeforeHook({ true }) {
            var activityInfo: ActivityInfo?

            if (args[1]!! is ActivityInfo)
                activityInfo = args[1] as ActivityInfo
            else {
                val arg = args[1]!!
                activityInfo = ReflectCache.getField<ActivityInfo>(arg, "targetActivityInfo")
                if (activityInfo == null) {
                    val taskInfo = ReflectCache.getField<Any>(arg, "taskInfo")!!
                    activityInfo = ReflectCache.getField<ActivityInfo>(taskInfo, "topActivityInfo")!!
                }
            }

            isHooking = true
            currentPackageName = activityInfo.packageName
            currentComponentName = activityInfo.name
            currentActivity = activityInfo.targetActivity ?: "unknown activity"
            currentApplicationInfo = activityInfo.applicationInfo
            currentActivityInfo = activityInfo
            exceptCurrentApp = isExcept()

            printLog {
                "****** $currentPackageName; $currentActivity: makeSplashScreenContentView(): ${if (exceptCurrentApp) "except" else "allow"} this app"
            }

            /**
             * 强制开启启动遮罩
             *
             * 直接干预 build() 中的 if 判断
             */
            val forceEnableSplashScreen = prefs.get(Preferences.Display.FORCE_ENABLE_SPLASH_SCREEN)
            if (forceEnableSplashScreen) {
                if (!exceptCurrentApp) {
                    args(args.indexOfFirst { it is Int }).set(StartingWindowInfo.STARTING_WINDOW_TYPE_SPLASH_SCREEN)
                    printLog { "makeSplashScreenContentView(): forceEnableSplashScreen, set mSuggestType to STARTING_WINDOW_TYPE_SPLASH_SCREEN(1)" }
                }
            }
        }

        // 遮罩最小持续时间, 也是 Hook 结束位置, 清除缓存的应用信息
        SystemUIHooker.Members.removeStartingWindow.addReplaceHook({ true }) {
            if (exceptCurrentApp || !isHooking) callOriginal()
            else when (currentPackageName) {
                "" -> {
                    callOriginal()
                }

                // 单独配置应用最小持续时长
                in prefs.get(Preferences.AppList.MIN_DURATION_LIST) -> {
                    val configMap = getMapPrefs(Preferences.AppList.MIN_DURATION_CONFIG_MAP)
                    try {
                        val duration = configMap[currentPackageName].toString().toLong()

                        if (duration == 0L) callOriginal()
                        else {
                            printLog { "removeStartingWindow(): remove splash screen of $currentPackageName after $duration ms" }
                            delayCallOriginal(duration, instance, args)
                        }

                    } catch (_: NumberFormatException) {
                        printLog { "removeStartingWindow(): $currentPackageName: a NumberFormatException is threw, maybe it's MIN_DURATION config is incorrect" }
                        callOriginal()
                    }
                }

                // 默认值
                else -> prefs.get(Preferences.Display.MIN_DURATION).let { duration ->
                    if (duration == 0) callOriginal()
                    else {
                        printLog { "removeStartingWindow(): remove splash screen of $currentPackageName after $duration ms (default value)" }
                        delayCallOriginal(duration.toLong(), instance, args)
                    }
                }
            }

            // 清除缓存的应用信息
            resetCache()
            null
        }
    }

    /**
     * 延迟 [duration] 毫秒后调用 `removeStartingWindow` 的原方法
     *
     */
    private fun delayCallOriginal(duration: Long, instance: Any?, args: Array<Any?>) {
        val method = resolveRemoveStartingWindowMethod()
        val argsCopy = args.copyOf()
        MainScope().launch {
            delay(duration.milliseconds)
            try {
                if (method != null) {
                    val invoker: XposedInterface.Invoker<*, Method> = module.getInvoker(method)
                    invoker.setType(XposedInterface.Invoker.Type.Origin())
                    invoker.invoke(instance, *argsCopy)
                } else {
                    MLog.w { "delayCallOriginal(): removeStartingWindow Method 解析失败，无法延迟调用原方法" }
                }
            } catch (e: Throwable) {
                MLog.e(e)
            }
        }
    }

    /**
     * 解析 `ShellTaskOrganizer#removeStartingWindow` 的原始 [Method]
     */
    private fun resolveRemoveStartingWindowMethod(): Method? = try {
        "com.android.wm.shell.ShellTaskOrganizer".toClass(appClassLoader, false)
            .resolve()
            .firstMethodOrNull { name = "removeStartingWindow" }
            ?.self
    } catch (_: Throwable) {
        null
    }

    /**
     * 判断是否应执行Hook操作
     *
     * @return 是否应执行Hook操作
     */
    private fun isExcept(): Boolean {
        return if (currentPackageName.isBlank())
            true
        else {
            val list = prefs.get(Preferences.AppList.CUSTOM_SCOPE_LIST)
            val isExceptionMode = prefs.get(Preferences.Scope.IS_CUSTOM_SCOPE_EXCEPTION_MODE)
            (prefs.get(Preferences.Scope.ENABLE_CUSTOM_SCOPE)
                    && ((isExceptionMode && (currentPackageName in list))
                    || (!isExceptionMode && currentPackageName !in list)))
        }
    }
}
