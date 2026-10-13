/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.session

import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.TaskStackListener
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The foreground app for the whole process: one TaskStackListener while anyone listens, with
 * task-switch bursts debounced into a single query. Listeners run on the main thread.
 */
@Singleton
class ForegroundAppTracker @Inject constructor() {

    fun interface Listener {
        fun onForegroundAppChanged(packageName: String)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<Listener>()
    private val lock = Any()
    private var registered = false

    /** Committed foreground package; main thread only. */
    var currentPackage: String? = null
        private set

    /** Uptime of the first event of the burst being debounced, 0 when idle. */
    private var burstStart = 0L

    private val settle = Runnable {
        burstStart = 0L
        commit(queryTopPackage() ?: return@Runnable)
    }

    private val taskStackListener = object : TaskStackListener() {
        override fun onTaskStackChanged() {
            handler.post(::onStackEvent)
        }

        override fun onTaskMovedToFront(taskInfo: ActivityManager.RunningTaskInfo?) {
            handler.post(::onStackEvent)
        }
    }

    fun addListener(listener: Listener) {
        listeners += listener
        synchronized(lock) {
            if (registered) return
            try {
                ActivityTaskManager.getService().registerTaskStackListener(taskStackListener)
                registered = true
                Log.i(TAG, "registered")
            } catch (e: Exception) {
                Log.e(TAG, "registerTaskStackListener failed", e)
                return
            }
        }
        // Seed the state so a listener added while an app is already in front
        // (e.g. service restart mid-game) does not wait for the next switch.
        handler.post(::onStackEvent)
    }

    private fun onStackEvent() {
        val now = SystemClock.uptimeMillis()
        if (burstStart == 0L) burstStart = now
        handler.removeCallbacks(settle)
        // A stack that never goes quiet (an app churning activities) must not
        // starve detection forever: cap the wait from the first event.
        val delay = if (now - burstStart >= MAX_WAIT_MS) 0L else SETTLE_MS
        handler.postDelayed(settle, delay)
    }

    private fun commit(topPackage: String) {
        // Shade, recents and keyguard are overlays on the real foreground:
        // opening them must not end a session, closing them must not restart it.
        if (topPackage in TRANSIENT_PACKAGES) return
        if (topPackage == currentPackage) return
        currentPackage = topPackage
        listeners.forEach { it.onForegroundAppChanged(topPackage) }
    }

    private fun queryTopPackage(): String? = try {
        val top = ActivityTaskManager.getService().getTasks(1, false, true, -1).firstOrNull()
        top?.topActivity?.packageName ?: top?.baseActivity?.packageName
    } catch (e: Exception) {
        Log.w(TAG, "getTasks failed", e)
        null
    }

    private companion object {
        const val TAG = "ForegroundAppTracker"

        /** Quiet period that ends a transition burst. */
        const val SETTLE_MS = 300L

        /** Upper bound on detection latency while events keep arriving. */
        const val MAX_WAIT_MS = 1_500L

        val TRANSIENT_PACKAGES = setOf("com.android.systemui", "android")
    }
}
