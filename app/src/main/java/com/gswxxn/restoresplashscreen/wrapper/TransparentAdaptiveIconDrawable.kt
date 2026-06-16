package com.gswxxn.restoresplashscreen.wrapper

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.gswxxn.restoresplashscreen.hook.utils.ReflectCache

/**
 * 用于 HyperOS 的透明背景 AdaptiveIconDrawable
 */
class TransparentAdaptiveIconDrawable(
    foregroundDrawable: Drawable
) : AdaptiveIconDrawable(Color.TRANSPARENT.toDrawable(), foregroundDrawable) {
    private var mLayersShader: Shader?
        get() = ReflectCache.getField(this, "mLayersShader")
        set(value) {
            ReflectCache.setField(this, "mLayersShader", value)
        }
    private val mCanvas: Canvas
        get() = ReflectCache.getField<Canvas>(this, "mCanvas")!!
    private val mLayersBitmap: Bitmap?
        get() = ReflectCache.getField(this, "mLayersBitmap")
    private val mPaint: Paint
        get() = ReflectCache.getField<Paint>(this, "mPaint")!!
    private val mMaskScaleOnly: Path?
        get() = ReflectCache.getField(this, "mMaskScaleOnly")

    /**
     * 继承修改自 AdaptiveIconDrawable
     * 详见 [AdaptiveIconDrawable.draw]
     */
    override fun draw(canvas: Canvas) {
        if (mLayersBitmap == null) {
            return
        }
        if (mLayersShader == null) {
            // 修改为透明色清空画布
            mCanvas.setBitmap(mLayersBitmap)
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            // 绘制背景图层
            background?.draw(mCanvas)

            // 绘制前景图层
            foreground?.setBounds(0, 0, bounds.width(), bounds.height())
            foreground?.draw(mCanvas)

            // 创建位图着色器
            mLayersShader =
                BitmapShader(mLayersBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            mPaint.setShader(mLayersShader)
        }
        if (mMaskScaleOnly != null) {
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            canvas.drawPath(mMaskScaleOnly!!, mPaint)
            canvas.translate(-bounds.left.toFloat(), -bounds.top.toFloat())
        }
    }
}
