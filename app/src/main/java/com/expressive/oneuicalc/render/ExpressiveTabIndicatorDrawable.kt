package com.expressive.oneuicalc.render

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import com.expressive.oneuicalc.model.ExpressiveShape
import com.expressive.oneuicalc.model.MorphMode
import com.expressive.oneuicalc.model.PaletteTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class ExpressiveTabIndicatorDrawable(
    private val tabStrip: ViewGroup,
    private val titleViewId: Int
) : Drawable() {

    private var currentPos: Float = 0f
    private var targetPos: Float = 0f
    private var initialized: Boolean = false
    private var animator: ValueAnimator? = null

    private var restingShape: ExpressiveShape = ExpressiveShape.PILL
    private var morphMode: MorphMode = MorphMode.EXPRESSIVE_MORPH
    private var tokens: PaletteTokens? = null
    private var showOutline: Boolean = false
    private var outlineWidthDp: Float = 0f
    private var showGlow: Boolean = true

    private val indicatorRect = RectF()
    private val path = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun updateStyle(
        shape: ExpressiveShape,
        morphMode: MorphMode,
        tokens: PaletteTokens,
        showOutline: Boolean,
        outlineWidthDp: Float,
        showGlow: Boolean
    ) {
        this.restingShape = ExpressiveShapeDrawable.wideChromeShape(shape)
        this.morphMode = morphMode
        this.tokens = tokens
        this.showOutline = showOutline
        this.outlineWidthDp = outlineWidthDp
        this.showGlow = showGlow
        invalidateSelf()
    }

    fun snapTo(index: Int) {
        val clamped = clampIndex(index).toFloat()
        animator?.cancel()
        currentPos = clamped
        targetPos = clamped
        initialized = true
        syncTitleColors()
        tabStrip.postInvalidateOnAnimation()
    }

    fun animateTo(index: Int) {
        val clamped = clampIndex(index).toFloat()
        if (!initialized) {
            snapTo(index)
            return
        }
        if (abs(targetPos - clamped) < 0.001f && animator?.isRunning == true) return
        if (abs(currentPos - clamped) < 0.001f) {
            targetPos = clamped
            syncTitleColors()
            return
        }
        targetPos = clamped
        animator?.cancel()
        val start = currentPos
        animator = ValueAnimator.ofFloat(start, clamped).apply {
            duration = 340L
            interpolator = OvershootInterpolator(0.85f)
            addUpdateListener { va ->
                currentPos = va.animatedValue as Float
                syncTitleColors()
                tabStrip.postInvalidateOnAnimation()
            }
            start()
        }
    }

    fun onScrollPosition(position: Int, offset: Float) {
        val raw = (position + offset).coerceIn(0f, max(0, tabStrip.childCount - 1).toFloat())
        if (!initialized) {
            currentPos = raw
            targetPos = raw
            initialized = true
            syncTitleColors()
            tabStrip.postInvalidateOnAnimation()
            return
        }
        // If TabLayout jumped straight to a distant index without intermediate scroll events, animate smoothly
        if (offset == 0f && abs(raw - currentPos) > 0.45f) {
            animateTo(position)
            return
        }
        animator?.cancel()
        currentPos = raw
        targetPos = raw
        syncTitleColors()
        tabStrip.postInvalidateOnAnimation()
    }

    private fun clampIndex(index: Int): Int {
        val count = tabStrip.childCount
        return if (count <= 0) 0 else index.coerceIn(0, count - 1)
    }

    private fun syncTitleColors() {
        val t = tokens ?: return
        val activeColor = t.equalsText
        val inactiveColor = Color.argb(
            185,
            Color.red(t.formulaText),
            Color.green(t.formulaText),
            Color.blue(t.formulaText)
        )
        for (i in 0 until tabStrip.childCount) {
            val child = tabStrip.getChildAt(i) ?: continue
            val tv = child.findViewById<TextView>(titleViewId) ?: continue
            val proximity = (1f - abs(i - currentPos)).coerceIn(0f, 1f)
            tv.setTextColor(lerpColor(inactiveColor, activeColor, proximity))
        }
    }

    private fun measureTabPillRect(index: Int, outRect: RectF): Boolean {
        val child = tabStrip.getChildAt(index) ?: return false
        val titleTv = child.findViewById<View>(titleViewId)
        val density = tabStrip.resources.displayMetrics.density
        if (titleTv != null && titleTv.width > 0 && titleTv.height > 0) {
            var left = titleTv.left.toFloat()
            var top = titleTv.top.toFloat()
            var p = titleTv.parent
            while (p is View && p !== tabStrip) {
                left += p.left
                top += p.top
                p = p.parent
            }
            val right = left + titleTv.width
            val bottom = top + titleTv.height
            outRect.set(left, top, right, bottom)
        } else {
            val padH = 6f * density
            val padV = 4f * density
            outRect.set(
                child.left + padH,
                child.top + padV,
                child.right - padH,
                child.bottom - padV
            )
        }
        return outRect.width() > 2f && outRect.height() > 2f
    }

    override fun draw(canvas: Canvas) {
        val t = tokens ?: return
        val count = tabStrip.childCount
        if (count <= 0) return

        val clamped = currentPos.coerceIn(0f, (count - 1).toFloat())
        val idxA = floor(clamped).toInt().coerceIn(0, count - 1)
        val idxB = ceil(clamped).toInt().coerceIn(0, count - 1)
        val frac = (clamped - idxA).coerceIn(0f, 1f)

        val rectA = RectF()
        val rectB = RectF()
        if (!measureTabPillRect(idxA, rectA)) return
        if (idxB != idxA && measureTabPillRect(idxB, rectB)) {
            val baseLeft = rectA.left + (rectB.left - rectA.left) * frac
            val baseTop = rectA.top + (rectB.top - rectA.top) * frac
            val baseRight = rectA.right + (rectB.right - rectA.right) * frac
            val baseBottom = rectA.bottom + (rectB.bottom - rectA.bottom) * frac

            // Material 3 Expressive liquid stretch/squish during motion
            val travelWave = sin(PI * frac).toFloat()
            val density = tabStrip.resources.displayMetrics.density
            val stretchX = 6f * density * travelWave
            val squishY = 2.5f * density * travelWave
            indicatorRect.set(
                baseLeft - stretchX,
                baseTop + squishY,
                baseRight + stretchX,
                baseBottom - squishY
            )
        } else {
            indicatorRect.set(rectA)
        }

        if (indicatorRect.width() <= 2f || indicatorRect.height() <= 2f) return

        val travelWave = sin(PI * frac).toFloat()
        val morphProgress = if (morphMode == MorphMode.STATIC) 0f else travelWave * 0.85f
        ExpressivePathFactory.buildPath(path, indicatorRect, restingShape, morphMode, morphProgress)

        fillPaint.color = t.equalsBg
        canvas.drawPath(path, fillPaint)

        if (showGlow && travelWave > 0.02f) {
            val glowAlpha = (72f * travelWave).roundToInt().coerceIn(0, 110)
            glowPaint.shader = LinearGradient(
                indicatorRect.left,
                indicatorRect.top,
                indicatorRect.right,
                indicatorRect.bottom,
                withAlpha(t.glowStart, glowAlpha),
                withAlpha(t.glowEnd, glowAlpha),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, glowPaint)
            glowPaint.shader = null
        }

        if (showOutline && outlineWidthDp > 0f) {
            val density = tabStrip.resources.displayMetrics.density
            strokePaint.strokeWidth = outlineWidthDp * density
            strokePaint.color = t.outlineColor
            canvas.drawPath(path, strokePaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun lerpColor(start: Int, end: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        val a = (Color.alpha(start) + (Color.alpha(end) - Color.alpha(start)) * f).roundToInt()
        val r = (Color.red(start) + (Color.red(end) - Color.red(start)) * f).roundToInt()
        val g = (Color.green(start) + (Color.green(end) - Color.green(start)) * f).roundToInt()
        val b = (Color.blue(start) + (Color.blue(end) - Color.blue(start)) * f).roundToInt()
        return Color.argb(a, r, g, b)
    }
}
