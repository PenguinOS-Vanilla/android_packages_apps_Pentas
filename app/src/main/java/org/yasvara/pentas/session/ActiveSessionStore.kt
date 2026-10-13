/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.session

import android.os.SystemClock
import org.yasvara.pentas.session.SidebarMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Which app has a session, in which mode, since when. */
@Singleton
class ActiveSessionStore @Inject constructor() {

    data class Session(
        val packageName: String,
        val mode: Int,
        val startElapsedMs: Long,
    )

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    val currentPackage: String? get() = _session.value?.packageName
    val mode: Int get() = _session.value?.mode ?: SidebarMode.MODE_NONE
    val startElapsedMs: Long get() = _session.value?.startElapsedMs ?: 0L
    val isLive: Boolean get() = _session.value != null
    val isGame: Boolean get() = mode == SidebarMode.MODE_GAME

    fun start(packageName: String, mode: Int) {
        _session.value = Session(packageName, mode, SystemClock.elapsedRealtime())
    }

    fun stop() {
        _session.value = null
    }
}
