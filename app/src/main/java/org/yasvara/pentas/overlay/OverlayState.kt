/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.overlay

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import javax.inject.Inject
import javax.inject.Singleton

/** Where the open panel is. One page at a time; the rail stays. */
enum class Page { HOME, MONITOR, WIDGETS, TUNER, BATTERY, BLUETOOTH, WIFI, PING, RECORD, EDIT, APPS }

enum class TunerTab { PERF, GRAPHICS, TOUCH, SOUND }

/** Session-scoped sidebar state. Lives as long as the :ui process, reset per session. */
@Singleton
class OverlayState @Inject constructor() {
    val page = mutableStateOf(Page.HOME)
    val tunerTab = mutableStateOf(TunerTab.GRAPHICS)
    val profileList = mutableStateOf(false)
    /** The game's thermal profile index, loaded when the panel opens. */
    val profile = mutableIntStateOf(0)

    val recording = mutableStateOf(false)
    val recordingSinceMs = mutableLongStateOf(0L)

    val statsRecording = mutableStateOf(false)
    val statsSeconds = mutableIntStateOf(0)

    val gestureLock = mutableStateOf(false)
    val keyboard = mutableStateOf(false)
    /** APPS page purpose: true pins a rail shortcut, false opens the app in a window. */
    val appsPin = mutableStateOf(false)

    /** Set by SessionService for the session's lifetime. */
    var onGestureLock: (Boolean) -> Unit = {}
    var onStatsRecord: (Boolean) -> Unit = {}

    fun open(p: Page) { profileList.value = false; page.value = p }
    fun tuner(tab: TunerTab) { tunerTab.value = tab; open(Page.TUNER) }
    fun home() = open(Page.HOME)

    fun reset() {
        page.value = Page.HOME
        profileList.value = false
        keyboard.value = false
    }
}
