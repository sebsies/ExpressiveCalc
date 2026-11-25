package com.expressive.oneuicalc.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.expressive.oneuicalc.model.ModuleConfig

object PlatformPrefs : PrefsBackend {
    @Suppress("DEPRECATION")
    private fun openWorldReadablePrefs(context: Context): Pair<SharedPreferences, Boolean> {
        return try {
            @SuppressLint("WorldReadableFiles")
            val prefs = context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_WORLD_READABLE)
            prefs to true
        } catch (_: SecurityException) {
            // SecurityException on MODE_WORLD_READABLE means LSPosed/Vector XSharedPreferences hook is not active yet
            context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE) to false
        }
    }

    override fun init(context: Context, onStatusChanged: (FrameworkStatus) -> Unit) {
        val (_, worldReadable) = openWorldReadablePrefs(context)
        onStatusChanged(
            FrameworkStatus(
                isActive = worldReadable,
                frameworkLabel = if (worldReadable) "Vector / LSPosed Active (Legacy API 82)" else "Module Not Activated in Vector",
                apiLabel = "Xposed API 82 • Universal Vector",
                supportsRemotePrefs = worldReadable
            )
        )
    }

    override fun loadConfig(context: Context): ModuleConfig {
        val (prefs, _) = openWorldReadablePrefs(context)
        return ModuleConfig.from(prefs)
    }

    override fun saveConfig(context: Context, config: ModuleConfig) {
        val (prefs, _) = openWorldReadablePrefs(context)
        config.writeTo(prefs.edit())
    }
}
