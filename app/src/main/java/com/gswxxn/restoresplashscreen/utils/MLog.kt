package com.gswxxn.restoresplashscreen.utils

import android.util.Log
import com.gswxxn.restoresplashscreen.BuildConfig
import io.github.libxposed.api.XposedModule

/**
 * 日志工具类
 * hook 进程内通过 [XposedModule.log] 写入框架日志，同时输出到 logcat。
 */
object MLog {
    const val DEFAULT_TAG = "RestoreSplashScreen"

    @Volatile
    private var module: XposedModule? = null

    @Volatile
    var isDebugEnabled: Boolean = BuildConfig.DEBUG

    fun init(module: XposedModule) {
        this.module = module
    }

    inline fun d(tag: String = DEFAULT_TAG, msg: () -> String) {
        if (isDebugEnabled) printLog(Log.DEBUG, tag, msg())
    }

    inline fun i(tag: String = DEFAULT_TAG, msg: () -> String) {
        printLog(Log.INFO, tag, msg())
    }

    inline fun w(tag: String = DEFAULT_TAG, msg: () -> String) {
        printLog(Log.WARN, tag, msg())
    }

    inline fun e(tag: String = DEFAULT_TAG, t: Throwable? = null, msg: () -> String) {
        printLog(Log.ERROR, tag, msg(), t)
    }

    fun e(t: Throwable, tag: String = DEFAULT_TAG) {
        printLog(Log.ERROR, tag, Log.getStackTraceString(t), t)
    }

    @PublishedApi
    internal fun printLog(priority: Int, tag: String, msg: String, t: Throwable? = null) {
        when (priority) {
            Log.DEBUG -> Log.d(tag, msg, t)
            Log.INFO -> Log.i(tag, msg, t)
            Log.WARN -> Log.w(tag, msg, t)
            Log.ERROR -> Log.e(tag, msg, t)
        }
        try {
            if (t != null) module?.log(priority, tag, msg, t)
            else module?.log(priority, tag, msg)
        } catch (_: Throwable) {
        }
    }
}
