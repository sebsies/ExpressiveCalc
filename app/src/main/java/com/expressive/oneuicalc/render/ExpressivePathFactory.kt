package com.expressive.oneuicalc.render

import android.graphics.Path
import android.graphics.RectF
import com.expressive.oneuicalc.model.ExpressiveShape
import com.expressive.oneuicalc.model.MorphMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

object ExpressivePathFactory {
    private const val SAMPLE_COUNT = 128

    fun morphTargetFor(shape: ExpressiveShape, mode: MorphMode): ExpressiveShape {
        return when (mode) {
            MorphMode.STATIC -> shape
            MorphMode.SQUISH_SHARPEN -> when (shape) {
                ExpressiveShape.SOFT_SQUARE -> ExpressiveShape.SQUIRCLE
                else -> ExpressiveShape.SOFT_SQUARE
            }
            MorphMode.EXPRESSIVE_MORPH,
            MorphMode.ROTATE_AND_MORPH -> when (shape) {
                ExpressiveShape.PILL -> ExpressiveShape.SOFT_SQUARE
                ExpressiveShape.SQUIRCLE -> ExpressiveShape.PILL
                ExpressiveShape.SOFT_SQUARE -> ExpressiveShape.PILL
                ExpressiveShape.CUT_CORNER -> ExpressiveShape.SQUIRCLE
                ExpressiveShape.CLOVER -> ExpressiveShape.PILL
                ExpressiveShape.FLOWER -> ExpressiveShape.PILL
                ExpressiveShape.COOKIE -> ExpressiveShape.PILL
                ExpressiveShape.DIAMOND -> ExpressiveShape.SQUIRCLE
                ExpressiveShape.ARCH -> ExpressiveShape.PILL
                ExpressiveShape.SLANT -> ExpressiveShape.SQUIRCLE
            }
        }
    }

    fun buildPath(
        outPath: Path,
        bounds: RectF,
        restingShape: ExpressiveShape,
        morphMode: MorphMode,
        morphProgress: Float
    ) {
        outPath.reset()
        val w = bounds.width()
        val h = bounds.height()
        if (w <= 1f || h <= 1f) return

        val clamped = morphProgress.coerceIn(0f, 1.25f)
        val targetShape = morphTargetFor(restingShape, morphMode)

        // Fast exact paths when not morphing and shape has an exact native primitive
        if (clamped <= 0.001f || morphMode == MorphMode.STATIC || targetShape == restingShape) {
            when (restingShape) {
                ExpressiveShape.PILL -> {
                    val r = min(w, h) / 2f
                    outPath.addRoundRect(bounds, r, r, Path.Direction.CW)
                    return
                }
                ExpressiveShape.SOFT_SQUARE -> {
                    val r = min(w, h) * 0.25f
                    outPath.addRoundRect(bounds, r, r, Path.Direction.CW)
                    return
                }
                ExpressiveShape.ARCH -> {
                    val rTop = min(w, h) * 0.5f
                    val rBot = min(w, h) * 0.16f
                    val radii = floatArrayOf(rTop, rTop, rTop, rTop, rBot, rBot, rBot, rBot)
                    outPath.addRoundRect(bounds, radii, Path.Direction.CW)
                    return
                }
                else -> Unit
            }
        }

        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val rx = w / 2f
        val ry = h / 2f

        val ptsA = FloatArray(SAMPLE_COUNT * 2)
        val ptsB = FloatArray(SAMPLE_COUNT * 2)
        sampleShape(restingShape, w, h, ptsA)
        if (clamped > 0.001f && targetShape != restingShape && morphMode != MorphMode.STATIC) {
            sampleShape(targetShape, w, h, ptsB)
        } else {
            System.arraycopy(ptsA, 0, ptsB, 0, ptsA.size)
        }

        val t = if (morphMode == MorphMode.STATIC) 0f else clamped
        for (i in 0 until SAMPLE_COUNT) {
            val idx = i * 2
            val nx = ptsA[idx] + (ptsB[idx] - ptsA[idx]) * t
            val ny = ptsA[idx + 1] + (ptsB[idx + 1] - ptsA[idx + 1]) * t
            val px = cx + nx * rx
            val py = cy + ny * ry
            if (i == 0) {
                outPath.moveTo(px, py)
            } else {
                outPath.lineTo(px, py)
            }
        }
        outPath.close()
    }

