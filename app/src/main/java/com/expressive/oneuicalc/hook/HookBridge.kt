package com.expressive.oneuicalc.hook

import java.lang.reflect.Member

interface HookCallback {
    fun before(param: HookContext) {}
    fun after(param: HookContext) {}
}

class HookContext(
    val member: Member,
    val thisObject: Any?,
    val args: Array<Any?>,
    private val getResultFn: () -> Any?,
    private val setResultFn: (Any?) -> Unit,
    private val getThrowableFn: () -> Throwable?
) {
    var result: Any?
        get() = getResultFn()
        set(value) = setResultFn(value)

    val throwable: Throwable?
        get() = getThrowableFn()

    private val extras = HashMap<String, Any?>(2)

    fun setExtra(key: String, value: Any?) {
        extras[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getExtra(key: String): T? = extras[key] as? T
}

interface HookBridge {
    fun log(message: String)
    fun logError(message: String, t: Throwable? = null)
    fun hookMethod(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg paramTypes: Any,
        callback: HookCallback
    )
    fun hookAllMethods(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        callback: HookCallback
    )
}
