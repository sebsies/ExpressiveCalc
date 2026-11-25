package com.expressive.oneuicalc.hook

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.view.WindowCompat
import com.expressive.oneuicalc.model.ButtonRole
import com.expressive.oneuicalc.model.ModuleConfig
import com.expressive.oneuicalc.model.MorphMode
import com.expressive.oneuicalc.model.PaletteTokens
import com.expressive.oneuicalc.render.ExpressivePathFactory
import com.expressive.oneuicalc.render.ExpressiveShapeDrawable
import com.expressive.oneuicalc.render.ExpressiveTabIndicatorDrawable
import com.expressive.oneuicalc.render.MorphAnimator
import com.expressive.oneuicalc.render.PaletteResolver
import java.lang.reflect.Field
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class CalculatorExpressiveHooks(
    private val bridge: HookBridge,
    private val configSupplier: () -> ModuleConfig
) {
    companion object {
        const val TARGET_PACKAGE = "com.sec.android.app.popupcalculator"

        private const val CLS_CALCULATOR = "$TARGET_PACKAGE.Calculator"
        private const val CLS_CONVERTER_ACT = "$TARGET_PACKAGE.converter.controller.NewUnitConverterActivity"
        private const val CLS_MORTGAGE_RESULT_ACT = "$TARGET_PACKAGE.converter.mortgage.controller.MortgageResultActivity"
        private const val CLS_BASE_MORTGAGE_ACT = "$TARGET_PACKAGE.converter.mortgage.controller.BaseMortgageActivity"
        private const val CLS_MORTGAGE_DETAIL_ACT = "$TARGET_PACKAGE.converter.mortgage.controller.MortgageDetailActivity"
        private const val CLS_CUSTOM_BUTTON = "$TARGET_PACKAGE.calc.view.CustomButton"
        private const val CLS_CUSTOM_IMAGE = "$TARGET_PACKAGE.calc.view.CustomImageView"
        private const val CLS_CHIP_BG = "$TARGET_PACKAGE.calc.view.CustomChipBackground"
        private const val CLS_KEYPAD_CTRL = "$TARGET_PACKAGE.calc.controller.KeypadController"
        private const val CLS_HANDLE_CTRL = "$TARGET_PACKAGE.calc.controller.HandleButtonsController"
        private const val CLS_HISTORY_CTRL = "$TARGET_PACKAGE.calc.controller.HistoryController"
        private const val CLS_HISTORY_ADAPTER = "$TARGET_PACKAGE.calc.controller.HistoryController\$HistoryAdapter"
        private const val CLS_CONVERTER_SPINNER_ADAPTER = "$TARGET_PACKAGE.converter.controller.ConverterSpinnerAdapter"
        private const val CLS_MORTGAGE_SPINNER_ADAPTER = "$TARGET_PACKAGE.converter.mortgage.view.UnitSpinnerAdapter"
        private const val CLS_CONVERTER_KEYPAD_CTRL = "$TARGET_PACKAGE.converter.controller.ConverterKeypadController"
        private const val CLS_CONVERTER_LAYOUT_CTRL = "$TARGET_PACKAGE.converter.controller.ConverterLayoutController"
        private const val CLS_CONVERTER_UTILS = "$TARGET_PACKAGE.converter.utils.ConverterUtils"
        private const val CLS_BASE_CONVERTER_FRAG = "$TARGET_PACKAGE.converter.controller.BaseUnitConverterFragment"
        private const val CLS_TAB_LAYOUT = "com.google.android.material.tabs.TabLayout"
        private const val CLS_ANIM_UTILS = "$TARGET_PACKAGE.calc.utils.ShowHideHistoryAnimationUtils"
        private const val CLS_COMMON_UTILS = "$TARGET_PACKAGE.common.utils.CommonUtils"
    }

    @Volatile
    private var appContext: Context? = null

    private val resNameCache = ConcurrentHashMap<Int, String>()
    private val isResolvingColor = ThreadLocal.withInitial { false }
    private val tabIndicators = WeakHashMap<ViewGroup, ExpressiveTabIndicatorDrawable>()

    // Reusable per-thread drawing objects (UI thread only, kept ThreadLocal for safety)
    private val drawPath = ThreadLocal.withInitial { Path() }
    private val drawBounds = ThreadLocal.withInitial { RectF() }
    private val fillPaint = ThreadLocal.withInitial { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val strokePaint = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    }
    private val shadowPaint = ThreadLocal.withInitial { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val glowPaint = ThreadLocal.withInitial { Paint(Paint.ANTI_ALIAS_FLAG) }

    // Reflected field caches
    private var btnFrameAlphaField: Field? = null
    private var btnTextColorField: Field? = null
    private var btnTextAlphaField: Field? = null
    private var imgFrameAlphaField: Field? = null
    private var imgTextColorField: Field? = null

    fun install(classLoader: ClassLoader) {
        bridge.log("Installing One UI 9 Calculator Material Expressive hooks")
        hookResourcesGetColor()
        hookPopupWindows()
        hookActivities(classLoader)
        hookCustomButton(classLoader)
        hookCustomImageView(classLoader)
        hookKeypadController(classLoader)
        hookShowHideAnimations(classLoader)
        hookHandleButtonsController(classLoader)
        hookHistoryController(classLoader)
        hookUnitConverter(classLoader)
        hookChipBackground(classLoader)
        hookCommonUtils(classLoader)
    }

    private fun currentConfig(): ModuleConfig = try {
        configSupplier()
    } catch (t: Throwable) {
        bridge.logError("Failed to read config", t)
        ModuleConfig()
    }

    private fun resolveTokens(context: Context?): PaletteTokens {
        val cfg = currentConfig()
        val ctx = context ?: appContext
        return if (ctx != null) {
            PaletteResolver.resolve(ctx, cfg)
        } else {
            com.expressive.oneuicalc.model.PaletteRegistry.findById(cfg.paletteId).dark
        }
    }

    private fun hookResourcesGetColor() {
        val resolveAppColor: (Resources, Int) -> Int? = { res, resId ->
            if ((resId ushr 24) != 0x7f) {
                null
            } else {
                val entryName = resNameCache.getOrPut(resId) {
                    try {
                        res.getResourceEntryName(resId)
                    } catch (_: Throwable) {
                        ""
                    }
                }
                if (entryName.isEmpty()) {
                    null
                } else {
                    isResolvingColor.set(true)
                    try {
                        val tokens = resolveTokens(appContext)
                        val isDark = appContext?.let { PaletteResolver.isDarkEffective(it, config = currentConfig()) } ?: true
                        mapResourceColor(entryName, tokens, isDark)
                    } finally {
                        isResolvingColor.set(false)
                    }
                }
            }
        }

        val colorCallback = object : HookCallback {
            override fun before(param: HookContext) {
                if (isResolvingColor.get() == true) return
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val res = param.thisObject as? Resources ?: return
                val resId = param.args[0] as? Int ?: return
                val mapped = resolveAppColor(res, resId)
                if (mapped != null) {
                    param.result = mapped
                }
            }
        }

        val colorStateListCallback = object : HookCallback {
            override fun before(param: HookContext) {
                if (isResolvingColor.get() == true) return
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val res = param.thisObject as? Resources ?: return
                val resId = param.args[0] as? Int ?: return
                val mapped = resolveAppColor(res, resId)
                if (mapped != null) {
                    param.result = ColorStateList.valueOf(mapped)
                }
            }
        }

        val bootLoader = Resources::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
        bridge.hookMethod(
            Resources::class.java.name,
            bootLoader,
            "getColor",
            Int::class.javaPrimitiveType!!,
            Resources.Theme::class.java,
            callback = colorCallback
        )
        bridge.hookMethod(
            Resources::class.java.name,
            bootLoader,
            "getColor",
            Int::class.javaPrimitiveType!!,
            callback = colorCallback
        )
        bridge.hookMethod(
            Resources::class.java.name,
            bootLoader,
            "getColorStateList",
            Int::class.javaPrimitiveType!!,
            Resources.Theme::class.java,
            callback = colorStateListCallback
        )
        bridge.hookMethod(
            Resources::class.java.name,
            bootLoader,
            "getColorStateList",
            Int::class.javaPrimitiveType!!,
            callback = colorStateListCallback
        )
    }

    private fun mapResourceColor(entryName: String, t: PaletteTokens, isDark: Boolean): Int? {
        return when (entryName) {
            // App & window background
            "calc_edit_bg",
            "windowBackground",
            "navigationBarColor",
            "status_bar_background_color",
            "actionbar_color_bg",
            "action_bar_title_bg",
            "tab_layout_bg",
            "sesl_round_and_bgcolor_light",
            "sesl_round_and_bgcolor_dark",
            "sesl_colorful_color_background" -> t.background

            // Formula & primary text
            "number",
            "actionbar_text_color",
            "title_actionbar",
            "title_button_text_color",
            "history_expression",
            "history_normal",
            "history_text_no_history",
            "converter_area_text",
            "converter_result_text_color",
            "converter_spinner_default",
            "converter_spinner_icon",
            "converter_list_item_text",
            "converter_sub_total_text",
            "converter_sub_total_text_tablet",
            "converter_sub_total_edittext_color",
            "converter_tip_amount_text_color",
            "converter_tip_btn_done",
            "converter_tip_icon",
            "converter_tip_number",
            "converter_tip_number_picker_dialog_title_text",
            "converter_tip_number_picker_text",
            "history_copy_share_item_text_color",
            "dialog_button_color",
            "dialog_alert_description" -> t.formulaText

            // Live result & history result
            "result",
            "result_scientific",
            "history_result",
            "history_result_scientific",
            "converter_spinner_select",
            "converter_text_color",
            "tw_dropdown_ic_check",
            "number_picker_scrolling" -> t.resultText

            // Formula operators, parentheses & cursor
            "symbols",
            "symbols_scientific",
            "calc_cursor",
            "calc_cursor_scientific",
            "converter_cursor" -> t.symbolAccent

            // Button backgrounds
            "calc_keypad_number_button_bg" -> t.numberBg
            "calc_keypad_operation_button_bg" -> t.operatorBg
            "calc_keypad_symbol_button_bg" -> t.utilityBg
            "calc_keypad_scientific_button_bg" -> t.scientificBg
            "keypad_equal_bg",
            "keypad_equal_scientific_bg",
            "converter_keypad_symbol_button_bg",
            "mtg_calculate_btn_stroke_color" -> t.equalsBg

            // Button foregrounds
            "keypad_number" -> t.numberText
            "keypad_operation",
            "keypad_symbol" -> t.operatorText
            "keypad_clear",
            "back_key",
            "back_key_unit_converter",
            "converter_btn_cursor_up",
            "primary_dark_color" -> t.utilityText
            "keypad_equal" -> t.equalsText
            "keypad_scientific",
            "keypad_func" -> t.scientificText

            // Gradient glow
            "keypad_normal_gradient_start",
            "keypad_scientific_gradient_start",
            "keypad_clear_gradient_start",
            "converter_keypad_normal_gradient_start" -> t.glowStart

            "keypad_normal_gradient_end",
            "keypad_scientific_gradient_end",
            "keypad_clear_gradient_end",
            "converter_keypad_normal_gradient_end" -> t.glowEnd

            // History chrome & popups
            "history_btn_bg",
            "dropdown_spinner_background",
            "converter_tip_number_layout_bg",
            "converter_tip_number_picker_dialog_bg",
            "history_copy_share_background" -> t.numberBg
            "history_btn_clear" -> t.utilityText
            "history_list_divider",
            "keypad_divider",
            "converter_edit_divider",
            "converter_spinner_dropdown_stroke",
            "history_scroll_bar_bg" -> t.outlineColor
            "history_item_focus",
            "ripple_effect" -> withAlpha(t.equalsBg, 46)
            "calc_btn_background_color_focus" -> t.formulaText

            // Tabs
            "calc_tab_title_selected" -> t.equalsText
            "calc_tab_title_default",
            "converter_list_item_unit_text",
            "converter_list_item_value_text" -> withAlpha(t.formulaText, if (isDark) 175 else 165)
            "border_tab_layout_selected" -> t.equalsBg

            // Suggestion chips
            "chip_background_color" -> t.numberBg
            "chip_text_color" -> t.formulaText

            else -> null
        }
    }

    private fun pressedOnlyColorStateList(pressedColor: Int): ColorStateList {
        return ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf()),
            intArrayOf(pressedColor, Color.TRANSPARENT)
        )
    }

    private fun createPopupMenuContainerDrawable(
        context: Context,
        cfg: ModuleConfig,
        tokens: PaletteTokens
    ): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f * density
            setColor(withAlpha(tokens.numberBg, 255))
            if (cfg.showOutline && cfg.outlineWidthDp > 0f) {
                setStroke((cfg.outlineWidthDp * density).toInt().coerceAtLeast(1), withAlpha(tokens.outlineColor, 110))
            }
        }
    }

    private fun applyPopupClipOutline(view: View, radiusDp: Float = 20f) {
        val radiusPx = radiusDp * view.resources.displayMetrics.density
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, radiusPx)
            }
        }
        view.clipToOutline = true
    }

    private fun clearSemBlurInfo(view: View) {
        try {
            val cls = Class.forName("android.view.SemBlurInfo", false, view.javaClass.classLoader)
            View::class.java.getMethod("semSetBlurInfo", cls).invoke(view, null)
        } catch (_: Throwable) {
        }
    }

    private fun styleDropdownListView(listView: android.widget.ListView, cfg: ModuleConfig, tokens: PaletteTokens) {
        val ctx = listView.context ?: return
        clearSemBlurInfo(listView)
        listView.background = createPopupMenuContainerDrawable(ctx, cfg, tokens)
        listView.backgroundTintList = null
        listView.divider = null
        listView.dividerHeight = 0
        listView.selector = RippleDrawable(
            pressedOnlyColorStateList(withAlpha(tokens.equalsBg, 44)),
            null,
            ColorDrawable(Color.WHITE)
        )
        listView.overScrollMode = View.OVER_SCROLL_NEVER
        listView.setPadding(0, 0, 0, 0)
        listView.clipToPadding = true
        applyPopupClipOutline(listView, 20f)
    }

    private fun hookPopupWindows() {
        val bootLoader = PopupWindow::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
        val dropDownHook = object : HookCallback {
            override fun before(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val popup = param.thisObject as? PopupWindow ?: return
                val content = popup.contentView ?: return
                val ctx = content.context ?: return
                if (ctx.packageName != TARGET_PACKAGE && appContext == null) return
                popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }

            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val popup = param.thisObject as? PopupWindow ?: return
                val content = popup.contentView ?: return
                val ctx = content.context ?: return
                if (ctx.packageName != TARGET_PACKAGE && appContext == null) return
                val tokens = resolveTokens(ctx)

                if (content is android.widget.ListView) {
                    styleDropdownListView(content, cfg, tokens)
                }

                // Clear any Samsung white PopupBackgroundView drawable sitting behind contentView
                var p: Any? = content.parent
                var depth = 0
                while (p is View && depth < 3) {
                    clearSemBlurInfo(p)
                    p.background = null
                    p.backgroundTintList = null
                    p = p.parent
                    depth++
                }
            }
        }
        bridge.hookAllMethods(PopupWindow::class.java.name, bootLoader, "showAsDropDown", dropDownHook)
    }

    private fun hookActivities(classLoader: ClassLoader) {
        val activityHook = object : HookCallback {
            override fun after(param: HookContext) {
                val activity = param.thisObject as? Activity ?: return
                appContext = activity.applicationContext ?: activity
                val cfg = currentConfig()
                if (!cfg.enabled) return
                applyActivityStyling(activity, cfg)
            }
        }

        for (actCls in listOf(CLS_CALCULATOR, CLS_CONVERTER_ACT, CLS_MORTGAGE_RESULT_ACT, CLS_BASE_MORTGAGE_ACT, CLS_MORTGAGE_DETAIL_ACT)) {
            bridge.hookAllMethods(actCls, classLoader, "onCreate", activityHook)
            bridge.hookAllMethods(actCls, classLoader, "onResume", activityHook)
            bridge.hookAllMethods(actCls, classLoader, "onConfigurationChanged", activityHook)
        }
    }

    private fun applyActivityStyling(activity: Activity, cfg: ModuleConfig) {
        try {
            val tokens = PaletteResolver.resolve(activity, cfg)
            val isLightBg = calculateLuminance(tokens.background) > 0.5f

            activity.window?.let { window ->
                window.decorView.setBackgroundColor(tokens.background)
                @Suppress("DEPRECATION")
                window.statusBarColor = tokens.background
                @Suppress("DEPRECATION")
                window.navigationBarColor = tokens.background
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = isLightBg
                    isAppearanceLightNavigationBars = isLightBg
                }
            }

            val pkg = activity.packageName
            val res = activity.resources

            for (rootIdName in listOf(
                "calc_layout_main_group",
                "converter_layout_main_group",
                "converter_main_group",
                "action_bar_layout",
                "calc_history_view_group",
                "converter_keypad",
                "calc_keypad"
            )) {
                val id = res.getIdentifier(rootIdName, "id", pkg)
                if (id != 0) {
                    activity.findViewById<View>(id)?.apply {
                        setBackgroundColor(tokens.background)
                        backgroundTintList = null
                    }
                }
            }

            // Formula EditText
            val formulaId = res.getIdentifier("calc_edt_formula", "id", pkg)
            if (formulaId != 0) {
                activity.findViewById<TextView>(formulaId)?.let { edt ->
                    edt.setTextColor(tokens.formulaText)
                    try {
                        val setNumColor = edt.javaClass.superclass?.getDeclaredMethod("setNumberColor", Int::class.javaPrimitiveType)
                        setNumColor?.isAccessible = true
                        setNumColor?.invoke(edt, tokens.formulaText)
                    } catch (_: Throwable) {
                    }
                    edt.invalidate()
                }
            }

            // Live Result TextView
            val resultId = res.getIdentifier("calc_tv_result", "id", pkg)
            if (resultId != 0) {
                activity.findViewById<TextView>(resultId)?.setTextColor(tokens.resultText)
            }

            // Rad indicator
            val degRadId = res.getIdentifier("calc_tv_deg_rad", "id", pkg)
            if (degRadId != 0) {
                activity.findViewById<TextView>(degRadId)?.setTextColor(tokens.symbolAccent)
            }

            // Unit Converter title & back button
            val convTitleId = res.getIdentifier("converter_text_title", "id", pkg)
            if (convTitleId != 0) {
                activity.findViewById<TextView>(convTitleId)?.setTextColor(tokens.formulaText)
            }
            val convBackId = res.getIdentifier("converter_btn_back", "id", pkg)
            if (convBackId != 0) {
                activity.findViewById<ImageView>(convBackId)?.imageTintList = ColorStateList.valueOf(tokens.formulaText)
            }
            val convTabDivId = res.getIdentifier("converter_divider_under_tab", "id", pkg)
            if (convTabDivId != 0) {
                activity.findViewById<View>(convTabDivId)?.setBackgroundColor(tokens.outlineColor)
            }

            // Clear history button if already inflated
            val clearHistId = res.getIdentifier("calc_history_btn_clear", "id", pkg)
            if (clearHistId != 0) {
                activity.findViewById<Button>(clearHistId)?.let { btn ->
                    styleClearHistoryButton(btn, cfg, tokens)
                }
            }

            // Handle toolbar buttons
            styleHandleButtons(activity, cfg, tokens)

            // Refresh all buttons & chrome in the view hierarchy
            val root = activity.window?.decorView
            if (root != null) {
                styleConverterHierarchy(root, cfg, tokens)
                invalidateCustomButtons(root)
            }
        } catch (t: Throwable) {
            bridge.logError("applyActivityStyling failed", t)
        }
    }

    private fun styleHandleButtons(activity: Activity, cfg: ModuleConfig, tokens: PaletteTokens) {
        val pkg = activity.packageName
        val res = activity.resources
        val tint = ColorStateList.valueOf(tokens.formulaText)

        val convId = res.getIdentifier("calc_handle_btn_converter", "id", pkg)
        if (convId != 0) {
            activity.findViewById<ImageView>(convId)?.apply {
                visibility = if (cfg.hideConverterButton) View.GONE else View.VISIBLE
                imageTintList = tint
            }
        }
        val histId = res.getIdentifier("calc_handle_btn_history", "id", pkg)
        if (histId != 0) {
            activity.findViewById<ImageView>(histId)?.apply {
                visibility = if (cfg.hideHistoryButton) View.GONE else View.VISIBLE
                imageTintList = tint
            }
        }
        val rotId = res.getIdentifier("calc_handle_btn_rotation", "id", pkg)
        if (rotId != 0) {
            activity.findViewById<ImageView>(rotId)?.apply {
                imageTintList = tint
            }
        }
    }

    private fun invalidateCustomButtons(view: View) {
        val name = view.javaClass.name
        if (name == CLS_CUSTOM_BUTTON || name == CLS_CUSTOM_IMAGE) {
            if (view.background != null) {
                view.background = null
            }
            view.invalidate()
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                invalidateCustomButtons(view.getChildAt(i))
            }
        }
    }

    private fun hookCustomButton(classLoader: ClassLoader) {
        val cls = try {
            Class.forName(CLS_CUSTOM_BUTTON, false, classLoader)
        } catch (t: Throwable) {
            bridge.logError("Cannot find CustomButton", t)
            return
        }
        btnFrameAlphaField = findFieldSafe(cls, "buttonFrameAlpha")
        btnTextColorField = findFieldSafe(cls, "textColor")
        btnTextAlphaField = findFieldSafe(cls, "textAlpha")

        // Skip default gradient & shadow bitmaps
        val skipWhenEnabled = object : HookCallback {
            override fun before(param: HookContext) {
                if (currentConfig().enabled) {
                    param.result = null
                }
            }
        }
        bridge.hookMethod(CLS_CUSTOM_BUTTON, classLoader, "drawGradient", Canvas::class.java, callback = skipWhenEnabled)
        bridge.hookMethod(CLS_CUSTOM_BUTTON, classLoader, "drawShadow", Canvas::class.java, callback = skipWhenEnabled)

        // Replace drawButtonFrameShape with Material 3 Expressive path rendering
        bridge.hookMethod(CLS_CUSTOM_BUTTON, classLoader, "drawButtonFrameShape", Canvas::class.java, callback = object : HookCallback {
            override fun before(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val view = param.thisObject as? View ?: return
                val canvas = param.args[0] as? Canvas ?: return
                val alpha = getIntField(btnFrameAlphaField, view, 255)
                drawExpressiveButtonShape(view, canvas, cfg, frameAlpha = alpha, isFocusOverlay = false)
                param.result = null
            }
        })

        // Replace focus overlay shape so keyboard/dpad focus matches the expressive shape
        bridge.hookMethod(CLS_CUSTOM_BUTTON, classLoader, "drawButtonFrameShapeFocusEffect", Canvas::class.java, callback = object : HookCallback {
            override fun before(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val view = param.thisObject as? View ?: return
                val canvas = param.args[0] as? Canvas ?: return
                drawExpressiveButtonShape(view, canvas, cfg, frameAlpha = 51, isFocusOverlay = true)
                param.result = null
            }
        })

        // Hook onDraw to enforce palette textColor without scaling Canvas before getClipBounds()
        bridge.hookMethod(CLS_CUSTOM_BUTTON, classLoader, "onDraw", Canvas::class.java, callback = object : HookCallback {
            override fun before(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val view = param.thisObject as? TextView ?: return

                appContext = view.context.applicationContext ?: view.context
                val role = roleForView(view)
                val tokens = resolveTokens(view.context)
                val baseFg = PaletteResolver.fgForRole(tokens, role)
                val fg = if (view.isEnabled) baseFg else withAlpha(baseFg, 115)
                setIntField(btnTextColorField, view, fg)
            }
        })

        // Also handle touch events directly on CustomButton (covers Converter keypad too!)
        bridge.hookMethod(
            View::class.java.name,
            classLoader,
            "onTouchEvent",
            MotionEvent::class.java,
            callback = object : HookCallback {
                override fun before(param: HookContext) {
                    val v = param.thisObject as? View ?: return
                    val clsName = v.javaClass.name
                    if (clsName != CLS_CUSTOM_BUTTON && clsName != CLS_CUSTOM_IMAGE) return
                    val cfg = currentConfig()
                    if (!cfg.enabled) return
                    val event = param.args[0] as? MotionEvent ?: return
                    MorphAnimator.onTouch(v, event)
                }
            }
        )
    }

    private fun hookCustomImageView(classLoader: ClassLoader) {
        val cls = try {
            Class.forName(CLS_CUSTOM_IMAGE, false, classLoader)
        } catch (t: Throwable) {
            bridge.logError("Cannot find CustomImageView", t)
            return
        }
        imgFrameAlphaField = findFieldSafe(cls, "buttonFrameAlpha")
        imgTextColorField = findFieldSafe(cls, "textColor")

        val skipWhenEnabled = object : HookCallback {
            override fun before(param: HookContext) {
                if (currentConfig().enabled) {
                    param.result = null
                }
            }
        }
        bridge.hookMethod(CLS_CUSTOM_IMAGE, classLoader, "drawGradient", Canvas::class.java, callback = skipWhenEnabled)
        bridge.hookMethod(CLS_CUSTOM_IMAGE, classLoader, "drawShadow", Canvas::class.java, callback = skipWhenEnabled)

        bridge.hookMethod(CLS_CUSTOM_IMAGE, classLoader, "drawButtonFrameShape", Canvas::class.java, callback = object : HookCallback {
            override fun before(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val view = param.thisObject as? View ?: return
                val canvas = param.args[0] as? Canvas ?: return
                val alpha = getIntField(imgFrameAlphaField, view, 255)
                drawExpressiveButtonShape(view, canvas, cfg, frameAlpha = alpha, isFocusOverlay = false)
                param.result = null
            }
        })

        bridge.hookMethod(CLS_CUSTOM_IMAGE, classLoader, "drawButtonFrameShapeFocusEffect", Canvas::class.java, callback = object : HookCallback {
            override fun before(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val view = param.thisObject as? View ?: return
                val canvas = param.args[0] as? Canvas ?: return
                drawExpressiveButtonShape(view, canvas, cfg, frameAlpha = 51, isFocusOverlay = true)
                param.result = null
            }
        })

        bridge.hookMethod(CLS_CUSTOM_IMAGE, classLoader, "onDraw", Canvas::class.java, callback = object : HookCallback {
            override fun before(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val view = param.thisObject as? ImageView ?: return

                appContext = view.context.applicationContext ?: view.context
                val role = roleForView(view)
                val tokens = resolveTokens(view.context)
                val fg = PaletteResolver.fgForRole(tokens, role)
                val frameAlpha = getIntField(imgFrameAlphaField, view, 255)
                val enabledAlpha = if (view.isEnabled) 255 else 128
                val effectiveFg = Color.argb(
                    (frameAlpha * enabledAlpha * Color.alpha(fg)) / (255 * 255),
                    Color.red(fg),
                    Color.green(fg),
                    Color.blue(fg)
                )
                view.clearColorFilter()
                setIntField(imgTextColorField, view, effectiveFg)
            }
        })
    }

    private fun applyPressTransform(view: View, canvas: Canvas, cfg: ModuleConfig, progress: Float) {
        val w = view.width.toFloat()
        val h = view.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val p = progress.coerceIn(0f, 1.25f)
        if (p <= 0.001f) return

        val scale = 1f - (1f - cfg.pressScale) * p
        val cx = w / 2f
        val cy = h / 2f
        canvas.scale(scale, scale, cx, cy)

        if (cfg.morphMode == MorphMode.ROTATE_AND_MORPH) {
            canvas.rotate(14f * p, cx, cy)
        }
    }

    private fun drawExpressiveButtonShape(
        view: View,
        canvas: Canvas,
        cfg: ModuleConfig,
        frameAlpha: Int,
        isFocusOverlay: Boolean
    ) {
        val w = view.width.toFloat()
        val h = view.height.toFloat()
        if (w <= 1f || h <= 1f || frameAlpha <= 0) return

        val density = view.resources.displayMetrics.density
        val inset = cfg.buttonInsetDp * density
        val bounds = drawBounds.get()!!
        bounds.set(inset, inset, (w - inset).coerceAtLeast(inset + 2f), (h - inset).coerceAtLeast(inset + 2f))

        val role = roleForView(view)
        val restingShape = cfg.shapeFor(role)
        val progress = MorphAnimator.getProgress(view)
        val path = drawPath.get()!!
        ExpressivePathFactory.buildPath(path, bounds, restingShape, cfg.morphMode, progress)

        val tokens = resolveTokens(view.context)
        val baseColor = if (isFocusOverlay) tokens.formulaText else PaletteResolver.bgForRole(tokens, role)
        val finalAlpha = ((frameAlpha.coerceIn(0, 255) * Color.alpha(baseColor)) / 255)

        val saveTransform = canvas.save()
        try {
            applyPressTransform(view, canvas, cfg, progress)

            if (!isFocusOverlay && cfg.showShadow && frameAlpha > 20) {
                val sp = shadowPaint.get()!!
                sp.style = Paint.Style.FILL
                sp.color = Color.argb((44 * frameAlpha) / 255, 0, 0, 0)
                sp.maskFilter = BlurMaskFilter(4f * density, BlurMaskFilter.Blur.NORMAL)
                val saveShadow = canvas.save()
                canvas.translate(0f, 2f * density)
                canvas.drawPath(path, sp)
                canvas.restoreToCount(saveShadow)
            }

            val fp = fillPaint.get()!!
            fp.style = Paint.Style.FILL
            fp.shader = null
            fp.color = baseColor
            fp.alpha = finalAlpha
            canvas.drawPath(path, fp)

            // Subtle expressive press glow / tonal highlight while pressed
            if (!isFocusOverlay && cfg.showExpressiveGlow && progress > 0.01f) {
                val gp = glowPaint.get()!!
                gp.style = Paint.Style.FILL
                val glowAlpha = (68 * progress.coerceIn(0f, 1f) * frameAlpha / 255f).toInt().coerceIn(0, 120)
                gp.shader = LinearGradient(
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    withAlpha(tokens.glowStart, glowAlpha),
                    withAlpha(tokens.glowEnd, glowAlpha),
                    Shader.TileMode.CLAMP
                )
                canvas.drawPath(path, gp)
            }

            if (!isFocusOverlay && cfg.showOutline && cfg.outlineWidthDp > 0f) {
                val stp = strokePaint.get()!!
                val strokePx = cfg.outlineWidthDp * density
                stp.strokeWidth = strokePx
                val outlineBase = tokens.outlineColor
                stp.color = outlineBase
                stp.alpha = ((frameAlpha * Color.alpha(outlineBase)) / 255).coerceIn(0, 255)
                canvas.drawPath(path, stp)
            }
        } finally {
            canvas.restoreToCount(saveTransform)
        }
    }

    private fun hookKeypadController(classLoader: ClassLoader) {
        bridge.hookMethod(
            CLS_KEYPAD_CTRL,
            classLoader,
            "onTouch",
            View::class.java,
            MotionEvent::class.java,
            callback = object : HookCallback {
                override fun before(param: HookContext) {
                    val cfg = currentConfig()
                    if (!cfg.enabled) return
                    val v = param.args[0] as? View ?: return
                    val ev = param.args[1] as? MotionEvent ?: return
                    MorphAnimator.onTouch(v, ev)
                }
            }
        )

        // Text scaling on keypad buttons
        bridge.hookMethod(CLS_KEYPAD_CTRL, classLoader, "initControl", callback = object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled || cfg.textScale == 1.0f) return
                val ctrl = param.thisObject ?: return
                val ctxField = findFieldSafe(ctrl.javaClass, "mContext")
                val activity = ctxField?.get(ctrl) as? Activity ?: return
                val root = activity.window?.decorView ?: return
                scaleButtonTextSizes(root, cfg.textScale)
            }
        })
    }

    private fun scaleButtonTextSizes(view: View, scale: Float) {
        if (view.javaClass.name == CLS_CUSTOM_BUTTON && view is Button) {
            val scaled = view.textSize * scale
            if (scaled > 4f) {
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, scaled)
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                scaleButtonTextSizes(view.getChildAt(i), scale)
            }
        }
    }

    private fun hookShowHideAnimations(classLoader: ClassLoader) {
        val skipTouchColorAnim = object : HookCallback {
            override fun before(param: HookContext) {
                if (currentConfig().enabled) {
                    param.result = null
                }
            }
        }
        bridge.hookAllMethods(CLS_ANIM_UTILS, classLoader, "showGradientAnimation", skipTouchColorAnim)
        bridge.hookAllMethods(CLS_ANIM_UTILS, classLoader, "hideGradientAnimation", skipTouchColorAnim)
    }

    private fun hookHandleButtonsController(classLoader: ClassLoader) {
        bridge.hookMethod(
            CLS_HANDLE_CTRL,
            classLoader,
            "initButton",
            ImageView::class.java,
            callback = object : HookCallback {
                override fun after(param: HookContext) {
                    val cfg = currentConfig()
                    if (!cfg.enabled) return
                    val iv = param.args[0] as? ImageView ?: return
                    val tokens = resolveTokens(iv.context)
                    iv.imageTintList = ColorStateList.valueOf(tokens.formulaText)
                    val entryName = safeEntryName(iv)
                    if (entryName == "calc_handle_btn_converter" && cfg.hideConverterButton) {
                        iv.visibility = View.GONE
                    } else if (entryName == "calc_handle_btn_history" && cfg.hideHistoryButton) {
                        iv.visibility = View.GONE
                    }
                }
            }
        )
    }

    private fun hookHistoryController(classLoader: ClassLoader) {
        bridge.hookMethod(CLS_HISTORY_CTRL, classLoader, "initControl", callback = object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val ctrl = param.thisObject ?: return
                val ctx = findFieldSafe(ctrl.javaClass, "mContext")?.get(ctrl) as? Context ?: return
                val tokens = resolveTokens(ctx)
                val historyView = findFieldSafe(ctrl.javaClass, "mHistoryView")?.get(ctrl) as? View
                historyView?.let { hv ->
                    hv.setBackgroundColor(tokens.background)
                    val res = ctx.resources
                    val pkg = ctx.packageName
                    val divId = res.getIdentifier("calc_history_divider", "id", pkg)
                    if (divId != 0) hv.findViewById<View>(divId)?.setBackgroundColor(tokens.outlineColor)
                    val noHistId = res.getIdentifier("calc_history_no_history", "id", pkg)
                    if (noHistId != 0) hv.findViewById<TextView>(noHistId)?.setTextColor(tokens.formulaText)
                }
                val listView = findFieldSafe(ctrl.javaClass, "mListView")?.get(ctrl) as? View
                listView?.setBackgroundColor(tokens.background)
                val clearBtn = findFieldSafe(ctrl.javaClass, "mClearHistory")?.get(ctrl) as? Button
                clearBtn?.let { styleClearHistoryButton(it, cfg, tokens) }
            }
        })

        // Remove Samsung's pitch-black @drawable/ripple_effect_history_item background behind each history row
        bridge.hookAllMethods(CLS_HISTORY_ADAPTER, classLoader, "getView", object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val row = param.result as? View ?: return
                val ctx = row.context
                val tokens = resolveTokens(ctx)
                val pkg = ctx.packageName
                val res = ctx.resources

                row.setBackgroundColor(Color.TRANSPARENT)

                val rippleColor = ColorStateList.valueOf(withAlpha(tokens.equalsBg, 44))
                val formulaId = res.getIdentifier("calc_history_item_formula", "id", pkg)
                val resultId = res.getIdentifier("calc_history_item_result", "id", pkg)
                val degRadId = res.getIdentifier("calc_history_item_deg_rad", "id", pkg)

                if (formulaId != 0) {
                    row.findViewById<TextView>(formulaId)?.apply {
                        setTextColor(tokens.formulaText)
                        background = RippleDrawable(rippleColor, null, ColorDrawable(Color.WHITE))
                        (parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
                if (resultId != 0) {
                    row.findViewById<TextView>(resultId)?.apply {
                        setTextColor(tokens.resultText)
                        background = RippleDrawable(rippleColor, null, ColorDrawable(Color.WHITE))
                        (parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
                if (degRadId != 0) {
                    row.findViewById<TextView>(degRadId)?.apply {
                        setTextColor(tokens.symbolAccent)
                        setBackgroundColor(Color.TRANSPARENT)
                    }
                }
            }
        })
    }

    private fun styleClearHistoryButton(btn: Button, cfg: ModuleConfig, tokens: PaletteTokens) {
        val density = btn.resources.displayMetrics.density
        btn.setTextColor(tokens.utilityText)
        btn.background = ExpressiveShapeDrawable.ripple(
            shape = cfg.shapeFor(ButtonRole.UTILITY),
            fillColor = tokens.utilityBg,
            rippleColor = withAlpha(tokens.utilityText, 48),
            strokeColor = if (cfg.showOutline) tokens.outlineColor else Color.TRANSPARENT,
            strokeWidthPx = if (cfg.showOutline) cfg.outlineWidthDp * density else 0f
        )
    }

    private fun hookUnitConverter(classLoader: ClassLoader) {
        // Restyle Unit Converter category tabs & animate Material Expressive sliding pill
        val setupTabHook = object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val activity = param.thisObject as? Activity ?: return
                val tokens = resolveTokens(activity)
                styleConverterTabsInActivity(activity, cfg, tokens, selectedIndex = null, animate = false)
            }
        }
        val selectTabHook = object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val activity = param.thisObject as? Activity ?: return
                val idx = param.args.getOrNull(0) as? Int
                val tokens = resolveTokens(activity)
                styleConverterTabsInActivity(activity, cfg, tokens, selectedIndex = idx, animate = true)
            }
        }
        bridge.hookAllMethods(CLS_CONVERTER_ACT, classLoader, "setupTabs", setupTabHook)
        bridge.hookAllMethods(CLS_CONVERTER_ACT, classLoader, "setTitleTabTextColor", selectTabHook)
        bridge.hookAllMethods(CLS_CONVERTER_ACT, classLoader, "onPageSelectedFunction", selectTabHook)
        bridge.hookAllMethods(CLS_MORTGAGE_RESULT_ACT, classLoader, "setupTabs", setupTabHook)
        bridge.hookAllMethods(CLS_MORTGAGE_RESULT_ACT, classLoader, "setTitleTabTextColor", selectTabHook)

        // Drive sliding tab indicator smoothly during ViewPager drags and animated transitions
        bridge.hookMethod(
            CLS_TAB_LAYOUT,
            classLoader,
            "setScrollPosition",
            Int::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            callback = object : HookCallback {
                override fun after(param: HookContext) {
                    val cfg = currentConfig()
                    if (!cfg.enabled) return
                    val tabLayout = param.thisObject as? ViewGroup ?: return
                    val tabStrip = tabLayout.getChildAt(0) as? ViewGroup ?: return
                    val indicator = tabIndicators[tabStrip] ?: return
                    val pos = param.args[0] as? Int ?: return
                    val offset = param.args[1] as? Float ?: return
                    indicator.onScrollPosition(pos, offset)
                }
            }
        )

        // Strip converter_keypad black background & button XML backgrounds set by ConverterKeypadController
        val keypadRefreshHook = object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val ctrl = param.thisObject ?: return
                val ctx = findFieldSafe(ctrl.javaClass, "mContext")?.get(ctrl) as? Activity ?: return
                val tokens = resolveTokens(ctx)
                val res = ctx.resources
                val pkg = ctx.packageName
                val keypadId = res.getIdentifier("converter_keypad", "id", pkg)
                if (keypadId != 0) {
                    ctx.findViewById<View>(keypadId)?.apply {
                        setBackgroundColor(tokens.background)
                        backgroundTintList = null
                    }
                }
                ctx.window?.decorView?.let { invalidateCustomButtons(it) }
            }
        }
        bridge.hookAllMethods(CLS_CONVERTER_KEYPAD_CTRL, classLoader, "initControl", keypadRefreshHook)
        bridge.hookAllMethods(CLS_CONVERTER_KEYPAD_CTRL, classLoader, "onConfigurationChanged", keypadRefreshHook)
        bridge.hookAllMethods(CLS_CONVERTER_LAYOUT_CTRL, classLoader, "arrangeLayout", keypadRefreshHook)
        bridge.hookAllMethods(CLS_CONVERTER_LAYOUT_CTRL, classLoader, "onResume", keypadRefreshHook)

        // Restyle Converter spinner anchor pills (replaces pitch-black @drawable/ripple_effect_spinner)
        val spinnerViewHook = object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val view = param.result as? View ?: return
                val tokens = resolveTokens(view.context)
                styleSpinnerPillView(view, cfg, tokens)
            }
        }
        bridge.hookAllMethods(CLS_CONVERTER_SPINNER_ADAPTER, classLoader, "getView", spinnerViewHook)
        bridge.hookAllMethods(CLS_MORTGAGE_SPINNER_ADAPTER, classLoader, "getView", spinnerViewHook)

        // Style Converter dropdown popup menu rows & container without touching AppCompatSpinner during measure
        bridge.hookAllMethods(CLS_CONVERTER_SPINNER_ADAPTER, classLoader, "getDropDownView", object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val row = param.result as? View ?: return
                val parent = param.args.getOrNull(2) as? ViewGroup
                val ctx = row.context
                val tokens = resolveTokens(ctx)
                val density = row.resources.displayMetrics.density
                val res = row.resources
                val pkg = ctx.packageName

                // CRITICAL: AppCompatSpinner passes ITSELF as `parent` during measureContentWidth().
                // Only style the popup's ListView and its PopupDecor/PopupBackground ancestors!
                if (parent is android.widget.ListView) {
                    styleDropdownListView(parent, cfg, tokens)
                    var container: Any? = parent.parent
                    var depth = 0
                    while (container is View && depth < 3) {
                        clearSemBlurInfo(container)
                        container.background = null
                        container.backgroundTintList = null
                        container = container.parent
                        depth++
                    }
                }

                val ltrId = res.getIdentifier("dropdown_ic_checked_ltr", "id", pkg)
                val rtlId = res.getIdentifier("dropdown_ic_checked_rtl", "id", pkg)
                val ltrIcon = if (ltrId != 0) row.findViewById<ImageView>(ltrId) else null
                val rtlIcon = if (rtlId != 0) row.findViewById<ImageView>(rtlId) else null
                val isSelected = (ltrIcon?.visibility == View.VISIBLE) || (rtlIcon?.visibility == View.VISIBLE)

                ltrIcon?.imageTintList = ColorStateList.valueOf(tokens.resultText)
                rtlIcon?.imageTintList = ColorStateList.valueOf(tokens.resultText)

                val itemTvId = res.getIdentifier("tv_unit_adapter_item", "id", pkg)
                val symTvId = res.getIdentifier("tv_unit_symbol_adapter_item", "id", pkg)
                val fg = if (isSelected) tokens.resultText else tokens.formulaText
                if (itemTvId != 0) row.findViewById<TextView>(itemTvId)?.setTextColor(fg)
                if (symTvId != 0) row.findViewById<TextView>(symTvId)?.setTextColor(fg)

                val rowPadL = row.paddingLeft
                val rowPadT = row.paddingTop
                val rowPadR = row.paddingRight
                val rowPadB = row.paddingBottom
                row.background = null
                row.backgroundTintList = null
                row.setPadding(rowPadL, rowPadT, rowPadR, rowPadB)
            }
        })

        // Refresh converter fragment content (values, unit labels, dividers, tip pills)
        val fragHook = object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val frag = param.thisObject ?: return
                val rootView = try {
                    frag.javaClass.getMethod("getView").invoke(frag) as? View
                } catch (_: Throwable) {
                    null
                } ?: return
                val tokens = resolveTokens(rootView.context)
                styleConverterHierarchy(rootView, cfg, tokens)
            }
        }
        bridge.hookAllMethods(CLS_BASE_CONVERTER_FRAG, classLoader, "onResume", fragHook)
        bridge.hookAllMethods(CLS_BASE_CONVERTER_FRAG, classLoader, "initLayoutControl", fragHook)

        // Ensure ConverterUtils.setEnabledButton never leaves a dark ColorFilter on disabled ↑ / ↓ buttons
        bridge.hookAllMethods(CLS_CONVERTER_UTILS, classLoader, "setEnabledButton", object : HookCallback {
            override fun after(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val ctx = param.args.getOrNull(0) as? Activity ?: return
                val viewId = param.args.getOrNull(1) as? Int ?: return
                val btn = ctx.findViewById<View>(viewId) ?: return
                if (btn is ImageView) {
                    btn.clearColorFilter()
                }
                if (btn.background != null) {
                    btn.background = null
                }
                btn.invalidate()
            }
        })
    }

    private fun styleConverterTabsInActivity(
        activity: Activity,
        cfg: ModuleConfig,
        tokens: PaletteTokens,
        selectedIndex: Int? = null,
        animate: Boolean = false
    ) {
        val pkg = activity.packageName
        val res = activity.resources
        val tabLayoutId = res.getIdentifier("converter_tab_layout", "id", pkg)
        val altTabId = res.getIdentifier("mtg_result_tab_layout", "id", pkg)
        val tabLayout = (if (tabLayoutId != 0) activity.findViewById<ViewGroup>(tabLayoutId) else null)
            ?: (if (altTabId != 0) activity.findViewById<ViewGroup>(altTabId) else null)
            ?: return

        (tabLayout.parent as? View)?.apply {
            setBackgroundColor(tokens.background)
            backgroundTintList = null
        }
        tabLayout.apply {
            setBackgroundColor(Color.TRANSPARENT)
            backgroundTintList = null
        }
        try {
            tabLayout.javaClass.getMethod("setSelectedTabIndicatorColor", Int::class.javaPrimitiveType)
                .invoke(tabLayout, Color.TRANSPARENT)
            tabLayout.javaClass.getMethod("setSelectedTabIndicatorHeight", Int::class.javaPrimitiveType)
                .invoke(tabLayout, 0)
        } catch (_: Throwable) {
        }

        val titleId = res.getIdentifier("converter_tab_item_title", "id", pkg)
        if (titleId == 0) return

        val tabStrip = tabLayout.getChildAt(0) as? ViewGroup ?: return
        tabStrip.backgroundTintList = null

        val indicator = tabIndicators.getOrPut(tabStrip) {
            ExpressiveTabIndicatorDrawable(tabStrip, titleId)
        }
        indicator.updateStyle(
            shape = cfg.shapeFor(ButtonRole.UTILITY),
            morphMode = cfg.morphMode,
            tokens = tokens,
            showOutline = cfg.showOutline,
            outlineWidthDp = cfg.outlineWidthDp,
            showGlow = cfg.showExpressiveGlow
        )
        if (tabStrip.background !== indicator) {
            tabStrip.background = indicator
        }

        var detectedSelected = selectedIndex ?: -1
        for (i in 0 until tabStrip.childCount) {
            val child = tabStrip.getChildAt(i) ?: continue
            child.setBackgroundColor(Color.TRANSPARENT)
            child.backgroundTintList = null
            if (child is ViewGroup) {
                for (j in 0 until child.childCount) {
                    val inner = child.getChildAt(j)
                    if (inner.id != titleId) {
                        inner.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
            }
            val tv = child.findViewById<TextView>(titleId)
            if (tv != null) {
                if (detectedSelected < 0 && tv.isSelected) {
                    detectedSelected = i
                }
                tv.background = RippleDrawable(
                    pressedOnlyColorStateList(withAlpha(tokens.equalsBg, 40)),
                    null,
                    ExpressiveShapeDrawable(
                        shape = cfg.shapeFor(ButtonRole.UTILITY),
                        fillColor = Color.WHITE
                    )
                )
            }
        }

        val targetIdx = if (detectedSelected >= 0) detectedSelected else 0
        if (animate) {
            indicator.animateTo(targetIdx)
        } else {
            indicator.snapTo(targetIdx)
            tabStrip.post { indicator.snapTo(targetIdx) }
        }
    }

    private fun styleSpinnerPillView(view: View, cfg: ModuleConfig, tokens: PaletteTokens) {
        val padL = view.paddingLeft
        val padT = view.paddingTop
        val padR = view.paddingRight
        val padB = view.paddingBottom
        val mask = ExpressiveShapeDrawable(
            shape = cfg.shapeFor(ButtonRole.NUMBER),
            fillColor = Color.WHITE
        )
        view.background = RippleDrawable(
            pressedOnlyColorStateList(withAlpha(tokens.equalsBg, 42)),
            null,
            mask
        )
        view.backgroundTintList = null
        view.setPadding(padL, padT, padR, padB)
        val res = view.resources
        val pkg = view.context.packageName
        for (tvName in listOf("converter_spinner_selected_item", "spinner_item_top_tv")) {
            val id = res.getIdentifier(tvName, "id", pkg)
            if (id != 0) {
                view.findViewById<TextView>(id)?.setTextColor(tokens.formulaText)
            }
        }
        tintChildImageViews(view, tokens.formulaText)
    }

    private fun tintChildImageViews(view: View, tintColor: Int) {
        if (view is ImageView && view.javaClass.name != CLS_CUSTOM_IMAGE) {
            view.clearColorFilter()
            view.imageTintList = ColorStateList.valueOf(tintColor)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                tintChildImageViews(view.getChildAt(i), tintColor)
            }
        }
    }

    private fun styleConverterHierarchy(view: View, cfg: ModuleConfig, tokens: PaletteTokens) {
        val entry = safeEntryName(view)
        val density = view.resources.displayMetrics.density
        when (entry) {
            "converter_keypad",
            "calc_keypad" -> {
                view.setBackgroundColor(tokens.background)
                view.backgroundTintList = null
            }
            "converter_page_main_layout",
            "converter_area",
            "converter_view_pager_content",
            "converter_tab_item_layout",
            "converter_page_item_1",
            "converter_page_item_2",
            "converter_page_item_3",
            "converter_page_item_1_exchange_rate",
            "converter_page_item_2_exchange_rate",
            "converter_pager_content_text_1",
            "converter_pager_content_text_2",
            "converter_pager_content_text_3" -> {
                view.setBackgroundColor(Color.TRANSPARENT)
                view.backgroundTintList = null
            }
            "converter_pager_edt_1",
            "converter_pager_edt_2",
            "converter_pager_edt_3" -> if (view is TextView) {
                view.setTextColor(tokens.formulaText)
                view.setBackgroundColor(Color.TRANSPARENT)
            }
            "converter_pager_tv_1",
            "converter_pager_tv_2",
            "converter_pager_tv_3",
            "tip_amount_tv",
            "each_tv",
            "total_text",
            "total_tv",
            "tip_number",
            "person_number",
            "converter_item_name" -> if (view is TextView) {
                view.setTextColor(tokens.formulaText)
            }
            "tip_amount_result",
            "each_result" -> if (view is TextView) {
                view.setTextColor(tokens.resultText)
            }
            "converter_item_value",
            "converter_item_unit" -> if (view is TextView) {
                view.setTextColor(withAlpha(tokens.formulaText, 195))
            }
            "converter_divider_under_tab",
            "converter_tip_view_1",
            "converter_tip_view_2",
            "divider_exchange_and_update_area",
            "calc_history_divider" -> {
                view.setBackgroundColor(tokens.outlineColor)
            }
            "tip_number_layout",
            "person_number_layout" -> {
                val outlinePx = if (cfg.showOutline && cfg.outlineWidthDp > 0f) cfg.outlineWidthDp * density else 0f
                view.background = ExpressiveShapeDrawable.ripple(
                    shape = cfg.shapeFor(ButtonRole.UTILITY),
                    fillColor = tokens.numberBg,
                    rippleColor = withAlpha(tokens.equalsBg, 48),
                    strokeColor = if (outlinePx > 0f) tokens.outlineColor else Color.TRANSPARENT,
                    strokeWidthPx = outlinePx
                )
                tintChildImageViews(view, tokens.formulaText)
            }
            "converter_spinner_selected_item",
            "spinner_item_top_tv" -> {
                (view.parent as? View)?.let { spinnerContainer ->
                    styleSpinnerPillView(spinnerContainer, cfg, tokens)
                }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                styleConverterHierarchy(view.getChildAt(i), cfg, tokens)
            }
        }
    }

    private fun hookChipBackground(classLoader: ClassLoader) {
        bridge.hookMethod(CLS_CHIP_BG, classLoader, "onDraw", Canvas::class.java, callback = object : HookCallback {
            override fun before(param: HookContext) {
                val cfg = currentConfig()
                if (!cfg.enabled) return
                val view = param.thisObject as? View ?: return
                val canvas = param.args[0] as? Canvas ?: return
                val w = view.width.toFloat()
                val h = view.height.toFloat()
                if (w <= 1f || h <= 1f) {
                    param.result = null
                    return
                }
                val tokens = resolveTokens(view.context)
                val p = fillPaint.get()!!
                p.style = Paint.Style.FILL
                p.shader = LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(
                        withAlpha(tokens.glowStart, 95),
                        withAlpha(tokens.glowEnd, 110),
                        withAlpha(tokens.equalsBg, 90)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
                val r = min(w, h) / 2f
                canvas.drawRoundRect(0f, 0f, w, h, r, r, p)
                p.shader = null
                param.result = null
            }
        })
    }

    private fun hookCommonUtils(classLoader: ClassLoader) {
        bridge.hookAllMethods(CLS_COMMON_UTILS, classLoader, "onHapticFeedback", object : HookCallback {
            override fun before(param: HookContext) {
                val cfg = currentConfig()
                if (cfg.enabled && cfg.disableHaptics) {
                    param.result = null
                }
            }
        })
    }

    private fun roleForView(view: View): ButtonRole {
        return ButtonRole.fromEntryName(safeEntryName(view))
    }

    private fun safeEntryName(view: View): String? {
        val id = view.id
        if (id == View.NO_ID || id == 0) return null
        return resNameCache.getOrPut(id) {
            try {
                view.resources.getResourceEntryName(id)
            } catch (_: Throwable) {
                ""
            }
        }.ifEmpty { null }
    }

    private fun findFieldSafe(cls: Class<*>, name: String): Field? {
        var cur: Class<*>? = cls
        while (cur != null) {
            try {
                return cur.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                cur = cur.superclass
            }
        }
        return null
    }

    private fun getIntField(field: Field?, target: Any, fallback: Int): Int {
        return try {
            field?.getInt(target) ?: fallback
        } catch (_: Throwable) {
            fallback
        }
    }

    private fun setIntField(field: Field?, target: Any, value: Int) {
        try {
            field?.setInt(target, value)
        } catch (_: Throwable) {
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun calculateLuminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}
