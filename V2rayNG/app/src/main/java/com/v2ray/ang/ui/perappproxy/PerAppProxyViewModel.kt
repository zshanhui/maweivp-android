package com.v2ray.ang.ui.perappproxy

import android.app.Application
import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.handler.ChinaApps
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.AppSelection
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.util.AppManagerUtil
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.Collator

/**
 * ViewModel for PerAppProxy screen.
 * Holds all UI state and business logic.
 */
class PerAppProxyViewModel(application: Application) : BaseViewModel(application) {

    // Blacklist (apps to be proxied or bypassed)
    private val _blacklist = MutableStateFlow(loadBlacklist())
    val blacklist: StateFlow<Set<String>> = _blacklist.asStateFlow()

    // UI states
    private val _displayedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val displayedApps: StateFlow<List<AppInfo>> = _displayedApps.asStateFlow()

    private val _perAppProxyEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY, false)
    )
    val perAppProxyEnabled: StateFlow<Boolean> = _perAppProxyEnabled.asStateFlow()

    private val _chinaMode = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_CHINA_MODE, false)
    )
    val chinaMode: StateFlow<Boolean> = _chinaMode.asStateFlow()

    /** Packages that end up outside the tunnel: manual selection plus China Mode, minus overrides. */
    private val _effectiveBypass = MutableStateFlow<Set<String>>(emptySet())
    val effectiveBypass: StateFlow<Set<String>> = _effectiveBypass.asStateFlow()

    /** China apps the user explicitly sent back through the tunnel; beats the catalogue. */
    private val forceProxy: MutableSet<String> = MmkvManager
        .decodeSettingsStringSet(AppConfig.PREF_PER_APP_FORCE_PROXY_SET)
        ?.toMutableSet()
        ?: mutableSetOf()

    // Cached full list for filtering
    private var appsAll: List<AppInfo>? = null
    private var currentQuery = ""
    private var isAppListLoading = false

    // Blacklist operations
    fun toggle(packageName: String) {
        val wasBypassed = packageName in _effectiveBypass.value
        if (wasBypassed) {
            // Unchecking always tunnels the app. When the catalogue would re-add it, remember the
            // override so China Mode does not undo the user's choice.
            if (_chinaMode.value && ChinaApps.isChinaApp(packageName)) {
                forceProxy.add(packageName)
            }
            replaceBlacklist(_blacklist.value - packageName)
        } else {
            forceProxy.remove(packageName)
            replaceBlacklist(_blacklist.value + packageName)
        }
        persistForceProxy()
        refreshDisplayedApps()
        SettingsChangeManager.makeRestartService()
    }

    fun setChinaMode(enabled: Boolean) {
        if (_chinaMode.value == enabled) return
        _chinaMode.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_CHINA_MODE, enabled)
        if (!enabled) {
            // Overrides only mean something while the catalogue is active; keep them for a
            // re-enable, they are small and re-applying is what the user asked for.
        }
        refreshDisplayedApps()
        SettingsChangeManager.makeRestartService()
    }

    private fun persistForceProxy() {
        MmkvManager.encodeSettings(
            AppConfig.PREF_PER_APP_FORCE_PROXY_SET,
            forceProxy.toMutableSet()
        )
    }

    /** Recomputes the bypass set from the manual selection, China Mode and the overrides. */
    private fun recomputeEffectiveBypass() {
        val automatic = if (_chinaMode.value) {
            (appsAll ?: emptyList()).asSequence()
                .map { it.packageName }
                .filter { ChinaApps.isChinaApp(it) }
                .toSet()
        } else {
            emptySet()
        }
        _effectiveBypass.value = ((_blacklist.value + automatic) - forceProxy)
    }

    /** Re-sorts and re-filters the list and publishes it, after any state change. */
    private fun refreshDisplayedApps() {
        recomputeEffectiveBypass()
        appsAll = appsAll?.let { sortApps(it) }
        _displayedApps.value = applyFilter(currentQuery)
    }
    private fun loadBlacklist(): Set<String> {
        return MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)?.toSet() ?: emptySet()
    }

    private fun replaceBlacklist(newBlacklist: Set<String>) {
        if (newBlacklist == _blacklist.value) return

        _blacklist.value = newBlacklist
        MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY_SET, newBlacklist.toMutableSet())
    }

    // Per‑app proxy switch
    fun setPerAppProxyEnabled(enabled: Boolean) {
        if (_perAppProxyEnabled.value != enabled) {
            _perAppProxyEnabled.value = enabled
            MmkvManager.encodeSettings(AppConfig.PREF_PER_APP_PROXY, enabled)
        }
    }

    // Load and filter apps
    fun loadApps(context: Context) {
        if (appsAll != null || isAppListLoading) return

        val applicationContext = context.applicationContext
        isAppListLoading = true
        launchLoading {
            try {
                val apps = withContext(Dispatchers.IO) {
                    AppManagerUtil.loadNetworkAppList(applicationContext, _blacklist.value)
                }
                appsAll = apps
                refreshDisplayedApps()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.ANG_PACKAGE, "Error loading apps", e)
            } finally {
                isAppListLoading = false
            }
        }
    }

    fun filterApps(query: String) {
        currentQuery = query
        _displayedApps.value = applyFilter(query)
    }

    private fun applyFilter(query: String): List<AppInfo> {
        val apps = appsAll ?: return emptyList()
        if (query.isEmpty()) return apps

        return apps.filter {
            it.appName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
        }
    }

    private fun sortApps(apps: List<AppInfo>): List<AppInfo> {
        val collator = Collator.getInstance()
        val bypassed = _effectiveBypass.value
        val manual = _blacklist.value
        fun rank(packageName: String): Int = when {
            packageName in bypassed && packageName !in manual -> 0 // China Mode
            packageName in bypassed -> 1 // the user's own checks
            else -> 2
        }
        return apps.sortedWith { p1, p2 ->
            val r1 = rank(p1.packageName)
            val r2 = rank(p2.packageName)
            when {
                r1 != r2 -> r1 - r2
                p1.isSystemApp != p2.isSystemApp -> if (p1.isSystemApp) 1 else -1
                else -> collator.compare(p1.appName, p2.appName)
            }
        }
    }

    // Bulk actions
    fun selectAll() {
        val displayedApps = _displayedApps.value
        val current = _effectiveBypass.value
        val allSelected = displayedApps.all { it.packageName in current }
        val newSelection = _blacklist.value.toMutableSet()
        if (allSelected) {
            // Clearing the visible ones: a bypassed China app needs an explicit override.
            displayedApps.forEach { app ->
                newSelection.remove(app.packageName)
                if (_chinaMode.value && ChinaApps.isChinaApp(app.packageName)) {
                    forceProxy.add(app.packageName)
                }
            }
        } else {
            displayedApps.forEach { app ->
                forceProxy.remove(app.packageName)
                newSelection.add(app.packageName)
            }
        }
        persistForceProxy()
        replaceBlacklist(newSelection)
        refreshDisplayedApps()
        enablePerAppProxyAndRestart()
    }

    fun invertSelection() {
        val current = _effectiveBypass.value
        val newSelection = _blacklist.value.toMutableSet()
        _displayedApps.value.forEach { app ->
            val packageName = app.packageName
            if (packageName in current) {
                newSelection.remove(packageName)
                if (_chinaMode.value && ChinaApps.isChinaApp(packageName)) {
                    forceProxy.add(packageName)
                }
            } else {
                forceProxy.remove(packageName)
                newSelection.add(packageName)
            }
        }
        persistForceProxy()
        replaceBlacklist(newSelection)
        refreshDisplayedApps()
        enablePerAppProxyAndRestart()
    }

    fun selectProxyAppAuto(context: Context) {
        val applicationContext = context.applicationContext
        launchLoading {
            val url = AppConfig.ANDROID_PACKAGE_NAME_LIST_URL
            var content = withContext(Dispatchers.IO) {
                HttpUtil.getUrlContent(
                    UrlContentRequest(
                        url = url,
                        timeout = 5000
                    )
                )
            }
            if (content.isNullOrEmpty()) {
                val proxyUsername = SettingsManager.getSocksUsername()
                val proxyPassword = SettingsManager.getSocksPassword()
                val httpPort = SettingsManager.getHttpPort()
                content = withContext(Dispatchers.IO) {
                    HttpUtil.getUrlContent(
                        UrlContentRequest(
                            url = url,
                            timeout = 5000,
                            httpPort = httpPort,
                            proxyUsername = proxyUsername,
                            proxyPassword = proxyPassword
                        )
                    )
                } ?: ""
            }
            val success = applyProxyAppList(
                content = content,
                context = applicationContext,
                forceGoogleApps = true
            )
            if (success) {
                enablePerAppProxyAndRestart()
            }
        }
    }

    fun importProxyApp(content: String?, context: Context) {
        if (content.isNullOrEmpty()) return

        val applicationContext = context.applicationContext
        launchLoading {
            val success = applyProxyAppList(
                content = content,
                context = applicationContext,
                forceGoogleApps = false
            )
            if (success) {
                enablePerAppProxyAndRestart()
            }
        }
    }

    fun exportProxyApp(): String {
        return buildString {
            append(PER_APP_BYPASS_LIST)
            _blacklist.value.forEach { packageName ->
                append(System.lineSeparator())
                append(packageName)
            }
        }
    }

    private suspend fun applyProxyAppList(content: String, context: Context, forceGoogleApps: Boolean): Boolean {
        val installedApps = appsAll ?: return false

        try {
            val proxyAppList = if (content.isEmpty()) {
                withContext(Dispatchers.IO) {
                    Utils.readTextFromAssets(context, "proxy_package_name")
                }
            } else content
            if (proxyAppList.isNullOrEmpty()) return false

            val bypassApps = PER_APP_BYPASS_LIST
            val newBlacklist = withContext(Dispatchers.Default) {
                AppSelection.fromProxyList(
                    packageNames = installedApps.map { it.packageName },
                    proxyAppList = proxyAppList,
                    bypassApps = bypassApps,
                    forceGoogleApps = forceGoogleApps
                )
            }
            replaceBlacklist(newBlacklist)
            refreshDisplayedApps()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Error selecting proxy app", e)
            return false
        }
        return true
    }

    private fun enablePerAppProxyAndRestart() {
        setPerAppProxyEnabled(true)
        SettingsChangeManager.makeRestartService()
    }

    companion object {
        /**
         * The per-app selection is a bypass list and always has been; the mode is not a setting.
         * Kept as a named constant so the import/export format still carries the flag.
         */
        private const val PER_APP_BYPASS_LIST = true
    }
}
