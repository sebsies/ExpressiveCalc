package com.expressive.oneuicalc.model

enum class ThemeMode(val displayName: String) {
    FOLLOW_SYSTEM("Follow System"),
    FORCE_LIGHT("Always Light"),
    FORCE_DARK("Always Dark"),
    AMOLED_BLACK("True AMOLED Black");

    companion object {
        fun fromName(name: String?): ThemeMode {
            return entries.firstOrNull { it.name == name } ?: FOLLOW_SYSTEM
        }
    }
}

data class PaletteTokens(
    val background: Int,
    val formulaText: Int,
    val resultText: Int,
    val symbolAccent: Int,
    val numberBg: Int,
    val numberText: Int,
    val operatorBg: Int,
    val operatorText: Int,
    val utilityBg: Int,
    val utilityText: Int,
    val equalsBg: Int,
    val equalsText: Int,
    val scientificBg: Int,
    val scientificText: Int,
    val outlineColor: Int,
    val glowStart: Int,
    val glowEnd: Int
)

data class ExpressivePalette(
    val id: String,
    val name: String,
    val tagline: String,
    val isDynamicMonet: Boolean = false,
    val light: PaletteTokens,
    val dark: PaletteTokens
)

object PaletteRegistry {
    private fun c(hex: Long): Int = hex.toInt()

    val DYNAMIC_MONET = ExpressivePalette(
        id = "dynamic_monet",
        name = "Material You (Monet)",
        tagline = "Follows your Android 17 wallpaper accent & neutral tones",
        isDynamicMonet = true,
        light = PaletteTokens(
            background = c(0xFFFDF7FF),
            formulaText = c(0xFF1D1B20),
            resultText = c(0xFF6750A4),
            symbolAccent = c(0xFF7D5260),
            numberBg = c(0xFFF3EDF7),
            numberText = c(0xFF1D1B20),
            operatorBg = c(0xFFE8DEF8),
            operatorText = c(0xFF1D192B),
            utilityBg = c(0xFFFFD8E4),
            utilityText = c(0xFF31111D),
            equalsBg = c(0xFF6750A4),
            equalsText = c(0xFFFFFFFF),
            scientificBg = c(0xFFE7E0EC),
            scientificText = c(0xFF49454F),
            outlineColor = c(0x3379747E),
            glowStart = c(0xFFD0BCFF),
            glowEnd = c(0xFFEFB8C8)
        ),
        dark = PaletteTokens(
            background = c(0xFF141218),
            formulaText = c(0xFFE6E0E9),
            resultText = c(0xFFD0BCFF),
            symbolAccent = c(0xFFEFB8C8),
            numberBg = c(0xFF211F26),
            numberText = c(0xFFE6E0E9),
            operatorBg = c(0xFF4A4458),
            operatorText = c(0xFFE8DEF8),
            utilityBg = c(0xFF633B48),
            utilityText = c(0xFFFFD8E4),
            equalsBg = c(0xFFD0BCFF),
            equalsText = c(0xFF381E72),
            scientificBg = c(0xFF2B2930),
            scientificText = c(0xFFCAC4D0),
            outlineColor = c(0x40938F99),
            glowStart = c(0xFF6750A4),
            glowEnd = c(0xFF7D5260)
        )
    )

    val PIXEL_BERRY = ExpressivePalette(
        id = "pixel_berry",
        name = "Pixel Berry",
        tagline = "Expressive magenta-plum tonal contrast",
        light = PaletteTokens(
            background = c(0xFFFFF8F9),
            formulaText = c(0xFF23191D),
            resultText = c(0xFFB31E5B),
            symbolAccent = c(0xFF8C4A60),
            numberBg = c(0xFFFCEEF2),
            numberText = c(0xFF23191D),
            operatorBg = c(0xFFFAD8E4),
            operatorText = c(0xFF3D0C20),
            utilityBg = c(0xFFEDE0F5),
            utilityText = c(0xFF2E1542),
            equalsBg = c(0xFFC2185B),
            equalsText = c(0xFFFFFFFF),
            scientificBg = c(0xFFF3E5EA),
            scientificText = c(0xFF524347),
            outlineColor = c(0x339E7C87),
            glowStart = c(0xFFFF80AB),
            glowEnd = c(0xFFCE93D8)
        ),
        dark = PaletteTokens(
            background = c(0xFF191114),
            formulaText = c(0xFFEFDFE3),
            resultText = c(0xFFFF80AB),
            symbolAccent = c(0xFFE5B8FF),
            numberBg = c(0xFF271D21),
            numberText = c(0xFFEFDFE3),
            operatorBg = c(0xFF5C283C),
            operatorText = c(0xFFFFD9E4),
            utilityBg = c(0xFF423254),
            utilityText = c(0xFFECD8FF),
            equalsBg = c(0xFFFF6E9F),
            equalsText = c(0xFF3E001D),
            scientificBg = c(0xFF32262B),
            scientificText = c(0xFFD6C2C7),
            outlineColor = c(0x40B08896),
            glowStart = c(0xFFC2185B),
            glowEnd = c(0xFF7B1FA2)
        )
    )

