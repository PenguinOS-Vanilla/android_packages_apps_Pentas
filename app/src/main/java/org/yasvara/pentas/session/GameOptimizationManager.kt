/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.session

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.yasvara.pentas.data.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launch-time housekeeping for a game: drop background apps to free RAM and clear the game's
 * cache once it grows past [CACHE_THRESHOLD]. Each half is its own switch in settings.
 */
@Singleton
class GameOptimizationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: AppSettings,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun optimizeGameLaunch(pkg: String) {
        if (!settings.memoryManagement && !settings.cacheManagement) return
        scope.launch {
            if (settings.memoryManagement) runCatching { clearBackgroundApps(pkg) }.onFailure { Log.w(TAG, "memory", it) }
            if (settings.cacheManagement) runCatching { clearCacheIfLarge(pkg) }.onFailure { Log.w(TAG, "cache", it) }
        }
    }

    private fun clearBackgroundApps(game: String) {
        val am = context.getSystemService(ActivityManager::class.java)
        // killBackgroundProcesses reaches down to SERVICE_ADJ, which includes the launcher. Killing
        // it or the keyboard only buys a cold start when the game is left or a text field opens.
        val keep = setOfNotNull(game, context.packageName, "android", homePackage(), imePackage())
        am.runningAppProcesses.orEmpty()
            .filter { it.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
            .flatMap { it.pkgList.asList() }
            .distinct()
            .filter { it !in keep }
            .forEach { am.killBackgroundProcesses(it) }
    }

    private fun homePackage(): String? = context.packageManager
        .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName

    private fun imePackage(): String? =
        Settings.Secure.getStringForUser(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD, UserHandle.USER_CURRENT)
            ?.let(ComponentName::unflattenFromString)?.packageName

    private fun clearCacheIfLarge(pkg: String) {
        val stats = context.getSystemService(StorageStatsManager::class.java)
        val cache = stats.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg, Process.myUserHandle()).cacheBytes
        if (cache <= CACHE_THRESHOLD) return
        val observer = Class.forName("android.content.pm.IPackageDataObserver")
        PackageManager::class.java
            .getMethod("deleteApplicationCacheFiles", String::class.java, observer)
            .invoke(context.packageManager, pkg, null)
        Log.i(TAG, "Cleared ${cache shr 20} MB cache for $pkg")
    }

    private companion object {
        const val TAG = "GameOptimization"
        const val CACHE_THRESHOLD = 100L * 1024 * 1024
    }
}
