package com.expressive.oneuicalc.ui.components

import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import com.expressive.oneuicalc.model.ExpressiveShape
import com.expressive.oneuicalc.model.MorphMode
import com.expressive.oneuicalc.render.ExpressivePathFactory

@Composable
fun ShapeBadge(
    shape: ExpressiveShape,
    fillColor: Color,
    strokeColor: Color? = null,
    morphProgress: Float = 0f,
    morphMode: MorphMode = MorphMode.EXPRESSIVE_MORPH,
    modifier: Modifier = Modifier
) {
    val androidPath = remember { Path() }
    val bounds = remember { RectF() }

    Canvas(modifier = modifier) {
        val pad = 2f
        bounds.set(pad, pad, size.width - pad, size.height - pad)
        ExpressivePathFactory.buildPath(
            outPath = androidPath,
            bounds = bounds,
            restingShape = shape,
            morphMode = morphMode,
            morphProgress = morphProgress
        )
        val composePath = androidPath.asComposePath()
        drawPath(path = composePath, color = fillColor)
        if (strokeColor != null) {
            drawPath(path = composePath, color = strokeColor, style = Stroke(width = 2.5f))
        }
    }
}
