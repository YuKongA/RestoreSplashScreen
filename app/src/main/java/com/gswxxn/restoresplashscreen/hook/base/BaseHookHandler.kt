package com.gswxxn.restoresplashscreen.hook.base

import com.gswxxn.restoresplashscreen.hook.SystemUIHooker
import com.gswxxn.restoresplashscreen.hook.utils.RemotePreferences.get
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey

/**
 * SystemUI 各 Hook 处理器的抽象基类
 */
abstract class BaseHookHandler {
    val module get() = SystemUIHooker.module
    val appClassLoader: ClassLoader get() = SystemUIHooker.classLoader
    val appContext get() = SystemUIHooker.appContext
    val appResources get() = SystemUIHooker.appContext?.resources
    val appUserId: Int get() = SystemUIHooker.appUserId
    val prefs = HookPrefs

    /** 在进行 Hook 时调用的方法 */
    abstract fun onHook()
}

/**
 * hook 进程内读取远程 SharedPreferences 的薄封装
 */
object HookPrefs {
    fun <T : Any> get(key: PreferenceKey<T>): T = key.get()
}
