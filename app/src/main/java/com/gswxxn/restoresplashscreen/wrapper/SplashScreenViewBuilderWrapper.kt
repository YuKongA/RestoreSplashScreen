package com.gswxxn.restoresplashscreen.wrapper

import android.graphics.drawable.Drawable
import com.gswxxn.restoresplashscreen.hook.utils.ReflectCache

/**
 * SplashScreenView.Builder 的包装类
 */
class SplashScreenViewBuilderWrapper private constructor(private val builder: Any) {

    companion object {
        private val instances: MutableMap<Any, SplashScreenViewBuilderWrapper> = mutableMapOf()

        /**
         * 获取给定 SplashScreenView.Builder 对应的包装实例 (按 builder 缓存)
         *
         * @throws IllegalArgumentException 传入的 builder 不是 SplashScreenView.Builder 实例时
         */
        fun getInstance(builder: Any): SplashScreenViewBuilderWrapper {
            if (builder.javaClass.name != $$"android.window.SplashScreenView$Builder") {
                throw IllegalArgumentException("Builder must be of type SplashScreenViewBuilder")
            }

            return instances.getOrPut(builder) {
                SplashScreenViewBuilderWrapper(builder)
            }
        }
    }

    /** 设置整体背景颜色 */
    fun setBackgroundColor(backgroundColor: Int) {
        ReflectCache.invokeMethod<Any>(builder, "setBackgroundColor", backgroundColor)
    }

    /** 设置 branding view 的 Drawable 与尺寸 */
    fun setBrandingDrawable(branding: Drawable?, width: Int, height: Int) {
        ReflectCache.invokeMethod<Any>(builder, "setBrandingDrawable", branding, width, height)
    }
}
