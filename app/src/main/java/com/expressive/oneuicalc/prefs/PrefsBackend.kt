package com.expressive.oneuicalc.prefs

import android.content.Context
import com.expressive.oneuicalc.model.ModuleConfig

data class FrameworkStatus(
    val isActive: Boolean,
    val frameworkLabel: String,
    val apiLabel: String,
    val supportsRemotePrefs: Boolean
)

interface PrefsBackend {
    fun init(context: Context, onStatusChanged: (FrameworkStatus) -> Unit)
    fun loadConfig(context: Context): ModuleConfig
    fun saveConfig(context: Context, config: ModuleConfig)
}