    val BOTANICAL_MATCHA = ExpressivePalette(
        id = "botanical_matcha",
        name = "Botanical Matcha",
        tagline = "Fresh organic sage, mint & warm oat",
        light = PaletteTokens(
            background = c(0xFFF7FBF2),
            formulaText = c(0xFF181D16),
            resultText = c(0xFF2E6B2C),
            symbolAccent = c(0xFF52634F),
            numberBg = c(0xFFEDF3E6),
            numberText = c(0xFF181D16),
            operatorBg = c(0xFFD5E8CA),
            operatorText = c(0xFF10200D),
            utilityBg = c(0xFFBCECE0),
            utilityText = c(0xFF00201B),
            equalsBg = c(0xFF2F6B2D),
            equalsText = c(0xFFFFFFFF),
            scientificBg = c(0xFFE3EAD9),
            scientificText = c(0xFF42493E),
            outlineColor = c(0x3372796D),
            glowStart = c(0xFFA5D6A7),
            glowEnd = c(0xFF80CBC4)
        ),
        dark = PaletteTokens(
            background = c(0xFF10150E),
            formulaText = c(0xFFE1E4DA),
            resultText = c(0xFF98D68D),
            symbolAccent = c(0xFFA0CFC4),
            numberBg = c(0xFF1D221B),
            numberText = c(0xFFE1E4DA),
            operatorBg = c(0xFF344E30),
            operatorText = c(0xFFD3F0CB),
            utilityBg = c(0xFF1F4D44),
            utilityText = c(0xFFBCECE0),
            equalsBg = c(0xFF96D68B),
            equalsText = c(0xFF0A3908),
            scientificBg = c(0xFF262D23),
            scientificText = c(0xFFC3C8BC),
            outlineColor = c(0x408B9386),
            glowStart = c(0xFF2E7D32),
            glowEnd = c(0xFF00897B)
        )
    )

    val ELECTRIC_COBALT = ExpressivePalette(
        id = "electric_cobalt",
        name = "Electric Cobalt",
        tagline = "Crisp Material blue & electric cyan",
        light = PaletteTokens(
            background = c(0xFFF8F9FF),
            formulaText = c(0xFF191C22),
            resultText = c(0xFF1B5EF0),
            symbolAccent = c(0xFF006782),
            numberBg = c(0xFFEEF1FA),
            numberText = c(0xFF191C22),
            operatorBg = c(0xFFD8E2FF),
            operatorText = c(0xFF001A43),
            utilityBg = c(0xFFC2E8FF),
            utilityText = c(0xFF001E2B),
            equalsBg = c(0xFF1E5EFF),
            equalsText = c(0xFFFFFFFF),
            scientificBg = c(0xFFE3E7F2),
            scientificText = c(0xFF434750),
            outlineColor = c(0x33747781),
            glowStart = c(0xFF82B1FF),
            glowEnd = c(0xFF80D8FF)
        ),
        dark = PaletteTokens(
            background = c(0xFF11131A),
            formulaText = c(0xFFE2E2EC),
            resultText = c(0xFF9EBBFF),
            symbolAccent = c(0xFF7BD3FF),
            numberBg = c(0xFF1C2029),
            numberText = c(0xFFE2E2EC),
            operatorBg = c(0xFF26407A),
            operatorText = c(0xFFDCE4FF),
            utilityBg = c(0xFF0E4B63),
            utilityText = c(0xFFC4EAFF),
            equalsBg = c(0xFF8CB4FF),
            equalsText = c(0xFF002B75),
            scientificBg = c(0xFF252A36),
            scientificText = c(0xFFC5C6D2),
            outlineColor = c(0x408E909C),
            glowStart = c(0xFF2962FF),
            glowEnd = c(0xFF00B0FF)
        )
    )

