package com.expressive.oneuicalc.xposed

import com.expressive.oneuicalc.BuildConfig
import com.expressive.oneuicalc.hook.CalculatorExpressiveHooks
import com.expressive.oneuicalc.hook.HookBridge
import com.expressive.oneuicalc.hook.HookCallback
import com.expressive.oneuicalc.hook.HookContext
import com.expressive.oneuicalc.model.ModuleConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class LegacyEntry : IXposedHookLoadPackage {

    @Volatile
    private var prefs: XSharedPreferences? = null

    @Volatile
    private var cachedConfig: ModuleConfig = ModuleConfig()

    @Volatile
    private var lastReloadCheckMs: Long = 0L

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != CalculatorExpressiveHooks.TARGET_PACKAGE) return

        val xPrefs = XSharedPreferences(BuildConfig.APPLICATION_ID, ModuleConfig.PREFS_NAME).apply {
            makeWorldReadable()
            reload()
        }
        prefs = xPrefs
        cachedConfig = ModuleConfig.from(xPrefs)

        val bridge = object : HookBridge {
            override fun log(message: String) {
                XposedBridge.log("[ExpressiveCalc] $message")
            }

            override fun logError(message: String, t: Throwable?) {
                XposedBridge.log("[ExpressiveCalc][ERR] $message")
                if (t != null) XposedBridge.log(t)
            }

            override fun hookMethod(
                className: String,
                classLoader: ClassLoader,
                methodName: String,
                vararg paramTypes: Any,
                callback: HookCallback
            ) {
                try {
                    val cls = XposedHelpers.findClass(className, classLoader)
                    val hook = toXcMethodHook(callback)
                    XposedHelpers.findAndHookMethod(cls, methodName, *paramTypes, hook)
                } catch (t: Throwable) {
                    logError("Failed to hook $className.$methodName", t)
                }
            }

            override fun hookAllMethods(
                className: String,
                classLoader: ClassLoader,
                methodName: String,
                callback: HookCallback
            ) {
                try {
                    val cls = XposedHelpers.findClass(className, classLoader)
                    XposedBridge.hookAllMethods(cls, methodName, toXcMethodHook(callback))
                } catch (t: Throwable) {
                    logError("Failed to hookAll $className.$methodName", t)
                }
            }
        }

        CalculatorExpressiveHooks(bridge) {
            val now = System.currentTimeMillis()
            if (now - lastReloadCheckMs > 1200L) {
                lastReloadCheckMs = now
                val xp = prefs
                if (xp != null && xp.hasFileChanged()) {
                    xp.reload()
                    cachedConfig = ModuleConfig.from(xp)
                }
            }
            cachedConfig
        }.install(lpparam.classLoader)
    }

    private fun toXcMethodHook(callback: HookCallback): XC_MethodHook {
        return object : XC_MethodHook() {
            private val ctxLocal = ThreadLocal<HookContext?>()

            override fun beforeHookedMethod(param: MethodHookParam) {
                val ctx = HookContext(
                    member = param.method,
                    thisObject = param.thisObject,
                    args = param.args ?: emptyArray(),
                    getResultFn = { param.result },
                    setResultFn = { param.result = it },
                    getThrowableFn = { param.throwable }
                )
                ctxLocal.set(ctx)
                callback.before(ctx)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val ctx = ctxLocal.get() ?: HookContext(
                    member = param.method,
                    thisObject = param.thisObject,
                    args = param.args ?: emptyArray(),
                    getResultFn = { param.result },
                    setResultFn = { param.result = it },
                    getThrowableFn = { param.throwable }
                )
                ctxLocal.remove()
                callback.after(ctx)
            }
        }
    }
}