    /**
     * Samples a normalized [-1, 1] x [-1, 1] boundary at SAMPLE_COUNT uniform angles.
     */
    private fun sampleShape(shape: ExpressiveShape, width: Float, height: Float, out: FloatArray) {
        val aspect = width / max(1f, height)
        for (i in 0 until SAMPLE_COUNT) {
            val theta = (2.0 * PI * i) / SAMPLE_COUNT
            val cosT = cos(theta).toFloat()
            val sinT = sin(theta).toFloat()

            var nx = 0f
            var ny = 0f

            when (shape) {
                ExpressiveShape.PILL -> {
                    val rNorm = 0.5f * min(width, height)
                    val p = intersectRoundedBox(cosT, sinT, width / 2f, height / 2f, rNorm)
                    nx = p.first
                    ny = p.second
                }
                ExpressiveShape.SOFT_SQUARE -> {
                    val rNorm = 0.25f * min(width, height)
                    val p = intersectRoundedBox(cosT, sinT, width / 2f, height / 2f, rNorm)
                    nx = p.first
                    ny = p.second
                }
                ExpressiveShape.SQUIRCLE -> {
                    // Continuous superellipse (n = 3.5)
                    val p = 3.5
                    val denom = (abs(cosT).toDouble().pow(p) + abs(sinT).toDouble().pow(p)).pow(1.0 / p)
                    val rad = (1.0 / max(1e-6, denom)).toFloat()
                    nx = cosT * rad
                    ny = sinT * rad
                }
                ExpressiveShape.DIAMOND -> {
                    // Puffy diamond superellipse (n = 1.48)
                    val p = 1.48
                    val denom = (abs(cosT).toDouble().pow(p) + abs(sinT).toDouble().pow(p)).pow(1.0 / p)
                    val rad = (1.0 / max(1e-6, denom)).toFloat()
                    nx = cosT * rad
                    ny = sinT * rad
                }
                ExpressiveShape.CUT_CORNER -> {
                    // Chamfered octagon / gem
                    val cut = 0.30f * min(width, height)
                    val hx = width / 2f
                    val hy = height / 2f
                    val dx = abs(cosT)
                    val dy = abs(sinT)
                    // Distance to horizontal/vertical sides vs 45-deg cut plane (x + y = hx + hy - cut)
                    val tx = if (dx > 1e-5f) hx / dx else Float.MAX_VALUE
                    val ty = if (dy > 1e-5f) hy / dy else Float.MAX_VALUE
                    val tc = if (dx + dy > 1e-5f) (hx + hy - cut) / (dx + dy) else Float.MAX_VALUE
                    val tMin = min(tx, min(ty, tc))
                    nx = (cosT * tMin) / hx
                    ny = (sinT * tMin) / hy
                }
                ExpressiveShape.CLOVER -> {
                    // Iconic 4-leaf M3 Expressive quatrefoil (lobes at diagonals)
                    val r = (0.86f - 0.14f * cos(4.0 * theta).toFloat())
                    val boxScale = if (aspect > 1.25f) 1.0f else 1.0f
                    nx = cosT * r * boxScale
                    ny = sinT * r
                }
                ExpressiveShape.FLOWER -> {
                    // 8-petal rounded blossom
                    val r = 0.89f + 0.11f * cos(8.0 * theta).toFloat()
                    nx = cosT * r
                    ny = sinT * r
                }
                ExpressiveShape.COOKIE -> {
                    // 12-wave M3 Expressive cookie / starburst
                    val r = 0.92f + 0.08f * cos(12.0 * theta).toFloat()
                    nx = cosT * r
                    ny = sinT * r
                }
                ExpressiveShape.ARCH -> {
                    val rTop = 0.50f * min(width, height)
                    val rBot = 0.16f * min(width, height)
                    val rCorner = if (sinT < 0f) rTop else rBot
                    val p = intersectRoundedBox(cosT, sinT, width / 2f, height / 2f, rCorner)
                    nx = p.first
                    ny = p.second
                }
                ExpressiveShape.SLANT -> {
                    val shear = 0.16f
                    val rNorm = 0.22f * min(width, height)
                    val box = intersectRoundedBox(cosT, sinT, (width / 2f) * (1f - shear), height / 2f, rNorm)
                    nx = (box.first * (1f - shear)) - box.second * shear
                    ny = box.second
                }
            }
            out[i * 2] = nx
            out[i * 2 + 1] = ny
        }
    }

    private fun intersectRoundedBox(
        cosT: Float,
        sinT: Float,
        hx: Float,
        hy: Float,
        radius: Float
    ): Pair<Float, Float> {
        val r = min(radius, min(hx, hy))
        val ix = max(0f, hx - r)
        val iy = max(0f, hy - r)
        val ax = abs(cosT)
        val ay = abs(sinT)

        // First test intersection with the rounded-box outer rect, then project corner if inside corner zone
        val tx = if (ax > 1e-5f) hx / ax else Float.MAX_VALUE
        val ty = if (ay > 1e-5f) hy / ay else Float.MAX_VALUE
        var t = min(tx, ty)
        var px = ax * t
        var py = ay * t

        if (px > ix && py > iy && r > 0.001f) {
            // Intersect ray (ax, ay)*t with circle centered at (ix, iy) of radius r
            // (ax*t - ix)^2 + (ay*t - iy)^2 = r^2
            val b = ax * ix + ay * iy
            val c = ix * ix + iy * iy - r * r
            val disc = max(0f, b * b - c)
            t = b + kotlin.math.sqrt(disc)
            px = ax * t
            py = ay * t
        }
        val sx = if (cosT < 0f) -px else px
        val sy = if (sinT < 0f) -py else py
        return Pair(sx / max(1f, hx), sy / max(1f, hy))
    }
}
