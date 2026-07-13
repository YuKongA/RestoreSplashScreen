package com.gswxxn.restoresplashscreen.hook.utils

import com.gswxxn.restoresplashscreen.data.preference.Preferences
import com.gswxxn.restoresplashscreen.hook.base.BaseHookHandler
import com.gswxxn.restoresplashscreen.hook.utils.RemotePreferences.get
import com.gswxxn.restoresplashscreen.hook.utils.RemotePreferences.observe
import com.gswxxn.restoresplashscreen.utils.CommonUtils.toMap
import com.gswxxn.restoresplashscreen.utils.XMLog
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Hook 端工具类
 */
object HookExt {

    /** 已解析的 MapPrefs 缓存, 值变更时经 observe 失效, 避免每次应用启动重复解析 */
    private val mapPrefsCache = ConcurrentHashMap<String, MutableMap<String, String>>()

    /** 已注册失效监听的 key 集合 (监听只注册一次, 与缓存条目的增删解耦) */
    private val mapPrefsObservedKeys = ConcurrentHashMap.newKeySet<String>()

    /**
     * 读取 MapPrefs
     *
     * @param key [PreferenceKey] 实例
     * @return [MutableMap]（缓存共享实例，调用方请勿修改）
     */
    fun BaseHookHandler.getMapPrefs(key: PreferenceKey<Set<String>>): MutableMap<String, String> =
        mapPrefsCache.getOrPut(key.name) {
            if (mapPrefsObservedKeys.add(key.name)) {
                key.observe(fireImmediately = false) { mapPrefsCache.remove(key.name) }
            }
            prefs.get(key).toMap()
        }

    /**
     * 根据名称获取实例 的 Field 内容, 并转换为指定类型
     *
     * 需要获取 Field 的实例
     * @param fieldName Field 名称
     */
    fun <T> Any.getField(fieldName: String): T? = ReflectCache.getField(this, fieldName)

    /**
     * 打印日志
     *
     * 入参为 lambda, 日志关闭时直接返回, 不会构造日志字符串
     *
     * 开关走 [XMLog.isDebugEnabled] 缓存布尔 (由 HookEntry 经 observe 维护),
     * 日志关闭时 (常态) 零 prefs 读取; 仅开启后才读时间戳判断 24h 过期
     */
    inline fun printLog(msg: () -> String) {
        if (!XMLog.isDebugEnabled) return
        if (System.currentTimeMillis() - Preferences.Log.ENABLE_LOG_TIMESTAMP.get() > 86400000) return
        val text = msg()
        XMLog.i { text }
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
