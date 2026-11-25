package com.expressive.oneuicalc.model

import android.content.SharedPreferences

data class ModuleConfig(
    val enabled: Boolean = true,
    val paletteId: String = PaletteRegistry.PIXEL_BERRY.id,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val numberShape: ExpressiveShape = ExpressiveShape.SQUIRCLE,
    val operatorShape: ExpressiveShape = ExpressiveShape.PILL,
    val utilityShape: ExpressiveShape = ExpressiveShape.SOFT_SQUARE,
    val equalsShape: ExpressiveShape = ExpressiveShape.CLOVER,
    val scientificShape: ExpressiveShape = ExpressiveShape.PILL,
    val morphMode: MorphMode = MorphMode.EXPRESSIVE_MORPH,
    val pressScale: Float = 0.94f,
    val showOutline: Boolean = false,
    val outlineWidthDp: Float = 1.5f,
    val showShadow: Boolean = false,
    val showExpressiveGlow: Boolean = true,
    val buttonInsetDp: Float = 1.5f,
    val textScale: Float = 1.0f,
    val hideConverterButton: Boolean = false,
    val hideHistoryButton: Boolean = false,
    val disableHaptics: Boolean = false,
    val customColors: PaletteTokens = PaletteRegistry.CUSTOM.dark
) {
    fun shapeFor(role: ButtonRole): ExpressiveShape = when (role) {
        ButtonRole.NUMBER -> numberShape
        ButtonRole.OPERATOR -> operatorShape
        ButtonRole.UTILITY -> utilityShape
        ButtonRole.EQUALS -> equalsShape
        ButtonRole.SCIENTIFIC -> scientificShape
    }

    fun writeTo(editor: SharedPreferences.Editor) {
        editor.putBoolean(KEY_ENABLED, enabled)
        editor.putString(KEY_PALETTE_ID, paletteId)
        editor.putString(KEY_THEME_MODE, themeMode.name)
        editor.putString(KEY_SHAPE_NUMBER, numberShape.name)
        editor.putString(KEY_SHAPE_OPERATOR, operatorShape.name)
        editor.putString(KEY_SHAPE_UTILITY, utilityShape.name)
        editor.putString(KEY_SHAPE_EQUALS, equalsShape.name)
        editor.putString(KEY_SHAPE_SCIENTIFIC, scientificShape.name)
        editor.putString(KEY_MORPH_MODE, morphMode.name)
        editor.putFloat(KEY_PRESS_SCALE, pressScale)
        editor.putBoolean(KEY_SHOW_OUTLINE, showOutline)
        editor.putFloat(KEY_OUTLINE_WIDTH_DP, outlineWidthDp)
        editor.putBoolean(KEY_SHOW_SHADOW, showShadow)
        editor.putBoolean(KEY_SHOW_GLOW, showExpressiveGlow)
        editor.putFloat(KEY_BUTTON_INSET_DP, buttonInsetDp)
        editor.putFloat(KEY_TEXT_SCALE, textScale)
        editor.putBoolean(KEY_HIDE_CONVERTER, hideConverterButton)
        editor.putBoolean(KEY_HIDE_HISTORY, hideHistoryButton)
        editor.putBoolean(KEY_DISABLE_HAPTICS, disableHaptics)

        editor.putInt(KEY_CUSTOM_BG, customColors.background)
        editor.putInt(KEY_CUSTOM_FORMULA, customColors.formulaText)
        editor.putInt(KEY_CUSTOM_RESULT, customColors.resultText)
        editor.putInt(KEY_CUSTOM_SYMBOL, customColors.symbolAccent)
        editor.putInt(KEY_CUSTOM_NUM_BG, customColors.numberBg)
        editor.putInt(KEY_CUSTOM_NUM_FG, customColors.numberText)
        editor.putInt(KEY_CUSTOM_OP_BG, customColors.operatorBg)
        editor.putInt(KEY_CUSTOM_OP_FG, customColors.operatorText)
        editor.putInt(KEY_CUSTOM_UTIL_BG, customColors.utilityBg)
        editor.putInt(KEY_CUSTOM_UTIL_FG, customColors.utilityText)
        editor.putInt(KEY_CUSTOM_EQ_BG, customColors.equalsBg)
        editor.putInt(KEY_CUSTOM_EQ_FG, customColors.equalsText)
        editor.putInt(KEY_CUSTOM_SCI_BG, customColors.scientificBg)
        editor.putInt(KEY_CUSTOM_SCI_FG, customColors.scientificText)
        editor.putInt(KEY_CUSTOM_OUTLINE, customColors.outlineColor)
        editor.putInt(KEY_CUSTOM_GLOW_START, customColors.glowStart)
        editor.putInt(KEY_CUSTOM_GLOW_END, customColors.glowEnd)
        editor.apply()
    }

    companion object {
        const val PREFS_NAME = "expressive_prefs"

        private const val KEY_ENABLED = "enabled"
        private const val KEY_PALETTE_ID = "palette_id"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SHAPE_NUMBER = "shape_number"
        private const val KEY_SHAPE_OPERATOR = "shape_operator"
        private const val KEY_SHAPE_UTILITY = "shape_utility"
        private const val KEY_SHAPE_EQUALS = "shape_equals"
        private const val KEY_SHAPE_SCIENTIFIC = "shape_scientific"
        private const val KEY_MORPH_MODE = "morph_mode"
        private const val KEY_PRESS_SCALE = "press_scale"
        private const val KEY_SHOW_OUTLINE = "show_outline"
        private const val KEY_OUTLINE_WIDTH_DP = "outline_width_dp"
        private const val KEY_SHOW_SHADOW = "show_shadow"
        private const val KEY_SHOW_GLOW = "show_glow"
        private const val KEY_BUTTON_INSET_DP = "button_inset_dp"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_HIDE_CONVERTER = "hide_converter"
        private const val KEY_HIDE_HISTORY = "hide_history"
        private const val KEY_DISABLE_HAPTICS = "disable_haptics"

        private const val KEY_CUSTOM_BG = "custom_bg"
        private const val KEY_CUSTOM_FORMULA = "custom_formula"
        private const val KEY_CUSTOM_RESULT = "custom_result"
        private const val KEY_CUSTOM_SYMBOL = "custom_symbol"
        private const val KEY_CUSTOM_NUM_BG = "custom_num_bg"
        private const val KEY_CUSTOM_NUM_FG = "custom_num_fg"
        private const val KEY_CUSTOM_OP_BG = "custom_op_bg"
        private const val KEY_CUSTOM_OP_FG = "custom_op_fg"
        private const val KEY_CUSTOM_UTIL_BG = "custom_util_bg"
        private const val KEY_CUSTOM_UTIL_FG = "custom_util_fg"
        private const val KEY_CUSTOM_EQ_BG = "custom_eq_bg"
        private const val KEY_CUSTOM_EQ_FG = "custom_eq_fg"
        private const val KEY_CUSTOM_SCI_BG = "custom_sci_bg"
        private const val KEY_CUSTOM_SCI_FG = "custom_sci_fg"
        private const val KEY_CUSTOM_OUTLINE = "custom_outline"
        private const val KEY_CUSTOM_GLOW_START = "custom_glow_start"
        private const val KEY_CUSTOM_GLOW_END = "custom_glow_end"

        fun from(prefs: SharedPreferences?): ModuleConfig {
            if (prefs == null) return ModuleConfig()
            val d = ModuleConfig()
            val dc = d.customColors
            return ModuleConfig(
                enabled = prefs.getBoolean(KEY_ENABLED, d.enabled),
                paletteId = prefs.getString(KEY_PALETTE_ID, d.paletteId) ?: d.paletteId,
                themeMode = ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, d.themeMode.name)),
                numberShape = ExpressiveShape.fromName(prefs.getString(KEY_SHAPE_NUMBER, null), d.numberShape),
                operatorShape = ExpressiveShape.fromName(prefs.getString(KEY_SHAPE_OPERATOR, null), d.operatorShape),
                utilityShape = ExpressiveShape.fromName(prefs.getString(KEY_SHAPE_UTILITY, null), d.utilityShape),
                equalsShape = ExpressiveShape.fromName(prefs.getString(KEY_SHAPE_EQUALS, null), d.equalsShape),
                scientificShape = ExpressiveShape.fromName(prefs.getString(KEY_SHAPE_SCIENTIFIC, null), d.scientificShape),
                morphMode = MorphMode.fromName(prefs.getString(KEY_MORPH_MODE, null)),
                pressScale = prefs.getFloat(KEY_PRESS_SCALE, d.pressScale),
                showOutline = prefs.getBoolean(KEY_SHOW_OUTLINE, d.showOutline),
                outlineWidthDp = prefs.getFloat(KEY_OUTLINE_WIDTH_DP, d.outlineWidthDp),
                showShadow = prefs.getBoolean(KEY_SHOW_SHADOW, d.showShadow),
                showExpressiveGlow = prefs.getBoolean(KEY_SHOW_GLOW, d.showExpressiveGlow),
                buttonInsetDp = prefs.getFloat(KEY_BUTTON_INSET_DP, d.buttonInsetDp),
                textScale = prefs.getFloat(KEY_TEXT_SCALE, d.textScale),
                hideConverterButton = prefs.getBoolean(KEY_HIDE_CONVERTER, d.hideConverterButton),
                hideHistoryButton = prefs.getBoolean(KEY_HIDE_HISTORY, d.hideHistoryButton),
                disableHaptics = prefs.getBoolean(KEY_DISABLE_HAPTICS, d.disableHaptics),
                customColors = PaletteTokens(
                    background = prefs.getInt(KEY_CUSTOM_BG, dc.background),
                    formulaText = prefs.getInt(KEY_CUSTOM_FORMULA, dc.formulaText),
                    resultText = prefs.getInt(KEY_CUSTOM_RESULT, dc.resultText),
                    symbolAccent = prefs.getInt(KEY_CUSTOM_SYMBOL, dc.symbolAccent),
                    numberBg = prefs.getInt(KEY_CUSTOM_NUM_BG, dc.numberBg),
                    numberText = prefs.getInt(KEY_CUSTOM_NUM_FG, dc.numberText),
                    operatorBg = prefs.getInt(KEY_CUSTOM_OP_BG, dc.operatorBg),
                    operatorText = prefs.getInt(KEY_CUSTOM_OP_FG, dc.operatorText),
                    utilityBg = prefs.getInt(KEY_CUSTOM_UTIL_BG, dc.utilityBg),
                    utilityText = prefs.getInt(KEY_CUSTOM_UTIL_FG, dc.utilityText),
                    equalsBg = prefs.getInt(KEY_CUSTOM_EQ_BG, dc.equalsBg),
                    equalsText = prefs.getInt(KEY_CUSTOM_EQ_FG, dc.equalsText),
                    scientificBg = prefs.getInt(KEY_CUSTOM_SCI_BG, dc.scientificBg),
                    scientificText = prefs.getInt(KEY_CUSTOM_SCI_FG, dc.scientificText),
                    outlineColor = prefs.getInt(KEY_CUSTOM_OUTLINE, dc.outlineColor),
                    glowStart = prefs.getInt(KEY_CUSTOM_GLOW_START, dc.glowStart),
                    glowEnd = prefs.getInt(KEY_CUSTOM_GLOW_END, dc.glowEnd)
                )
            )
        }
    }
}
