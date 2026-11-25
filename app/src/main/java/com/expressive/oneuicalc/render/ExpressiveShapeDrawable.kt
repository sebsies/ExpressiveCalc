package com.expressive.oneuicalc.render

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import com.expressive.oneuicalc.model.ExpressiveShape
import com.expressive.oneuicalc.model.MorphMode

class ExpressiveShapeDrawable(
    private val shape: ExpressiveShape,
    private val fillColor: Int,
    private val strokeColor: Int = Color.TRANSPARENT,
    private val strokeWidthPx: Float = 0f,
    private val insetPx: Float = 0f,
    private val adaptWideChrome: Boolean = true
) : Drawable() {

    private val path = Path()
    private val boundsF = RectF()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = strokeColor
        strokeWidth = strokeWidthPx
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        rebuildPath(bounds)
    }

    private fun rebuildPath(bounds: Rect) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 1f || h <= 1f) {
            path.reset()
            return
        }
        val effectiveInset = insetPx.coerceAtMost(minOf(w, h) * 0.25f)
        boundsF.set(
            bounds.left + effectiveInset,
            bounds.top + effectiveInset,
            (bounds.right - effectiveInset).coerceAtLeast(bounds.left + effectiveInset + 2f),
            (bounds.bottom - effectiveInset).coerceAtLeast(bounds.top + effectiveInset + 2f)
        )
        val effectiveShape = if (adaptWideChrome && w > h * 1.45f) {
            wideChromeShape(shape)
        } else {
            shape
        }
        ExpressivePathFactory.buildPath(path, boundsF, effectiveShape, MorphMode.STATIC, 0f)
    }

    override fun draw(canvas: Canvas) {
        if (path.isEmpty && !bounds.isEmpty) {
            rebuildPath(bounds)
        }
        if (path.isEmpty) return
        if (Color.alpha(fillPaint.color) > 0) {
            canvas.drawPath(path, fillPaint)
        }
        if (strokeWidthPx > 0f && Color.alpha(strokePaint.color) > 0) {
            canvas.drawPath(path, strokePaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = (alpha * Color.alpha(fillColor)) / 255
        strokePaint.alpha = (alpha * Color.alpha(strokeColor)) / 255
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getPadding(padding: Rect): Boolean = false

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        fun wideChromeShape(shape: ExpressiveShape): ExpressiveShape = when (shape) {
            ExpressiveShape.PILL,
            ExpressiveShape.SQUIRCLE,
            ExpressiveShape.SOFT_SQUARE,
            ExpressiveShape.CUT_CORNER,
            ExpressiveShape.ARCH -> shape
            ExpressiveShape.CLOVER,
            ExpressiveShape.FLOWER,
            ExpressiveShape.COOKIE,
            ExpressiveShape.DIAMOND,
            ExpressiveShape.SLANT -> ExpressiveShape.SOFT_SQUARE
        }

        fun ripple(
            shape: ExpressiveShape,
            fillColor: Int,
            rippleColor: Int,
            strokeColor: Int = Color.TRANSPARENT,
            strokeWidthPx: Float = 0f,
            insetPx: Float = 0f,
            adaptWideChrome: Boolean = true
        ): RippleDrawable {
            val content = if (Color.alpha(fillColor) == 0 && strokeWidthPx <= 0f) {
                null
            } else {
                ExpressiveShapeDrawable(
                    shape = shape,
                    fillColor = fillColor,
                    strokeColor = strokeColor,
                    strokeWidthPx = strokeWidthPx,
                    insetPx = insetPx,
                    adaptWideChrome = adaptWideChrome
                )
            }
            val mask = ExpressiveShapeDrawable(
                shape = shape,
                fillColor = Color.WHITE,
                insetPx = insetPx,
                adaptWideChrome = adaptWideChrome
            )
            return RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask)
        }
    }
}
