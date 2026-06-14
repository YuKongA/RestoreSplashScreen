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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.gswxxn.restoresplashscreen.hook.utils.getValueFrom
import com.gswxxn.restoresplashscreen.hook.utils.setValueTo
import com.highcapable.kavaref.KavaRef.Companion.resolve

/**
 * 透明背景的 AdaptiveIconDrawable
 */
class TransparentAdaptiveIconDrawable(
    foregroundDrawable: Drawable
) : AdaptiveIconDrawable(ColorDrawable(Color.TRANSPARENT), foregroundDrawable) {
    private var mLayersShader: Shader?
        get() = this.javaClass.resolve().firstField {
            name = "mLayersShader"
            superclass()
        }.getValueFrom(this)
        set(value) {
            this.javaClass.resolve().firstField {
                name = "mLayersShader"
                superclass()
            }.setValueTo(this, value)
        }
    private val mCanvas: Canvas
        get() = this.javaClass.resolve().firstField {
            name = "mCanvas"
            superclass()
        }.getValueFrom<TransparentAdaptiveIconDrawable, Canvas>(this)!!
    private val mLayersBitmap: Bitmap?
        get() = this.javaClass.resolve().firstField {
            name = "mLayersBitmap"
            superclass()
        }.getValueFrom(this)
    private val mPaint: Paint
        get() = this.javaClass.resolve().firstField {
            name = "mPaint"
            superclass()
        }.getValueFrom<TransparentAdaptiveIconDrawable, Paint>(this)!!
    private val mMaskScaleOnly: Path?
        get() = this.javaClass.resolve().firstField {
            name = "mMaskScaleOnly"
            superclass()
        }.getValueFrom(this)

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
