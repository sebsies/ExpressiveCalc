package com.expressive.oneuicalc.prefs

import android.content.Context
import android.content.SharedPreferences
import com.expressive.oneuicalc.model.ModuleConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

object PlatformPrefs : PrefsBackend {
    @Volatile
    private var service: XposedService? = null

    @Volatile
    private var remotePrefs: SharedPreferences? = null

    override fun init(context: Context, onStatusChanged: (FrameworkStatus) -> Unit) {
        onStatusChanged(
            FrameworkStatus(
                isActive = false,
                frameworkLabel = "Connecting to Vector Service…",
                apiLabel = "libxposed API 102",
                supportsRemotePrefs = false
            )
        )
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(svc: XposedService) {
                service = svc
                val rp = svc.getRemotePreferences(ModuleConfig.PREFS_NAME)
                remotePrefs = rp
                // Push local fallback config into remote prefs if remote prefs is fresh/empty
                val localPrefs = context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)
                if (!rp.contains("palette_id") && localPrefs.contains("palette_id")) {
                    ModuleConfig.from(localPrefs).writeTo(rp.edit())
                }
                onStatusChanged(
                    FrameworkStatus(
                        isActive = true,
                        frameworkLabel = "${svc.frameworkName} ${svc.frameworkVersion} Active",
                        apiLabel = "libxposed API ${svc.apiVersion}",
                        supportsRemotePrefs = true
                    )
                )
            }

            override fun onServiceDied(svc: XposedService) {
                service = null
                remotePrefs = null
                onStatusChanged(
                    FrameworkStatus(
                        isActive = false,
                        frameworkLabel = "Vector Service Disconnected",
                        apiLabel = "libxposed API 102",
                        supportsRemotePrefs = false
                    )
                )
            }
        })
    }

    override fun loadConfig(context: Context): ModuleConfig {
        val rp = remotePrefs
        return if (rp != null) {
            ModuleConfig.from(rp)
        } else {
            ModuleConfig.from(context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE))
        }
    }

    override fun saveConfig(context: Context, config: ModuleConfig) {
        val local = context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)
        config.writeTo(local.edit())
        remotePrefs?.let { rp ->
            config.writeTo(rp.edit())
        }
    }
}
