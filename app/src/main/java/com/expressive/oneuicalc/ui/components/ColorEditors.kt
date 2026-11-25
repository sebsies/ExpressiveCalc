package com.expressive.oneuicalc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expressive.oneuicalc.model.PaletteTokens
import android.graphics.Color as AndroidColor

data class ColorSlot(
    val title: String,
    val get: (PaletteTokens) -> Int,
    val set: (PaletteTokens, Int) -> PaletteTokens
)

val CUSTOM_COLOR_SLOTS = listOf(
    ColorSlot("Canvas Background", { it.background }, { t, c -> t.copy(background = c) }),
    ColorSlot("Formula Text", { it.formulaText }, { t, c -> t.copy(formulaText = c) }),
    ColorSlot("Result Text", { it.resultText }, { t, c -> t.copy(resultText = c) }),
    ColorSlot("Symbol & Cursor Accent", { it.symbolAccent }, { t, c -> t.copy(symbolAccent = c) }),
    ColorSlot("Number Key Fill", { it.numberBg }, { t, c -> t.copy(numberBg = c) }),
    ColorSlot("Number Key Text", { it.numberText }, { t, c -> t.copy(numberText = c) }),
    ColorSlot("Operator Key Fill", { it.operatorBg }, { t, c -> t.copy(operatorBg = c) }),
    ColorSlot("Operator Key Text", { it.operatorText }, { t, c -> t.copy(operatorText = c) }),
    ColorSlot("Utility Key Fill (C, %, ⌫)", { it.utilityBg }, { t, c -> t.copy(utilityBg = c) }),
    ColorSlot("Utility Key Text", { it.utilityText }, { t, c -> t.copy(utilityText = c) }),
    ColorSlot("Equals (=) Key Fill", { it.equalsBg }, { t, c -> t.copy(equalsBg = c) }),
    ColorSlot("Equals (=) Key Text", { it.equalsText }, { t, c -> t.copy(equalsText = c) }),
    ColorSlot("Scientific Key Fill", { it.scientificBg }, { t, c -> t.copy(scientificBg = c) }),
    ColorSlot("Scientific Key Text", { it.scientificText }, { t, c -> t.copy(scientificText = c) }),
    ColorSlot("Outline Stroke Color", { it.outlineColor }, { t, c -> t.copy(outlineColor = c) }),
    ColorSlot("Press Glow Start", { it.glowStart }, { t, c -> t.copy(glowStart = c) }),
    ColorSlot("Press Glow End", { it.glowEnd }, { t, c -> t.copy(glowEnd = c) })
)

@Composable
fun ColorTokenRow(
    title: String,
    colorInt: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = String.format("#%08X", colorInt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(colorInt))
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}

@Composable
fun HexColorPickerDialog(
    title: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var red by remember { mutableFloatStateOf(AndroidColor.red(initialColor) / 255f) }
    var green by remember { mutableFloatStateOf(AndroidColor.green(initialColor) / 255f) }
    var blue by remember { mutableFloatStateOf(AndroidColor.blue(initialColor) / 255f) }
    var alpha by remember { mutableFloatStateOf(AndroidColor.alpha(initialColor) / 255f) }

    val currentColor = Color(red, green, blue, alpha).toArgb()
    var hexText by remember(currentColor) {
        mutableStateOf(String.format("#%08X", currentColor))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(currentColor))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                )
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        val cleaned = input.trim()
                        if (cleaned.startsWith("#") && (cleaned.length == 7 || cleaned.length == 9)) {
                            try {
                                val parsed = AndroidColor.parseColor(cleaned)
                                alpha = AndroidColor.alpha(parsed) / 255f
                                red = AndroidColor.red(parsed) / 255f
                                green = AndroidColor.green(parsed) / 255f
                                blue = AndroidColor.blue(parsed) / 255f
                            } catch (_: IllegalArgumentException) {
                            }
                        }
                    },
                    label = { Text("Hex (#AARRGGBB or #RRGGBB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ChannelSlider("R", red, Color(0xFFE53935)) { red = it }
                ChannelSlider("G", green, Color(0xFF43A047)) { green = it }
                ChannelSlider("B", blue, Color(0xFF1E88E5)) { blue = it }
                ChannelSlider("A", alpha, MaterialTheme.colorScheme.primary) { alpha = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Float,
    tint: Color,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label ${(value * 255).toInt()}",
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f)
        )
    }
}
