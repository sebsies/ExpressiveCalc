package com.expressive.oneuicalc.model

enum class ExpressiveShape(val displayName: String, val shortLabel: String) {
    PILL("Pill / Circle", "Pill"),
    SQUIRCLE("Squircle", "Squircle"),
    SOFT_SQUARE("Soft Square", "Soft Sq"),
    CUT_CORNER("Gem Cut", "Gem"),
    CLOVER("4-Leaf Clover", "Clover"),
    FLOWER("8-Petal Flower", "Flower"),
    COOKIE("12-Wave Cookie", "Cookie"),
    DIAMOND("Puffy Diamond", "Diamond"),
    ARCH("Expressive Arch", "Arch"),
    SLANT("Slanted Key", "Slant");

    companion object {
        fun fromName(name: String?, default: ExpressiveShape): ExpressiveShape {
            return entries.firstOrNull { it.name == name } ?: default
        }
    }
}

enum class MorphMode(val displayName: String, val description: String) {
    EXPRESSIVE_MORPH(
        "M3 Expressive Morph",
        "Morphs resting shapes into complementary rounded/circle forms on press with spring bounce"
    ),
    SQUISH_SHARPEN(
        "Squish & Tighten",
        "Compresses gently and tightens corner curvature while held"
    ),
    ROTATE_AND_MORPH(
        "Spin & Morph",
        "Rotates subtly (+14°) while morphing shape on press — ideal for Clover/Flower/Cookie"
    ),
    STATIC(
        "Static Shape",
        "Keeps the resting shape fixed with only subtle press tint"
    );

    companion object {
        fun fromName(name: String?): MorphMode {
            return entries.firstOrNull { it.name == name } ?: EXPRESSIVE_MORPH
        }
    }
}

data class ShapePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val numberShape: ExpressiveShape,
    val operatorShape: ExpressiveShape,
    val utilityShape: ExpressiveShape,
    val equalsShape: ExpressiveShape,
    val scientificShape: ExpressiveShape
)

object ShapePresets {
    val ALL = listOf(
        ShapePreset(
            id = "m3_expressive_mix",
            title = "M3 Expressive Mix",
            subtitle = "Squircle digits, Pill ops, Soft Sq utility, Clover =",
            numberShape = ExpressiveShape.SQUIRCLE,
            operatorShape = ExpressiveShape.PILL,
            utilityShape = ExpressiveShape.SOFT_SQUARE,
            equalsShape = ExpressiveShape.CLOVER,
            scientificShape = ExpressiveShape.PILL
        ),
        ShapePreset(
            id = "botanical_bloom",
            title = "Botanical Bloom",
            subtitle = "Soft Square digits, Arch ops, Flower utility, Cookie =",
            numberShape = ExpressiveShape.SOFT_SQUARE,
            operatorShape = ExpressiveShape.ARCH,
            utilityShape = ExpressiveShape.FLOWER,
            equalsShape = ExpressiveShape.COOKIE,
            scientificShape = ExpressiveShape.SQUIRCLE
        ),
        ShapePreset(
            id = "all_squircle",
            title = "Unified Squircle",
            subtitle = "Continuous superellipse across every key",
            numberShape = ExpressiveShape.SQUIRCLE,
            operatorShape = ExpressiveShape.SQUIRCLE,
            utilityShape = ExpressiveShape.SQUIRCLE,
            equalsShape = ExpressiveShape.SQUIRCLE,
            scientificShape = ExpressiveShape.SQUIRCLE
        ),
        ShapePreset(
            id = "geometric_gem",
            title = "Geometric Facets",
            subtitle = "Gem-cut digits, Diamond ops, Cookie =",
            numberShape = ExpressiveShape.CUT_CORNER,
            operatorShape = ExpressiveShape.DIAMOND,
            utilityShape = ExpressiveShape.SOFT_SQUARE,
            equalsShape = ExpressiveShape.COOKIE,
            scientificShape = ExpressiveShape.CUT_CORNER
        ),
        ShapePreset(
            id = "playful_slanted",
            title = "Kinetic Slant",
            subtitle = "Slanted keys with Clover = and Flower utility",
            numberShape = ExpressiveShape.SLANT,
            operatorShape = ExpressiveShape.SQUIRCLE,
            utilityShape = ExpressiveShape.FLOWER,
            equalsShape = ExpressiveShape.CLOVER,
            scientificShape = ExpressiveShape.SLANT
        ),
        ShapePreset(
            id = "classic_pill",
            title = "Classic One UI Pill",
            subtitle = "Standard circular/pill geometry with custom colors",
            numberShape = ExpressiveShape.PILL,
            operatorShape = ExpressiveShape.PILL,
            utilityShape = ExpressiveShape.PILL,
            equalsShape = ExpressiveShape.PILL,
            scientificShape = ExpressiveShape.PILL
        )
    )
}
