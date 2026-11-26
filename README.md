<p align="center">
  <img src="assets/app_icon.png" alt="ExpressiveCalc App Icon" width="128" />
</p>

[![Get it on Orion Store](https://raw.githubusercontent.com/RookieEnough/Orion-Store/refs/heads/main/assets/orion-badge.png)](https://rookieenough.github.io/Orion-Data/app/?id=expressivecalc)
https://discord.gg/zsqNQF9aut
# ExpressiveCalc

An open-source **Vector / Xposed** root module that transforms Samsung's stock One UI 9+ Calculator (`com.sec.android.app.popupcalculator`) with **Material 3 Expressive** button geometry, spring-animated press morphing, dynamic color palettes, and a smooth expressive Unit Converter experience.

> **Transparency & Scope:** ExpressiveCalc operates purely at runtime via Zygisk/Vector (`libxposed`) hooks and does not modify or redistribute any Samsung APK files. Hooks were designed and verified against Samsung One UI 9 Calculator (`v12.6.00.35`, `minSdk 36` / `targetSdk 37`).

---

## Table of Contents

- [Features](#features)
- [Module Flavors & APKs](#module-flavors--apks)
- [Installation & Setup](#installation--setup)
- [How It Works](#how-it-works)
- [Project Structure](#project-structure)
- [Building from Source](#building-from-source)
- [License](#license)

---

## Features

### Material 3 Expressive Button Geometry & Spring Morphing
- **10 Custom Expressive Shapes** rendered via 128-point polar boundary sampling and morph interpolation:
  - `PILL`, `SQUIRCLE`, `SOFT_SQUARE`, `CUT_CORNER`, `CLOVER`, `FLOWER`, `COOKIE`, `DIAMOND`, `ARCH`, `SLANT`
- **Per-Role Customization:** Assign independent shapes for `NUMBER` (`0–9`, `.`), `OPERATOR` (`+`, `−`, `×`, `÷`), `UTILITY` (`C`, `()`, `%`, `⌫`, `↑`, `↓`, `+/-`), `EQUALS` (`=`), and `SCIENTIFIC` keys — or pick from **6 curated shape presets**.
- **4 Press Morph Modes:** `STATIC`, `SQUISH_SHARPEN`, `EXPRESSIVE_MORPH`, and `ROTATE_AND_MORPH`, driven per-button by an `OvershootInterpolator` spring curve (`MorphAnimator`).
- **Tactile Options:** Adjustable press scale, button inset, optional expressive press glow, drop shadow, outline width, text scale, and optional haptic/toolbar button toggles.

### Dynamic Color Engine & Themes
- **10 Built-in Palettes:**
  - `DYNAMIC_MONET` (reads `android.R.color.system_accent*` & `system_neutral*` live from your system wallpaper/theme)
  - `PIXEL_BERRY`, `BOTANICAL_MATCHA`, `ELECTRIC_COBALT`, `SUNSET_APRICOT`, `CATPPUCCIN`, `AMOLED_CYBER_NEON`, `RETRO_SYNTH`, `NORDIC_FROST`, and `CUSTOM` (full hex color editor for every token).
- **Theme Modes:** Follow System, Force Light, Force Dark, and true **AMOLED Black**.

### Full One UI 9 Calculator Coverage
- **Main & Scientific Keypads:** Hooks Samsung's custom `Canvas`-drawn `CustomButton` and `CustomImageView` views while preserving centered glyph metrics (`getXY` / `drawPowText` clip bounds remain untouched).
- **History Panel:** Removes hardcoded Samsung black row backgrounds (`ripple_effect_history_item`), applies palette colors to expressions/results/dividers, and restyles the **Clear history** button with your active expressive shape.
- **Unit Converter & Menus:**
  - Replaces static Samsung category tabs (`mortgage_tab_bg`) with [`ExpressiveTabIndicatorDrawable`](app/src/main/java/com/expressive/oneuicalc/render/ExpressiveTabIndicatorDrawable.kt) — a spring-animated gliding Material Expressive pill that smoothly stretches and crossfades tab labels during taps and swipes.
  - Styles dropdown pop-out menus (`PopupWindow` / `ListView`) cleanly with opaque `20dp` rounded containers and strips Samsung's blur/light-theme selector artifacts.
  - Handles disabled/enabled arrow buttons (`↑` / `↓`) cleanly without dark leftover color filters.

### Companion Configuration App
- Built with **Jetpack Compose + Material 3**, featuring an **interactive live mini One UI 9 Calculator preview** (`LiveCalculatorPreview`) with real-time spring morphing, shape/preset pickers, palette selector, custom hex editor, and cross-process Xposed preference synchronization.

---

## Module Flavors & APKs

Prebuilt, signed release APKs are included in [`apk/`](apk/):

| Flavor | Target API | Compatibility | APK |
| :--- | :--- | :--- | :--- |
| **`modern`** *(Recommended)* | `libxposed` API `102` (`service:101.0.0`) | **Vector v2.2+** | [`apk/ExpressiveCalc-v1.0.0-vector-api102.apk`](apk/ExpressiveCalc-v1.0.0-vector-api102.apk) |
| **`legacy`** *(Universal fallback)* | Xposed API `82` (`xposedsharedprefs`) | **Vector v1.x–v2.2 / LSPosed** | [`apk/ExpressiveCalc-v1.0.0-legacy-api82.apk`](apk/ExpressiveCalc-v1.0.0-legacy-api82.apk) |

> **Requirements:** Android 12+ (`minSdk = 31`, `compileSdk = 36`), rooted device with Zygisk + Vector (or LSPosed), and Samsung Calculator (`com.sec.android.app.popupcalculator`).

---

## Installation & Setup

1. Download and install **`ExpressiveCalc-v1.0.0-vector-api102.apk`** (or the `legacy` APK if your framework uses API 82).
2. Open **Vector Manager** (or LSPosed Manager) → **Modules** → enable **ExpressiveCalc**.
3. Ensure **Calculator** (`com.sec.android.app.popupcalculator`) is checked in the module scope.
4. **Force-stop** Samsung Calculator and reopen it.
5. Open the **ExpressiveCalc** companion app anytime to customize shapes, morph behavior, and color palettes, then force-stop/relaunch Calculator to apply preference changes.

---

## How It Works

Samsung's One UI 9 Calculator does not draw its keypad with standard XML `<shape>` backgrounds—it renders button frames, blur-cached shadows, and touch gradients directly on `Canvas` inside `CustomButton` and `CustomImageView`, and animates colors inside `ShowHideHistoryAnimationUtils` and `KeypadController`.

1. **Platform Entry Points (`modern` & `legacy`):**
   - [`ModernEntry.kt`](app/src/modern/java/com/expressive/oneuicalc/xposed/ModernEntry.kt) implements `XposedModule` (`io.github.libxposed.api:102.0.0`) using `META-INF/xposed/{module.prop,scope.list,java_init.list}` and `XposedService` remote preferences.
   - [`LegacyEntry.kt`](app/src/legacy/java/com/expressive/oneuicalc/xposed/LegacyEntry.kt) implements `IXposedHookLoadPackage` (`de.robv.android.xposed:api:82`) with `XSharedPreferences`.
   - Both adapt into the shared [`HookBridge.kt`](app/src/main/java/com/expressive/oneuicalc/hook/HookBridge.kt) abstraction so all hook logic lives in one shared codebase.
2. **Hook Orchestration ([`CalculatorExpressiveHooks.kt`](app/src/main/java/com/expressive/oneuicalc/hook/CalculatorExpressiveHooks.kt)):**
   - Intercepts `Resources.getColor` / `Resources.getColorStateList` for `0x7fxxxxxx` app color resources.
   - Replaces `CustomButton` & `CustomImageView` `drawButtonFrameShape`, `drawButtonFrameShapeFocusEffect`, `drawGradient`, and `drawShadow` with [`ExpressivePathFactory`](app/src/main/java/com/expressive/oneuicalc/render/ExpressivePathFactory.kt) + [`MorphAnimator`](app/src/main/java/com/expressive/oneuicalc/render/MorphAnimator.kt) (applying press transforms strictly inside frame drawing so `CustomButton.getXY()` text centering stays exact).
   - Overrides hardcoded XML backgrounds on History rows, Clear History, Unit Converter tabs, spinner pills, and `PopupWindow` dropdown menus.

---

## Project Structure

```text
ExpressiveCalc/
├── apk/                                      # Signed release APKs (modern API 102 & legacy API 82)
├── assets/                                   # README media & exported 512x512 app icon PNG
├── app/
│   ├── build.gradle.kts                      # AGP 8.7.3, Kotlin 2.2.10, Compose, modern/legacy flavors
│   └── src/
│       ├── main/java/com/expressive/oneuicalc/
│       │   ├── hook/                         # HookBridge & CalculatorExpressiveHooks
│       │   ├── model/                        # ButtonRole, ExpressiveShape, ColorPalette, ModuleConfig
│       │   ├── prefs/                        # Shared prefs contract
│       │   ├── render/                       # ExpressivePathFactory, MorphAnimator, PaletteResolver, Drawables
│       │   └── ui/                           # Jetpack Compose companion app & live calculator preview
│       ├── modern/                           # libxposed API 102 entry, META-INF/xposed metadata & RemotePrefs
│       └── legacy/                           # Xposed API 82 entry, assets/xposed_init & XSharedPreferences
├── gradle/                                   # Gradle 8.11.1 wrapper
├── build.gradle.kts
├── settings.gradle.kts
├── LICENSE                                   # MIT License
└── README.md
```

---

## Building from Source

### Prerequisites
- JDK 17+
- Android SDK Platform 36 (`compileSdk = 36`)

### Build Commands

```bash
# Build both Vector (libxposed API 102) and Legacy (Xposed API 82) release APKs
./gradlew assembleModernRelease assembleLegacyRelease

# Outputs:
# app/build/outputs/apk/modern/release/app-modern-release.apk
# app/build/outputs/apk/legacy/release/app-legacy-release.apk
```

---

## License

Released under the [MIT License](LICENSE).
