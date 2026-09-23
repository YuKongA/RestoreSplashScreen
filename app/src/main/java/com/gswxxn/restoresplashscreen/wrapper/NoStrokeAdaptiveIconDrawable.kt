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
    monochrome: Drawable?,
    private val getBorderMode: Method?,
    private val setBorderMode: Method?,
    private val getIsIconStroke: Method?,
    private val setIsIconStroke: Method?
) : AdaptiveIconDrawable(background, foreground, monochrome) {

    override fun draw(canvas: Canvas) {
        val borderModeSetter = setBorderMode
        val previousMode = runCatching { getBorderMode?.invoke(null) as? Int }.getOrNull()
        val previousStroke = runCatching { getIsIconStroke?.invoke(null) as? Boolean }.getOrNull()
        if (previousMode == null && previousStroke == null) {
            super.draw(canvas)
            return
        }
        try {
            if (getBorderMode != null && borderModeSetter != null) {
                runCatching { borderModeSetter.invoke(null, 0) }
            } else {
                runCatching { setIsIconStroke?.invoke(null, false) }
            }
            super.draw(canvas)
        } finally {
            when {
                previousMode != null && borderModeSetter != null ->
                    runCatching { borderModeSetter.invoke(null, previousMode) }
                previousStroke != null -> runCatching { setIsIconStroke?.invoke(null, previousStroke) }
            }
        }
    }

    companion object {
        /**
         * 用 [src] 的前景/背景/单色图层重建一个无描边版本
         */
        fun from(src: AdaptiveIconDrawable, classLoader: ClassLoader): AdaptiveIconDrawable =
            runCatching {
                val iconCustomizerClass = "miui.content.res.IconCustomizer".toClassOrNull(loader = classLoader)
                NoStrokeAdaptiveIconDrawable(
                    src.background,
                    src.foreground,
                    src.monochrome,
                    iconCustomizerClass?.getDeclaredMethod("getIconBorderMode")?.apply { makeAccessible() },
                    iconCustomizerClass?.getDeclaredMethod("setIconBorderMode", classOf<Int>())?.apply { makeAccessible() },
                    iconCustomizerClass?.getDeclaredMethod("getIsIconStroke")?.apply { makeAccessible() },
                    iconCustomizerClass?.getDeclaredMethod("setIsIconStroke", classOf<Boolean>())?.apply { makeAccessible() }
                )
            }.getOrDefault(src)
    }
}
