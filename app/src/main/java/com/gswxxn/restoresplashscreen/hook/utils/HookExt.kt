package com.gswxxn.restoresplashscreen.hook.utils

import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.hook.base.BaseHookHandler
import com.gswxxn.restoresplashscreen.hook.utils.RemotePreferences.get
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toMap
import com.gswxxn.restoresplashscreen.utils.MLog
import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey

/**
 * Hook 端工具类
 */
object HookExt {

    /**
     * 读取 MapPrefs
     *
     * @param key [PreferenceKey] 实例
     * @return [MutableMap]
     */
    fun BaseHookHandler.getMapPrefs(key: PreferenceKey<Set<String>>) = prefs.get(key).toMap()

    /**
     * 根据名称获取实例 的 Field 内容, 并转换为指定类型
     *
     * 需要获取 Field 的实例
     * @param fieldName Field 名称
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> Any.getField(fieldName: String): T? =
        this.javaClass.resolve().optional().firstFieldOrNull {
            name = fieldName
            superclass()
        }?.copy()?.of(this)?.get() as? T

    /**
     * 打印日志
     *
     * 入参为 lambda, 日志关闭时直接返回, 不会构造日志字符串
     */
    inline fun printLog(msg: () -> String) {
        if (!Preferences.Log.ENABLE_LOG.get()) return
        if (System.currentTimeMillis() - Preferences.Log.ENABLE_LOG_TIMESTAMP.get() > 86400000) return
        val text = msg()
        MLog.i { text }
    }

    /**
     * 加载 HookHandler
     */
    fun loadHookHandler(vararg hookHandler: BaseHookHandler) {
        hookHandler.forEach {
            it.onHook()
        }
    }

    /**
     * 获取 开发者选项 Prefs 值
     */
    fun <T : Any> BaseHookHandler.getDevPrefs(key: PreferenceKey<T>): T {
        if (prefs.get(Preferences.Dev.ENABLE_DEV_SETTINGS)) {
            return prefs.get(key)
        }
        return key.default
    }
}