    val SUNSET_APRICOT = ExpressivePalette(
        id = "sunset_apricot",
        name = "Sunset Apricot",
        tagline = "Warm terracotta, peach & toasted amber",
        light = PaletteTokens(
            background = c(0xFFFFF8F5),
            formulaText = c(0xFF231A15),
            resultText = c(0xFFC84B00),
            symbolAccent = c(0xFF8A5026),
            numberBg = c(0xFFFCEFE8),
            numberText = c(0xFF231A15),
            operatorBg = c(0xFFFFDBCA),
            operatorText = c(0xFF341100),
            utilityBg = c(0xFFFFE5B4),
            utilityText = c(0xFF2E1B00),
            equalsBg = c(0xFFD94F04),
            equalsText = c(0xFFFFFFFF),
            scientificBg = c(0xFFF3E4DC),
            scientificText = c(0xFF53443C),
            outlineColor = c(0x3386736A),
            glowStart = c(0xFFFFAB91),
            glowEnd = c(0xFFFFE082)
        ),
        dark = PaletteTokens(
            background = c(0xFF1A120E),
            formulaText = c(0xFFF2DFD6),
            resultText = c(0xFFFFB595),
            symbolAccent = c(0xFFFFD180),
            numberBg = c(0xFF271D18),
            numberText = c(0xFFF2DFD6),
            operatorBg = c(0xFF662E14),
            operatorText = c(0xFFFFDCCD),
            utilityBg = c(0xFF543C10),
            utilityText = c(0xFFFFE7BA),
            equalsBg = c(0xFFFF9668),
            equalsText = c(0xFF471600),
            scientificBg = c(0xFF332620),
            scientificText = c(0xFFD9C3B9),
            outlineColor = c(0x40A18C83),
            glowStart = c(0xFFE64A19),
            glowEnd = c(0xFFFFA000)
        )
    )

    val CATPPUCCIN = ExpressivePalette(
        id = "catppuccin",
        name = "Catppuccin Mocha / Latte",
        tagline = "Soothing pastel developer palette",
        light = PaletteTokens(
            background = c(0xFFEFF1F5),
            formulaText = c(0xFF4C4F69),
            resultText = c(0xFF8839EF),
            symbolAccent = c(0xFF1E66F5),
            numberBg = c(0xFFE6E9EF),
            numberText = c(0xFF4C4F69),
            operatorBg = c(0xFFCCD0DA),
            operatorText = c(0xFF1E66F5),
            utilityBg = c(0xFFF5D0DC),
            utilityText = c(0xFFD20F39),
            equalsBg = c(0xFF8839EF),
            equalsText = c(0xFFFFFFFF),
            scientificBg = c(0xFFDCE0E8),
            scientificText = c(0xFF5C5F77),
            outlineColor = c(0x338C8FA1),
            glowStart = c(0xFFCA9EE6),
            glowEnd = c(0xFF8CAAEE)
        ),
        dark = PaletteTokens(
            background = c(0xFF1E1E2E),
            formulaText = c(0xFFCDD6F4),
            resultText = c(0xFFCBA6F7),
            symbolAccent = c(0xFF89B4FA),
            numberBg = c(0xFF313244),
            numberText = c(0xFFCDD6F4),
            operatorBg = c(0xFF45475A),
            operatorText = c(0xFF89B4FA),
            utilityBg = c(0xFF4D3443),
            utilityText = c(0xFFF38BA8),
            equalsBg = c(0xFFCBA6F7),
            equalsText = c(0xFF11111B),
            scientificBg = c(0xFF28293A),
            scientificText = c(0xFFBAC2DE),
            outlineColor = c(0x406C7086),
            glowStart = c(0xFFCBA6F7),
            glowEnd = c(0xFF89B4FA)
        )
    )

    val AMOLED_CYBER_NEON = ExpressivePalette(
        id = "amoled_cyber",
        name = "AMOLED Cyber Neon",
        tagline = "Pure #000000 canvas with electric neon keys",
        light = PaletteTokens(
            background = c(0xFFF5F7FA),
            formulaText = c(0xFF0E141B),
            resultText = c(0xFF0091EA),
            symbolAccent = c(0xFFD500F9),
            numberBg = c(0xFFE8EEF5),
            numberText = c(0xFF0E141B),
            operatorBg = c(0xFFE0F7FA),
            operatorText = c(0xFF006064),
            utilityBg = c(0xFFFCE4EC),
            utilityText = c(0xFFC51162),
            equalsBg = c(0xFF00BFA5),
            equalsText = c(0xFF00201C),
            scientificBg = c(0xFFEDE7F6),
            scientificText = c(0xFF4A148C),
            outlineColor = c(0x5500B0FF),
            glowStart = c(0xFF00E5FF),
            glowEnd = c(0xFFFF4081)
        ),
        dark = PaletteTokens(
            background = c(0xFF000000),
            formulaText = c(0xFFF5F9FF),
            resultText = c(0xFF00E5FF),
            symbolAccent = c(0xFFFF4081),
            numberBg = c(0xFF111318),
            numberText = c(0xFFF0F4F8),
            operatorBg = c(0xFF0A2630),
            operatorText = c(0xFF00E5FF),
            utilityBg = c(0xFF2C0B1E),
            utilityText = c(0xFFFF5C93),
            equalsBg = c(0xFF00E676),
            equalsText = c(0xFF00210D),
            scientificBg = c(0xFF1A1229),
            scientificText = c(0xFFCE93D8),
            outlineColor = c(0x8800E5FF),
            glowStart = c(0xFF00E5FF),
            glowEnd = c(0xFF00E676)
        )
    )

