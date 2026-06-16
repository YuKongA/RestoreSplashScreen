package com.gswxxn.restoresplashscreen.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.palette.graphics.Palette

/**
 * 图形工具类
 */
object GraphicUtils {
    /**
     * Drawable 图标转 Bitmap
     *
     * @param drawable 待转换的 Drawable 图标
     * @param size 生成此大小的 Bitmap
     * @return [Bitmap]
     */
    fun drawable2Bitmap(drawable: Drawable, size: Int): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        canvas.setBitmap(null)
        return bitmap
    }

    /** 取色失败时的回退色 */
    private val FALLBACK_LIGHT_COLOR = "#F5F5F5".toColorInt()
    private val FALLBACK_DARK_COLOR = "#1C2833".toColorInt()

    /**
     * 根据 Bitmap 获取背景颜色
     *
     * @param bitmap 从中获取颜色的图片
     * @param isLight 是否为浅色模式
     * @return [Int]
     */
    fun getBgColor(bitmap: Bitmap, isLight: Boolean): Int {
        val hsv = FloatArray(3)

        val color = Palette.from(bitmap)
            .maximumColorCount(8).generate()
            .getDominantColor(if (isLight) FALLBACK_LIGHT_COLOR else FALLBACK_DARK_COLOR)
        Color.colorToHSV(color, hsv)
        if (isLight) {
            hsv[1] = hsv[1] - 0.4f // 减小饱和度
            hsv[2] = hsv[2] + 0.2f // 增大明度
        } else {
            hsv[1] = hsv[1] - 0.2f // 减小饱和度
            hsv[2] = hsv[2] - 0.7f // 减小明度
        }
        return Color.HSVToColor(hsv)
    }

    /**
     * 缩小图标, 以避免后续使用 setRenderEffect 模糊图标时出现毛边问题
     *
     * @param context 上下文，用于获取资源
     * @param drawable 需要创建阴影的 Drawable
     * @param oriIconSize 原始图标大小
     * @param blurIconSize 待模糊图标大小
     * @param cornerRadius 模糊图标圆角大小
     * @return 待模糊图标 Drawable，如果输入的 Drawable 或者 Context 为空则返回 null
     */
    fun createShadowedIcon(context: Context?, drawable: Drawable?, oriIconSize: Int, blurIconSize: Int, cornerRadius: Float): Drawable? {
        if (drawable == null || context == null) {
            return null
        }

        // 计算缩放比例
        val originalSize = drawable.intrinsicWidth
        val ratio = (oriIconSize.toDouble() / originalSize).coerceAtMost(1.0).toFloat()
        val scaledSize = (originalSize * ratio).toInt()
        val shadowSize = blurIconSize / 4
        val shadowBitmap = createBitmap(scaledSize + shadowSize, scaledSize + shadowSize)
        val canvas = Canvas(shadowBitmap)
        val offset = (shadowSize / 2).toFloat()

        val checkpoint = canvas.saveLayerAlpha(0f, 0f, shadowBitmap.width.toFloat(), shadowBitmap.height.toFloat(), 90)
        canvas.translate(offset, offset)
        canvas.clipPath(
            Path().apply {
                addRoundRect(
                    RectF(0f, 0f, scaledSize.toFloat(), scaledSize.toFloat()),
                    cornerRadius, cornerRadius, Path.Direction.CW
                )
            }
        )
        drawable.setBounds(0, 0, scaledSize, scaledSize)
        drawable.draw(canvas)
        canvas.restoreToCount(checkpoint)

        return shadowBitmap.toDrawable(context.resources)
    }

    /**
     * 将给定的 Drawable 转换为一个新的正方形 Drawable，其空白区域用透明色填充。
     *
     * @param drawable 需要转换的 Drawable。如果为 null，则函数返回 null。
     * @param resources 应用程序的资源，用于将 Bitmap 转换回 Drawable。
     * @return 返回一个新的正方形 Drawable，其空白区域用透明色填充。如果传入的 Drawable 为 null，则返回 null。
     */
    fun convertToSquareDrawable(drawable: Drawable?, resources: Resources): Drawable? {
        // 如果传入的 Drawable 是 null，直接返回 null
        drawable ?: return null

        // 将 Drawable 转换为 Bitmap
        val originalBitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            // 如果不是 BitmapDrawable，创建一个新的 Bitmap 并绘制原始 Drawable
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight).apply {
                val canvas = Canvas(this)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }

        // 计算新的正方形 Bitmap 的尺寸
        val size = maxOf(originalBitmap.width, originalBitmap.height)

        // 创建一个新的正方形 Bitmap
        val squareBitmap = createBitmap(size, size)

        // 在新的 Bitmap 上创建一个 Canvas 用于绘图
        val canvas = Canvas(squareBitmap)

        // 计算原始 Bitmap 在新 Bitmap 上的位置
        val x = (size - originalBitmap.width) / 2
        val y = (size - originalBitmap.height) / 2

        // 将原始 Bitmap 绘制到新的 Canvas 上
        canvas.drawBitmap(originalBitmap, x.toFloat(), y.toFloat(), null)

        // 将新的正方形 Bitmap 转换回 Drawable 并返回
        return squareBitmap.toDrawable(resources)
    }

}