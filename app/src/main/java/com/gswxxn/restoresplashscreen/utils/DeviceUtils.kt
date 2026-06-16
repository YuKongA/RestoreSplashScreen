package com.gswxxn.restoresplashscreen.utils

import com.highcapable.kavaref.extension.toClass

/**
 * 进程内的设备判断
 */
object DeviceUtils {
    val isHyperOS: Boolean by lazy { runCatching { "android.miui.R".toClass() }.isSuccess }

    val isColorOS: Boolean by lazy {
        runCatching { ("oppo.R").toClass() }.isSuccess ||
                runCatching { "com.color.os.ColorBuild".toClass() }.isSuccess ||
                runCatching { "oplus.R".toClass() }.isSuccess
    }
}
