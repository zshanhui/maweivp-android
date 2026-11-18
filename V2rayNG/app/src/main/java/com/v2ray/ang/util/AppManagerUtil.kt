package com.v2ray.ang.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.Manifest
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.handler.ChinaApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManagerUtil {
    /**
     * Load the list of network applications.
     *
     * A package is listed when it declares the INTERNET permission *and* has a launcher entry: it
     * must be able to open a socket to be worth proxying, and it must be launchable for the choice
     * to be a user decision. That drops storage providers, services, overlays and other system
     * packages that would only clutter the list. Packages listed in [keepPackages] are returned
     * regardless, so a selection made earlier stays visible and can still be removed.
     *
     * @param context The context to use.
     * @param keepPackages Package names to return unconditionally, normally the current selection.
     * @return A list of AppInfo objects representing the network applications.
     */
    suspend fun loadNetworkAppList(
        context: Context,
        keepPackages: Set<String> = emptySet(),
    ): ArrayList<AppInfo> =
        withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            val launcherPackages = launcherPackages(packageManager)
            val apps = ArrayList<AppInfo>()

            for (pkg in packages) {
                val networkVisible = declaresInternet(pkg.requestedPermissions) &&
                        pkg.packageName in launcherPackages
                if (!networkVisible && pkg.packageName !in keepPackages) {
                    continue
                }
                val applicationInfo = pkg.applicationInfo ?: continue

                val appName = applicationInfo.loadLabel(packageManager).toString()
                val isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM > 0

                val appInfo = AppInfo(appName, pkg.packageName, isSystemApp, 0)
                apps.add(appInfo)
            }

            return@withContext apps
        }

    /** Packages that expose a launcher entry, i.e. that a user can start on purpose. */
    private fun launcherPackages(packageManager: PackageManager): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }

    /**
     * Whether a package's requested permissions include the INTERNET permission.
     *
     * @param requestedPermissions Permissions declared by the package, or null when none.
     */
    internal fun declaresInternet(requestedPermissions: Array<String>?): Boolean {
        return requestedPermissions?.contains(Manifest.permission.INTERNET) == true
    }

    /**
     * Packages that China Mode should bypass on this device.
     *
     * The catalogue is intersected with the same rule the picker uses ([declaresInternet] plus a
     * launcher entry), so the apps that get bypassed are exactly the apps a user can see and
     * choose. Packages in [forceProxy] are excluded: an explicit uncheck beats the catalogue.
     *
     * Runs on the calling thread; the caller is the VPN service while it establishes the
     * interface, and the lists are small.
     */
    fun chinaBypassPackages(
        packageManager: PackageManager,
        forceProxy: Set<String>,
    ): Set<String> {
        val launcherPackages = launcherPackages(packageManager)
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        return packages.asSequence()
            .filter { pkg ->
                pkg.packageName !in forceProxy &&
                    ChinaApps.isChinaApp(pkg.packageName) &&
                    pkg.packageName in launcherPackages &&
                    declaresInternet(pkg.requestedPermissions)
            }
            .map { it.packageName }
            .toSet()
    }

    fun getLastUpdateTime(context: Context): Long =
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime

}
