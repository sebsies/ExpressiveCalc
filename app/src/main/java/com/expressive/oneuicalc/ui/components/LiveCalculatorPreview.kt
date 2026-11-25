package com.expressive.oneuicalc.ui.components

import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expressive.oneuicalc.model.ButtonRole
import com.expressive.oneuicalc.model.ModuleConfig
import com.expressive.oneuicalc.model.MorphMode
import com.expressive.oneuicalc.model.PaletteTokens
import com.expressive.oneuicalc.render.ExpressivePathFactory
import com.expressive.oneuicalc.render.PaletteResolver
import kotlinx.coroutines.launch

private data class PreviewKey(
    val label: String,
    val role: ButtonRole
)

@Composable
fun LiveCalculatorPreview(
    config: ModuleConfig,
    showScientificRow: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = remember(config) { PaletteResolver.resolve(context, config) }
    var lastPressed by remember { mutableStateOf("Tap any key to test morph!") }

    val sciRow = remember {
        listOf(
            PreviewKey("sin", ButtonRole.SCIENTIFIC),
            PreviewKey("cos", ButtonRole.SCIENTIFIC),
            PreviewKey("√", ButtonRole.SCIENTIFIC),
            PreviewKey("π", ButtonRole.SCIENTIFIC)
        )
    }

    val rows = remember {
        listOf(
            listOf(
                PreviewKey("C", ButtonRole.UTILITY),
                PreviewKey("( )", ButtonRole.UTILITY),
                PreviewKey("%", ButtonRole.UTILITY),
                PreviewKey("÷", ButtonRole.OPERATOR)
            ),
            listOf(
                PreviewKey("7", ButtonRole.NUMBER),
                PreviewKey("8", ButtonRole.NUMBER),
                PreviewKey("9", ButtonRole.NUMBER),
                PreviewKey("×", ButtonRole.OPERATOR)
            ),
            listOf(
                PreviewKey("4", ButtonRole.NUMBER),
                PreviewKey("5", ButtonRole.NUMBER),
                PreviewKey("6", ButtonRole.NUMBER),
                PreviewKey("−", ButtonRole.OPERATOR)
            ),
            listOf(
                PreviewKey("1", ButtonRole.NUMBER),
                PreviewKey("2", ButtonRole.NUMBER),
                PreviewKey("3", ButtonRole.NUMBER),
                PreviewKey("+", ButtonRole.OPERATOR)
            ),
            listOf(
                PreviewKey("⌫", ButtonRole.UTILITY),
                PreviewKey("0", ButtonRole.NUMBER),
                PreviewKey(".", ButtonRole.NUMBER),
                PreviewKey("=", ButtonRole.EQUALS)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(tokens.background))
            .border(1.dp, Color(tokens.outlineColor).copy(alpha = 0.45f), RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Formula & Live Result preview header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lastPressed,
                color = Color(tokens.formulaText).copy(alpha = 0.55f),
                fontSize = 11.sp
            )
            Text(
                text = "One UI 9 Live Preview",
                color = Color(tokens.symbolAccent),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(tokens.formulaText))) { append("128 ") }
                withStyle(SpanStyle(color = Color(tokens.symbolAccent))) { append("×") }
                withStyle(SpanStyle(color = Color(tokens.formulaText))) { append(" 3.5") }
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "448",
            color = Color(tokens.resultText),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        if (showScientificRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sciRow.forEach { key ->
                    PreviewExpressiveKey(
                        key = key,
                        config = config,
                        tokens = tokens,
                        onPressed = { lastPressed = "${key.role.displayName} • ${config.shapeFor(key.role).displayName}" },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        PreviewExpressiveKey(
                            key = key,
                            config = config,
                            tokens = tokens,
                            onPressed = { lastPressed = "${key.role.displayName} • ${config.shapeFor(key.role).displayName}" },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.12f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewExpressiveKey(
    key: PreviewKey,
    config: ModuleConfig,
    tokens: PaletteTokens,
    onPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val morphAnim = remember { Animatable(0f) }
    val androidPath = remember { Path() }
    val bounds = remember { RectF() }

    val shape = config.shapeFor(key.role)
    val bgColor = Color(PaletteResolver.bgForRole(tokens, key.role))
    val fgColor = Color(PaletteResolver.fgForRole(tokens, key.role))
    val outlineColor = Color(tokens.outlineColor)
    val glowStart = Color(tokens.glowStart)
    val glowEnd = Color(tokens.glowEnd)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(key, config) {
                detectTapGestures(
                    onPress = {
                        onPressed()
                        val pressJob = scope.launch {
                            morphAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        tryAwaitRelease()
                        pressJob.cancel()
                        scope.launch {
                            morphAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = morphAnim.value.coerceIn(0f, 1.25f)
            val scaleFactor = 1f - (1f - config.pressScale) * p
            val rotationDeg = if (config.morphMode == MorphMode.ROTATE_AND_MORPH) 14f * p else 0f

            scale(scaleFactor, scaleFactor) {
                rotate(rotationDeg) {
                    val insetPx = (config.buttonInsetDp.dp.toPx()).coerceAtMost(size.minDimension * 0.15f)
                    bounds.set(insetPx, insetPx, size.width - insetPx, size.height - insetPx)
                    ExpressivePathFactory.buildPath(
                        outPath = androidPath,
                        bounds = bounds,
                        restingShape = shape,
                        morphMode = config.morphMode,
                        morphProgress = p
                    )
                    val path = androidPath.asComposePath()
                    if (config.showShadow) {
                        drawPath(path = path, color = Color.Black.copy(alpha = 0.16f))
                    }
                    drawPath(path = path, color = bgColor)
                    if (config.showExpressiveGlow && p > 0.01f) {
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    glowStart.copy(alpha = (0.30f * p).coerceIn(0f, 0.5f)),
                                    glowEnd.copy(alpha = (0.30f * p).coerceIn(0f, 0.5f))
                                )
                            )
                        )
                    }
                    if (config.showOutline && config.outlineWidthDp > 0f) {
                        drawPath(
                            path = path,
                            color = outlineColor,
                            style = Stroke(width = config.outlineWidthDp.dp.toPx())
                        )
                    }
                }
            }
        }
        Text(
            text = key.label,
            color = fgColor,
            fontSize = (18f * config.textScale.coerceIn(0.75f, 1.3f)).sp,
            fontWeight = if (key.role == ButtonRole.EQUALS) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
