package com.expressive.oneuicalc.render

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import com.expressive.oneuicalc.model.ButtonRole
import com.expressive.oneuicalc.model.ModuleConfig
import com.expressive.oneuicalc.model.PaletteRegistry
import com.expressive.oneuicalc.model.PaletteTokens
import com.expressive.oneuicalc.model.ThemeMode

object PaletteResolver {

    fun isDarkEffective(context: Context, config: ModuleConfig): Boolean {
        return when (config.themeMode) {
            ThemeMode.FORCE_LIGHT -> false
            ThemeMode.FORCE_DARK,
            ThemeMode.AMOLED_BLACK -> true
            ThemeMode.FOLLOW_SYSTEM -> {
                val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun resolve(context: Context, config: ModuleConfig): PaletteTokens {
        val isDark = isDarkEffective(context, config)
        val palette = PaletteRegistry.findById(config.paletteId)

        var tokens = when {
            palette.id == PaletteRegistry.CUSTOM.id -> config.customColors
            palette.isDynamicMonet -> resolveDynamicMonet(context, isDark)
            isDark -> palette.dark
            else -> palette.light
        }

        if (config.themeMode == ThemeMode.AMOLED_BLACK) {
            tokens = tokens.copy(
                background = Color.BLACK,
                numberBg = blendTowardBlack(tokens.numberBg, 0.45f),
                scientificBg = blendTowardBlack(tokens.scientificBg, 0.35f)
            )
        }
        return tokens
    }

    fun bgForRole(tokens: PaletteTokens, role: ButtonRole): Int = when (role) {
        ButtonRole.NUMBER -> tokens.numberBg
        ButtonRole.OPERATOR -> tokens.operatorBg
        ButtonRole.UTILITY -> tokens.utilityBg
        ButtonRole.EQUALS -> tokens.equalsBg
        ButtonRole.SCIENTIFIC -> tokens.scientificBg
    }

    fun fgForRole(tokens: PaletteTokens, role: ButtonRole): Int = when (role) {
        ButtonRole.NUMBER -> tokens.numberText
        ButtonRole.OPERATOR -> tokens.operatorText
        ButtonRole.UTILITY -> tokens.utilityText
        ButtonRole.EQUALS -> tokens.equalsText
        ButtonRole.SCIENTIFIC -> tokens.scientificText
    }

    private fun resolveDynamicMonet(context: Context, isDark: Boolean): PaletteTokens {
        return try {
            val a1_100 = context.getColor(android.R.color.system_accent1_100)
            val a1_200 = context.getColor(android.R.color.system_accent1_200)
            val a1_600 = context.getColor(android.R.color.system_accent1_600)
            val a1_700 = context.getColor(android.R.color.system_accent1_700)
            val a1_900 = context.getColor(android.R.color.system_accent1_900)
            val a2_100 = context.getColor(android.R.color.system_accent2_100)
            val a2_200 = context.getColor(android.R.color.system_accent2_200)
            val a2_700 = context.getColor(android.R.color.system_accent2_700)
            val a2_900 = context.getColor(android.R.color.system_accent2_900)
            val a3_100 = context.getColor(android.R.color.system_accent3_100)
            val a3_200 = context.getColor(android.R.color.system_accent3_200)
            val a3_700 = context.getColor(android.R.color.system_accent3_700)
            val a3_900 = context.getColor(android.R.color.system_accent3_900)
            val n1_10 = context.getColor(android.R.color.system_neutral1_10)
            val n1_50 = context.getColor(android.R.color.system_neutral1_50)
            val n1_100 = context.getColor(android.R.color.system_neutral1_100)
            val n1_800 = context.getColor(android.R.color.system_neutral1_800)
            val n1_900 = context.getColor(android.R.color.system_neutral1_900)
            val n2_100 = context.getColor(android.R.color.system_neutral2_100)
            val n2_700 = context.getColor(android.R.color.system_neutral2_700)
            val n2_800 = context.getColor(android.R.color.system_neutral2_800)
            val n1_0 = context.getColor(android.R.color.system_neutral1_0)

            if (isDark) {
                PaletteTokens(
                    background = n1_900,
                    formulaText = n1_50,
                    resultText = a1_200,
                    symbolAccent = a3_200,
                    numberBg = n1_800,
                    numberText = n1_50,
                    operatorBg = a2_700,
                    operatorText = a2_100,
                    utilityBg = a3_700,
                    utilityText = a3_100,
                    equalsBg = a1_200,
                    equalsText = a1_900,
                    scientificBg = n2_800,
                    scientificText = n2_100,
                    outlineColor = Color.argb(64, Color.red(a1_200), Color.green(a1_200), Color.blue(a1_200)),
                    glowStart = a1_600,
                    glowEnd = a3_700
                )
            } else {
                PaletteTokens(
                    background = n1_10,
                    formulaText = n1_900,
                    resultText = a1_600,
                    symbolAccent = a3_700,
                    numberBg = n1_100,
                    numberText = n1_900,
                    operatorBg = a2_100,
                    operatorText = a2_900,
                    utilityBg = a3_100,
                    utilityText = a3_900,
                    equalsBg = a1_600,
                    equalsText = n1_0,
                    scientificBg = n2_100,
                    scientificText = n2_700,
                    outlineColor = Color.argb(56, Color.red(a1_600), Color.green(a1_600), Color.blue(a1_600)),
                    glowStart = a1_200,
                    glowEnd = a3_200
                )
            }
        } catch (_: Throwable) {
            if (isDark) PaletteRegistry.DYNAMIC_MONET.dark else PaletteRegistry.DYNAMIC_MONET.light
        }
    }

    private fun blendTowardBlack(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
}
