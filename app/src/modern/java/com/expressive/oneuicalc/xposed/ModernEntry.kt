package com.expressive.oneuicalc.xposed

import android.content.SharedPreferences
import android.util.Log
import com.expressive.oneuicalc.hook.CalculatorExpressiveHooks
import com.expressive.oneuicalc.hook.HookBridge
import com.expressive.oneuicalc.hook.HookCallback
import com.expressive.oneuicalc.hook.HookContext
import com.expressive.oneuicalc.model.ModuleConfig
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Method

class ModernEntry : XposedModule() {

    @Volatile
    private var remotePrefs: SharedPreferences? = null

    @Volatile
    private var cachedConfig: ModuleConfig = ModuleConfig()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, _ ->
        cachedConfig = ModuleConfig.from(sp)
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName != CalculatorExpressiveHooks.TARGET_PACKAGE) return

        try {
            val rp = getRemotePreferences(ModuleConfig.PREFS_NAME)
            remotePrefs = rp
            cachedConfig = ModuleConfig.from(rp)
            rp.registerOnSharedPreferenceChangeListener(prefsListener)
        } catch (t: Throwable) {
            log(Log.WARN, "ExpressiveCalc", "RemotePreferences unavailable, using defaults", t)
        }

        val bridge = object : HookBridge {
            override fun log(message: String) {
                this@ModernEntry.log(Log.INFO, "ExpressiveCalc", message)
            }

            override fun logError(message: String, t: Throwable?) {
                if (t != null) {
                    this@ModernEntry.log(Log.ERROR, "ExpressiveCalc", message, t)
                } else {
                    this@ModernEntry.log(Log.ERROR, "ExpressiveCalc", message)
                }
            }

            override fun hookMethod(
                className: String,
                classLoader: ClassLoader,
                methodName: String,
                vararg paramTypes: Any,
                callback: HookCallback
            ) {
                try {
                    val cls = Class.forName(className, false, classLoader)
                    val types = paramTypes.map {
                        when (it) {
                            is Class<*> -> it
                            is String -> Class.forName(it, false, classLoader)
                            else -> error("Unsupported paramType: $it")
                        }
                    }.toTypedArray()
                    val method = findMethodInHierarchy(cls, methodName, types)
                    if (method == null) {
                        logError("Method not found: $className.$methodName")
                        return
                    }
                    hook(method).intercept(toHooker(callback))
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
                    val cls = Class.forName(className, false, classLoader)
                    val hooker = toHooker(callback)
                    var count = 0
                    for (m in cls.declaredMethods) {
                        if (m.name == methodName) {
                            hook(m).intercept(hooker)
                            count++
                        }
                    }
                    if (count == 0) {
                        var cur: Class<*>? = cls.superclass
                        while (cur != null && count == 0) {
                            for (m in cur.declaredMethods) {
                                if (m.name == methodName) {
                                    hook(m).intercept(hooker)
                                    count++
                                }
                            }
                            cur = cur.superclass
                        }
                    }
                } catch (t: Throwable) {
                    logError("Failed to hookAll $className.$methodName", t)
                }
            }
        }

        CalculatorExpressiveHooks(bridge) { cachedConfig }.install(param.classLoader)
    }

    private fun toHooker(callback: HookCallback): XposedInterface.Hooker {
        return XposedInterface.Hooker { chain ->
            val argsArray = chain.args.toTypedArray()
            var earlyReturn = false
            var resultValue: Any? = null
            var thrown: Throwable? = null

            val ctx = HookContext(
                member = chain.executable,
                thisObject = chain.thisObject,
                args = argsArray,
                getResultFn = { resultValue },
                setResultFn = {
                    resultValue = it
                    earlyReturn = true
                },
                getThrowableFn = { thrown }
            )

            callback.before(ctx)
            if (!earlyReturn) {
                try {
                    resultValue = chain.proceed(argsArray)
                } catch (t: Throwable) {
                    thrown = t
                }
            }
            callback.after(ctx)
            if (thrown != null && !earlyReturn) {
                throw thrown
            }
            resultValue
        }
    }

    private fun findMethodInHierarchy(cls: Class<*>, name: String, paramTypes: Array<Class<*>>): Method? {
        var cur: Class<*>? = cls
        while (cur != null) {
            try {
                return cur.getDeclaredMethod(name, *paramTypes)
            } catch (_: NoSuchMethodException) {
                cur = cur.superclass
            }
        }
        return null
    }
}
