package com.gswxxn.restoresplashscreen.wrapper

import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.makeAccessible
import com.highcapable.kavaref.extension.toClassOrNull
import java.lang.reflect.Method

/**
 * 用于 HyperOS 的无描边 AdaptiveIconDrawable
 */
class NoStrokeAdaptiveIconDrawable private constructor(
    background: Drawable?,
    foreground: Drawable?,
    monochrome: Drawable?
) : AdaptiveIconDrawable(background, foreground, monochrome) {

    override fun draw(canvas: Canvas) {
        val getter = getIsIconStroke
        val setter = setIsIconStroke
        if (getter == null || setter == null) {
            super.draw(canvas)
            return
        }
        val previous = runCatching { getter.invoke(null) as Boolean }.getOrDefault(true)
        try {
            runCatching { setter.invoke(null, false) }
            super.draw(canvas)
        } finally {
            runCatching { setter.invoke(null, previous) }
        }
    }

    companion object {
        private val iconCustomizerClass by lazy {
            "miui.content.res.IconCustomizer".toClassOrNull()
        }
        private val getIsIconStroke: Method? by lazy {
            runCatching {
                iconCustomizerClass
                    ?.getDeclaredMethod("getIsIconStroke")
                    ?.apply { makeAccessible() }
            }.getOrNull()
        }
        private val setIsIconStroke: Method? by lazy {
            runCatching {
                iconCustomizerClass
                    ?.getDeclaredMethod("setIsIconStroke", classOf<Boolean>())
                    ?.apply { makeAccessible() }
            }.getOrNull()
        }

        /**
         * 用 [src] 的前景/背景/单色图层重建一个无描边版本
         */
        fun from(src: AdaptiveIconDrawable): AdaptiveIconDrawable =
            runCatching {
                NoStrokeAdaptiveIconDrawable(src.background, src.foreground, src.monochrome)
            }.getOrDefault(src)
    }
}
