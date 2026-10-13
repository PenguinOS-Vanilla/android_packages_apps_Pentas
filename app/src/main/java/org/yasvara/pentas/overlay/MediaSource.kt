/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.overlay

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class NowPlaying(
    val title: String,
    val artist: String,
    val app: String,
    val art: Bitmap?,
    val playing: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val at: Long,
    val speed: Float,
) {
    /** Position extrapolated to [now] while playing, like SystemUI's seekbar. */
    fun position(now: Long = SystemClock.elapsedRealtime()): Long =
        if (!playing) positionMs else (positionMs + ((now - at) * speed).toLong()).coerceAtMost(durationMs)
}

/**
 * The active media session for the home's context slot. Listens only while the panel is
 * open; a closed panel costs nothing.
 */
@Singleton
class MediaSource @Inject constructor(@ApplicationContext private val context: Context) {
    private val msm = context.getSystemService(MediaSessionManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var listening = false

    val now = mutableStateOf<NowPlaying?>(null)

    private val sessions = MediaSessionManager.OnActiveSessionsChangedListener { pick(it.orEmpty()) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() { bind(null) }
    }

    fun start() {
        if (listening) return
        listening = true
        runCatching {
            msm.addOnActiveSessionsChangedListener(sessions, null, handler)
            pick(msm.getActiveSessions(null))
        }
    }

    fun stop() {
        if (!listening) return
        listening = false
        runCatching { msm.removeOnActiveSessionsChangedListener(sessions) }
        bind(null)
    }

    private fun pick(list: List<MediaController>) {
        val active = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull { it.metadata != null }
        if (active?.sessionToken != controller?.sessionToken) bind(active)
    }

    private fun bind(c: MediaController?) {
        controller?.unregisterCallback(callback)
        controller = c
        c?.registerCallback(callback, handler)
        publish()
    }

    private fun publish() {
        val c = controller
        val m = c?.metadata
        if (c == null || m == null) { now.value = null; return }
        val st = c.playbackState
        val app = runCatching {
            context.packageManager.run { getApplicationLabel(getApplicationInfo(c.packageName, 0)).toString() }
        }.getOrDefault(c.packageName)
        now.value = NowPlaying(
            title = m.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = (m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty(),
            app = app,
            art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART),
            playing = st?.state == PlaybackState.STATE_PLAYING,
            durationMs = m.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0),
            positionMs = st?.position ?: 0,
            at = st?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed ?: 1f,
        )
    }

    fun playPause() {
        val t = controller?.transportControls ?: return
        if (now.value?.playing == true) t.pause() else t.play()
    }
    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
}