    val RETRO_SYNTH = ExpressivePalette(
        id = "retro_synth",
        name = "Retro Tactile (TE-1)",
        tagline = "Industrial hardware cream/graphite + burnt orange =",
        light = PaletteTokens(
            background = c(0xFFF1EFEA),
            formulaText = c(0xFF181818),
            resultText = c(0xFFE64800),
            symbolAccent = c(0xFF2B6CB0),
            numberBg = c(0xFFFFFFFF),
            numberText = c(0xFF1A1A1A),
            operatorBg = c(0xFFE1DFD8),
            operatorText = c(0xFF1A1A1A),
            utilityBg = c(0xFF333333),
            utilityText = c(0xFFF6F4EE),
            equalsBg = c(0xFFFF4F00),
            equalsText = c(0xFFFFFFFF),
            scientificBg = c(0xFFE6E3DC),
            scientificText = c(0xFF404040),
            outlineColor = c(0x44000000),
            glowStart = c(0xFFFF6D00),
            glowEnd = c(0xFFFFAB00)
        ),
        dark = PaletteTokens(
            background = c(0xFF141414),
            formulaText = c(0xFFF3F1EC),
            resultText = c(0xFFFF661A),
            symbolAccent = c(0xFF63B3ED),
            numberBg = c(0xFF262626),
            numberText = c(0xFFF3F1EC),
            operatorBg = c(0xFF3B3B3B),
            operatorText = c(0xFFF3F1EC),
            utilityBg = c(0xFFD9D6CE),
            utilityText = c(0xFF141414),
            equalsBg = c(0xFFFF4F00),
            equalsText = c(0xFFFFFFFF),
            scientificBg = c(0xFF1F1F1F),
            scientificText = c(0xFFBDBDBD),
            outlineColor = c(0x55FFFFFF),
            glowStart = c(0xFFFF4F00),
            glowEnd = c(0xFFFFB300)
        )
    )

    val NORDIC_FROST = ExpressivePalette(
        id = "nordic_frost",
        name = "Nordic Frost",
        tagline = "Arctic slate blues & soft aurora accents",
        light = PaletteTokens(
            background = c(0xFFECEFF4),
            formulaText = c(0xFF2E3440),
            resultText = c(0xFF5E81AC),
            symbolAccent = c(0xFF4C566A),
            numberBg = c(0xFFE5E9F0),
            numberText = c(0xFF2E3440),
            operatorBg = c(0xFFD8DEE9),
            operatorText = c(0xFF2E3440),
            utilityBg = c(0xFFF3D5D8),
            utilityText = c(0xFFBF616A),
            equalsBg = c(0xFF5E81AC),
            equalsText = c(0xFFECEFF4),
            scientificBg = c(0xFFDFE5EE),
            scientificText = c(0xFF434C5E),
            outlineColor = c(0x334C566A),
            glowStart = c(0xFF88C0D0),
            glowEnd = c(0xFF81A1C1)
        ),
        dark = PaletteTokens(
            background = c(0xFF2E3440),
            formulaText = c(0xFFECEFF4),
            resultText = c(0xFF88C0D0),
            symbolAccent = c(0xFF81A1C1),
            numberBg = c(0xFF3B4252),
            numberText = c(0xFFECEFF4),
            operatorBg = c(0xFF434C5E),
            operatorText = c(0xFF88C0D0),
            utilityBg = c(0xFF4C3940),
            utilityText = c(0xFFED8796),
            equalsBg = c(0xFF88C0D0),
            equalsText = c(0xFF2E3440),
            scientificBg = c(0xFF363D4C),
            scientificText = c(0xFFD8DEE9),
            outlineColor = c(0x4081A1C1),
            glowStart = c(0xFF88C0D0),
            glowEnd = c(0xFF5E81AC)
        )
    )

    val CUSTOM = ExpressivePalette(
        id = "custom",
        name = "Custom Palette",
        tagline = "Your own handcrafted hex colors per button role",
        light = ELECTRIC_COBALT.light,
        dark = ELECTRIC_COBALT.dark
    )

    val ALL = listOf(
        DYNAMIC_MONET,
        PIXEL_BERRY,
        BOTANICAL_MATCHA,
        ELECTRIC_COBALT,
        SUNSET_APRICOT,
        CATPPUCCIN,
        AMOLED_CYBER_NEON,
        RETRO_SYNTH,
        NORDIC_FROST,
        CUSTOM
    )

    fun findById(id: String?): ExpressivePalette {
        return ALL.firstOrNull { it.id == id } ?: PIXEL_BERRY
    }
}
