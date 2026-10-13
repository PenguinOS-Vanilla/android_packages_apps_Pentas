/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.overlay.capture

import android.app.Activity
import android.app.ActivityTaskManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.android.systemui.mediaprojection.MediaProjectionCaptureTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import org.yasvara.pentas.overlay.OverlayState
import org.yasvara.pentas.session.ActiveSessionStore
import org.yasvara.pentas.session.SidebarMode
import javax.inject.Inject
import javax.inject.Singleton

/** Drives SystemUI's screen recorder; in a game it records only the game's task. */
@Singleton
class ScreenRecordController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val state: OverlayState,
    private val session: ActiveSessionStore,
) {
    private val main = Handler(Looper.getMainLooper())
    private val recorder = ComponentName("com.android.systemui", "com.android.systemui.screenrecord.RecordingService")
    private var lastToggle = 0L

    fun toggle(gameOnly: Boolean = session.mode == SidebarMode.MODE_GAME) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastToggle < DEBOUNCE_MS) return
        lastToggle = now
        runCatching {
            if (state.recording.value) {
                context.startService(Intent(ACTION_STOP).setComponent(recorder))
            } else {
                val intent = Intent(ACTION_START).setComponent(recorder)
                    .putExtra("extra_resultCode", Activity.RESULT_OK)
                    .putExtra("extra_useAudio", AUDIO_INTERNAL)
                    .putExtra("extra_showTaps", false)
                if (gameOnly) gameTask()?.let { intent.putExtra("extra_captureTarget", MediaProjectionCaptureTarget(null, it)) }
                context.startForegroundService(intent)
            }
        }.onFailure { Log.w(TAG, "Screen record toggle failed", it) }
        // The projection starts and stops asynchronously.
        listOf(500L, 2000L, 4000L).forEach { main.postDelayed(::refresh, it) }
    }

    fun refresh() {
        val running = runCatching {
            val mpm = context.getSystemService(MediaProjectionManager::class.java)
            mpm.javaClass.getMethod("getActiveProjectionInfo").invoke(mpm) != null
        }.getOrDefault(false)
        state.recording.value = running
        state.recordingSinceMs.longValue = when {
            !running -> 0L
            state.recordingSinceMs.longValue == 0L -> SystemClock.elapsedRealtime()
            else -> state.recordingSinceMs.longValue
        }
    }

    private fun gameTask(): Int? = runCatching {
        val pkg = session.currentPackage
        ActivityTaskManager.getService().getTasks(10, false, true, -1)
            .firstOrNull { it.topActivity?.packageName == pkg || it.baseActivity?.packageName == pkg }?.taskId
    }.getOrNull()

    private companion object {
        const val TAG = "Pentas"
        const val ACTION_START = "com.android.systemui.screenrecord.START"
        const val ACTION_STOP = "com.android.systemui.screenrecord.STOP"
        const val DEBOUNCE_MS = 1500L
        /** ScreenRecordingAudioSource.INTERNAL */
        const val AUDIO_INTERNAL = 1
    }
}
