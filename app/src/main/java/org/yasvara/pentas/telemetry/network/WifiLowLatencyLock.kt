/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.telemetry.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.WorkSource
import android.util.Log

/** A low-latency Wi-Fi lock attributed to the game, held for the session. */
class WifiLowLatencyLock(private val context: Context) {

    private val wifiManager = context.getSystemService(WifiManager::class.java)

    private val lock: WifiManager.WifiLock? by lazy {
        wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, TAG)?.apply {
            setReferenceCounted(false)
        }
    }

    fun acquire(pkg: String? = null) {
        val l = lock ?: return
        if (pkg != null) {
            val uid = runCatching { context.packageManager.getPackageUid(pkg, 0) }.getOrNull()
            if (uid != null) {
                runCatching {
                    l.setWorkSource(WorkSource(uid, pkg))
                }.onFailure { Log.w(TAG, "failed to set work source for $pkg ($uid)", it) }
            }
        }
        if (l.isHeld) return
        runCatching { l.acquire() }
            .onSuccess { Log.d(TAG, "low latency lock acquired for $pkg") }
            .onFailure { Log.w(TAG, "acquire failed", it) }
    }

    fun release() {
        val l = lock ?: return
        if (!l.isHeld) return
        runCatching { l.release() }
            .onSuccess { Log.d(TAG, "low latency lock released") }
            .onFailure { Log.w(TAG, "release failed", it) }
    }

    companion object {
        private const val TAG = "Pentas:WifiLowLatency"
    }
}
