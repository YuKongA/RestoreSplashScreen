package com.gswxxn.restoresplashscreen.wrapper

import android.graphics.drawable.Drawable
import com.gswxxn.restoresplashscreen.hook.utils.getValueFrom
import com.gswxxn.restoresplashscreen.hook.utils.toTyped
import com.highcapable.kavaref.KavaRef.Companion.resolve

/**
 * SplashScreenView.Builder 的包装类
 */
class SplashScreenViewBuilderWrapper private constructor(private val builder: Any) {

    companion object {
        private val instances: MutableMap<Any, SplashScreenViewBuilderWrapper> = mutableMapOf()

        /**
         * 获取给定的 SplashScreenView.Builder 对应的单例实例，如果已经存在则返回现有实例，
         * 如果不存在则创建新的实例并进行缓存。
         *
         * @param builder SplashScreenViewBuilder 对象，用于唯一标识需要的包装实例。
         * @return 对应的 SplashScreenViewBuilderWrapper 实例。
         * @throws IllegalArgumentException 如果传递的 builder 不是有效的 SplashScreenView.Builder 实例
         */
        fun getInstance(builder: Any): SplashScreenViewBuilderWrapper {
            if (builder.javaClass.name != "android.window.SplashScreenView\$Builder") {
                throw IllegalArgumentException("Builder must be of type SplashScreenViewBuilder")
            }

            return instances.getOrPut(builder) {
                SplashScreenViewBuilderWrapper(builder)
            }
        }
    }

    /**
     * Get the rectangle size for the center view.
     */
    fun getIconSize() =
        builder.javaClass.resolve().firstField { name = "mIconSize" }.getValueFrom<Any, Int>(builder)!!

    /**
     * Get the background color for the view.
     */
    fun getBackgroundColor() =
        builder.javaClass.resolve().firstField { name = "mBackgroundColor" }.getValueFrom<Any, Int>(builder)!!

    /**
     * Get the Drawable object to fill the entire view.
     */
    fun getOverlayDrawable() =
        builder.javaClass.resolve().firstField { name = "mOverlayDrawable" }.getValueFrom<Any, Drawable>(builder)

    /**
     * Get the Drawable object to fill the center view.
     */
    fun getCenterViewDrawable() =
        builder.javaClass.resolve().firstField { name = "mIconDrawable" }.getValueFrom<Any, Drawable>(builder)

    /**
     * Get the background color for the icon.
     */
    fun getIconBackground() =
        builder.javaClass.resolve().firstField { name = "mIconBackground" }.getValueFrom<Any, Drawable>(builder)

    /**
     * Get the Drawable object and size for the branding view.
     */
    fun getBrandingDrawable() =
        builder.javaClass.resolve().firstField { name = "mBrandingDrawable" }.getValueFrom<Any, Drawable>(builder)

    /**
     * Get whether this view can be copied and transferred to the client if the view is
     * an empty style splash screen.
     */
    fun getAllowHandleSolidColor() =
        builder.javaClass.resolve().firstField { name = "mAllowHandleSolidColor" }.getValueFrom<Any, Boolean>(builder)!!

    /**
     * Set the rectangle size for the center view.
     */
    fun setIconSize(iconSize: Int) =
        builder.javaClass.resolve().firstMethod { name = "setIconSize" }.toTyped<Any>().invoke(builder, iconSize)

    /**
     * Set the background color for the view.
     */
    fun setBackgroundColor(backgroundColor: Int) =
        builder.javaClass.resolve().firstMethod { name = "setBackgroundColor" }.toTyped<Any>().invoke(builder, backgroundColor)

    /**
     * Set the Drawable object to fill the entire view.
     */
    fun setOverlayDrawable(drawable: Drawable?) =
        builder.javaClass.resolve().firstMethod { name = "setOverlayDrawable" }.toTyped<Any>().invoke(builder, drawable)

    /**
     * Set the Drawable object to fill the center view.
     */
    fun setCenterViewDrawable(drawable: Drawable?) =
        builder.javaClass.resolve().firstMethod { name = "setCenterViewDrawable" }.toTyped<Any>().invoke(builder, drawable)

    /**
     * Set the background color for the icon.
     */
    fun setIconBackground(iconBackground: Drawable) =
        builder.javaClass.resolve().firstMethod { name = "setIconBackground" }.toTyped<Any>().invoke(builder, iconBackground)

    /**
     * Set the Drawable object and size for the branding view.
     */
    fun setBrandingDrawable(branding: Drawable?, width: Int, height: Int) =
        builder.javaClass.resolve().firstMethod { name = "setBrandingDrawable" }.toTyped<Any>().invoke(builder, branding, width, height)

    /**
     * Sets whether this view can be copied and transferred to the client if the view is
     * an empty style splash screen.
     */
    fun setAllowHandleSolidColor(allowHandleSolidColor: Boolean) =
        builder.javaClass.resolve().firstMethod { name = "setAllowHandleSolidColor" }.toTyped<Any>().invoke(builder, allowHandleSolidColor)

}
