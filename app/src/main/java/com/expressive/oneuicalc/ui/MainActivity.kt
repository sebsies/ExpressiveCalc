package com.expressive.oneuicalc.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expressive.oneuicalc.hook.CalculatorExpressiveHooks
import com.expressive.oneuicalc.model.ButtonRole
import com.expressive.oneuicalc.model.ExpressivePalette
import com.expressive.oneuicalc.model.ExpressiveShape
import com.expressive.oneuicalc.model.ModuleConfig
import com.expressive.oneuicalc.model.MorphMode
import com.expressive.oneuicalc.model.PaletteRegistry
import com.expressive.oneuicalc.model.ShapePresets
import com.expressive.oneuicalc.model.ThemeMode
import com.expressive.oneuicalc.prefs.FrameworkStatus
import com.expressive.oneuicalc.prefs.PlatformPrefs
import com.expressive.oneuicalc.render.PaletteResolver
import com.expressive.oneuicalc.ui.components.CUSTOM_COLOR_SLOTS
import com.expressive.oneuicalc.ui.components.ColorSlot
import com.expressive.oneuicalc.ui.components.ColorTokenRow
import com.expressive.oneuicalc.ui.components.HexColorPickerDialog
import com.expressive.oneuicalc.ui.components.LiveCalculatorPreview
import com.expressive.oneuicalc.ui.components.ShapeBadge
import com.expressive.oneuicalc.ui.theme.ExpressiveCalcTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExpressiveCalcTheme {
                var config by remember { mutableStateOf(PlatformPrefs.loadConfig(this)) }
                var status by remember {
                    mutableStateOf(
                        FrameworkStatus(
                            isActive = false,
                            frameworkLabel = "Checking Vector status…",
                            apiLabel = "",
                            supportsRemotePrefs = false
                        )
                    )
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    PlatformPrefs.init(this@MainActivity) { st ->
                        status = st
                        config = PlatformPrefs.loadConfig(this@MainActivity)
                    }
                }

                val updateConfig: (ModuleConfig) -> Unit = { newCfg ->
                    config = newCfg
                    PlatformPrefs.saveConfig(this@MainActivity, newCfg)
                }

                ExpressiveCalcScreen(
                    config = config,
                    status = status,
                    onConfigChange = updateConfig,
                    onRestartCalculator = { restartSamsungCalculator() }
                )
            }
        }
    }

    private fun restartSamsungCalculator() {
        val pkg = CalculatorExpressiveHooks.TARGET_PACKAGE
        val restarted = try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop $pkg"))
            proc.waitFor() == 0
        } catch (_: Throwable) {
            false
        }
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
            Toast.makeText(
                this,
                if (restarted) "Restarted Samsung Calculator with new style" else "Opened Samsung Calculator (force-stop manually if needed)",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, "Samsung Calculator ($pkg) not found on this device", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveCalcScreen(
    config: ModuleConfig,
    status: FrameworkStatus,
    onConfigChange: (ModuleConfig) -> Unit,
    onRestartCalculator: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showScientificPreview by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<ColorSlot?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ExpressiveCalc", fontWeight = FontWeight.Bold)
                        Text(
                            "One UI 9 Calculator • Material Expressive",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onRestartCalculator,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Launch", style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                    label = { Text("Shapes") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.ColorLens, contentDescription = null) },
                    label = { Text("Palettes") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                    label = { Text("Tweaks") }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StatusAndMasterCard(
                    config = config,
                    status = status,
                    onToggleEnabled = { onConfigChange(config.copy(enabled = it)) }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveCalculatorPreview(
                        config = config,
                        showScientificRow = showScientificPreview
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showScientificPreview,
                            onClick = { showScientificPreview = !showScientificPreview },
                            label = { Text(if (showScientificPreview) "Hide Scientific Row" else "Show Scientific Row") }
                        )
                        OutlinedButton(onClick = onRestartCalculator) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Apply & Restart")
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    item { ShapesSection(config = config, onConfigChange = onConfigChange) }
                }
                1 -> {
                    item {
                        PalettesSection(
                            config = config,
                            onConfigChange = onConfigChange,
                            onEditSlot = { editingSlot = it }
                        )
                    }
                }
                2 -> {
                    item { TweaksSection(config = config, onConfigChange = onConfigChange) }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    editingSlot?.let { slot ->
        HexColorPickerDialog(
            title = slot.title,
            initialColor = slot.get(config.customColors),
            onDismiss = { editingSlot = null },
            onConfirm = { newColor ->
                onConfigChange(
                    config.copy(
                        paletteId = PaletteRegistry.CUSTOM.id,
                        customColors = slot.set(config.customColors, newColor)
                    )
                )
                editingSlot = null
            }
        )
    }
}

@Composable
private fun StatusAndMasterCard(
    config: ModuleConfig,
    status: FrameworkStatus,
    onToggleEnabled: (Boolean) -> Unit
) {
    val container = if (status.isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val onContainer = if (status.isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (status.isActive) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = onContainer
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = status.frameworkLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = onContainer
                    )
                    Text(
                        text = if (status.isActive) status.apiLabel else "Enable module in Vector Manager & select Samsung Calculator",
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainer.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = config.enabled, onCheckedChange = onToggleEnabled)
        }
    }
}

@Composable
private fun ShapesSection(
    config: ModuleConfig,
    onConfigChange: (ModuleConfig) -> Unit
) {
    var activeRole by remember { mutableStateOf(ButtonRole.NUMBER) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Quick Shape Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ShapePresets.ALL) { preset ->
                val isSelected = config.numberShape == preset.numberShape &&
                    config.operatorShape == preset.operatorShape &&
                    config.utilityShape == preset.utilityShape &&
                    config.equalsShape == preset.equalsShape &&
                    config.scientificShape == preset.scientificShape
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .width(196.dp)
                        .clickable {
                            onConfigChange(
                                config.copy(
                                    numberShape = preset.numberShape,
                                    operatorShape = preset.operatorShape,
                                    utilityShape = preset.utilityShape,
                                    equalsShape = preset.equalsShape,
                                    scientificShape = preset.scientificShape
                                )
                            )
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ShapeBadge(preset.numberShape, MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            ShapeBadge(preset.operatorShape, MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                            ShapeBadge(preset.utilityShape, MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
                            ShapeBadge(preset.equalsShape, MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Text(preset.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(preset.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Text("Per-Group Button Shape", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonRole.entries.forEach { role ->
                FilterChip(
                    selected = activeRole == role,
                    onClick = { activeRole = role },
                    label = { Text("${role.displayName.substringBefore(" ")} (${config.shapeFor(role).shortLabel})") }
                )
            }
        }

        val currentShape = config.shapeFor(activeRole)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(activeRole.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExpressiveShape.entries.chunked(5).forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunk.forEach { shape ->
                        val selected = shape == currentShape
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable {
                                    val updated = when (activeRole) {
                                        ButtonRole.NUMBER -> config.copy(numberShape = shape)
                                        ButtonRole.OPERATOR -> config.copy(operatorShape = shape)
                                        ButtonRole.UTILITY -> config.copy(utilityShape = shape)
                                        ButtonRole.EQUALS -> config.copy(equalsShape = shape)
                                        ButtonRole.SCIENTIFIC -> config.copy(scientificShape = shape)
                                    }
                                    onConfigChange(updated)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            ShapeBadge(
                                shape = shape,
                                fillColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = shape.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Text("Press Morph Motion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MorphMode.entries.forEach { mode ->
                val selected = config.morphMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { onConfigChange(config.copy(morphMode = mode)) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShapeBadge(
                        shape = config.equalsShape,
                        fillColor = MaterialTheme.colorScheme.primary,
                        morphMode = mode,
                        morphProgress = if (selected) 0.65f else 0f,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mode.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PalettesSection(
    config: ModuleConfig,
    onConfigChange: (ModuleConfig) -> Unit,
    onEditSlot: (ColorSlot) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Theme Brightness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = config.themeMode == mode,
                    onClick = { onConfigChange(config.copy(themeMode = mode)) },
                    label = { Text(mode.displayName) }
                )
            }
        }

        Text("Material Expressive Palettes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PaletteRegistry.ALL.forEach { palette ->
                val selected = config.paletteId == palette.id
                val previewTokens = remember(palette, config.themeMode, config.customColors) {
                    PaletteResolver.resolve(context, config.copy(paletteId = palette.id))
                }
                PaletteCard(
                    palette = palette,
                    previewTokens = previewTokens,
                    selected = selected,
                    onSelect = { onConfigChange(config.copy(paletteId = palette.id)) }
                )
            }
        }

        AnimatedVisibility(visible = config.paletteId != PaletteRegistry.CUSTOM.id) {
            Button(
                onClick = {
                    val cloned = PaletteResolver.resolve(context, config)
                    onConfigChange(
                        config.copy(
                            paletteId = PaletteRegistry.CUSTOM.id,
                            customColors = cloned
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clone Active Palette into Custom Editor")
            }
        }

        if (config.paletteId == PaletteRegistry.CUSTOM.id) {
            Text("Custom Role Colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CUSTOM_COLOR_SLOTS.forEach { slot ->
                    ColorTokenRow(
                        title = slot.title,
                        colorInt = slot.get(config.customColors),
                        onClick = { onEditSlot(slot) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteCard(
    palette: ExpressivePalette,
    previewTokens: com.expressive.oneuicalc.model.PaletteTokens,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val swatches = listOf(
        Color(previewTokens.background),
        Color(previewTokens.numberBg),
        Color(previewTokens.operatorBg),
        Color(previewTokens.utilityBg),
        Color(previewTokens.equalsBg)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(palette.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(palette.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            swatches.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun TweaksSection(
    config: ModuleConfig,
    onConfigChange: (ModuleConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Button Surface & Geometry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        SliderCard(
            title = "Press Bounce Scale",
            valueLabel = "${(config.pressScale * 100).roundToInt()}%",
            value = config.pressScale,
            range = 0.84f..1.0f,
            onValueChange = { onConfigChange(config.copy(pressScale = it)) }
        )

        SliderCard(
            title = "Key Spacing Inset",
            valueLabel = String.format("%.1f dp", config.buttonInsetDp),
            value = config.buttonInsetDp,
            range = 0f..6f,
            onValueChange = { onConfigChange(config.copy(buttonInsetDp = it)) }
        )

        SliderCard(
            title = "Button Label Scale",
            valueLabel = "${(config.textScale * 100).roundToInt()}%",
            value = config.textScale,
            range = 0.75f..1.25f,
            onValueChange = { onConfigChange(config.copy(textScale = it)) }
        )

        ToggleCard(
            title = "Expressive Press Glow",
            subtitle = "Linear gradient tonal sheen when pressing buttons",
            checked = config.showExpressiveGlow,
            onCheckedChange = { onConfigChange(config.copy(showExpressiveGlow = it)) }
        )

        ToggleCard(
            title = "Tonal Outline Border",
            subtitle = "Crisp border around each button shape (great with AMOLED)",
            checked = config.showOutline,
            onCheckedChange = { onConfigChange(config.copy(showOutline = it)) }
        )

        if (config.showOutline) {
            SliderCard(
                title = "Outline Thickness",
                valueLabel = String.format("%.1f dp", config.outlineWidthDp),
                value = config.outlineWidthDp,
                range = 0.5f..3.5f,
                onValueChange = { onConfigChange(config.copy(outlineWidthDp = it)) }
            )
        }

        ToggleCard(
            title = "Elevated Button Shadow",
            subtitle = "Soft drop shadow synced with morphing shape",
            checked = config.showShadow,
            onCheckedChange = { onConfigChange(config.copy(showShadow = it)) }
        )

        Spacer(Modifier.height(4.dp))
        Text("One UI 9 Calculator Layout & Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        ToggleCard(
            title = "Hide Unit Converter Icon",
            subtitle = "Removes the ruler button from the top handle bar",
            checked = config.hideConverterButton,
            onCheckedChange = { onConfigChange(config.copy(hideConverterButton = it)) }
        )

        ToggleCard(
            title = "Hide History Icon",
            subtitle = "Removes the clock history button from the top handle bar",
            checked = config.hideHistoryButton,
            onCheckedChange = { onConfigChange(config.copy(hideHistoryButton = it)) }
        )

        ToggleCard(
            title = "Disable Key Haptics",
            subtitle = "Silences Samsung Calculator vibration feedback on button presses",
            checked = config.disableHaptics,
            onCheckedChange = { onConfigChange(config.copy(disableHaptics = it)) }
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Target Info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Built for Samsung One UI 9 Calculator (com.sec.android.app.popupcalculator v12.6.00.35, SDK 37). Hooks CustomButton, CustomImageView, KeypadController, ShowHideHistoryAnimationUtils, CustomChipBackground, HistoryController, and Resources.getColor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
