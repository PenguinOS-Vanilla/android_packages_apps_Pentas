/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.session

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.yasvara.pentas.data.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What an incoming call does during a game: nothing (the system heads-up handles it), answer
 * on the speaker so the game keeps the screen, or reject. Registered only while a game runs.
 */
@Singleton
class CallPolicy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: AppSettings,
) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val telephony = context.getSystemService(TelephonyManager::class.java)
    private val telecom = context.getSystemService(TelecomManager::class.java)
    private var registered = false
    private var speakerByUs = false

    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> when (settings.callsMode) {
                    MODE_ANSWER -> runCatching { telecom.acceptRingingCall() }
                    MODE_REJECT -> runCatching { telecom.endCall() }
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> if (settings.callsMode == MODE_ANSWER) speakerIfNoHeadset()
                TelephonyManager.CALL_STATE_IDLE -> setSpeaker(false)
            }
        }
    }

    fun start() {
        if (registered || settings.callsMode == MODE_NONE) return
        registered = runCatching { telephony.registerTelephonyCallback(context.mainExecutor, callback) }.isSuccess
    }

    fun stop() {
        // A route left behind would put the next call on the speaker too.
        setSpeaker(false)
        if (!registered) return
        runCatching { telephony.unregisterTelephonyCallback(callback) }
        registered = false
    }

    /**
     * Puts call audio on the built-in speaker, or hands routing back to the system. [CallOverlay]
     * shares this route with Answer mode, so a call is routed once whoever gets there first.
     * Returns whether the speaker is now ours.
     */
    fun setSpeaker(on: Boolean): Boolean {
        if (on == speakerByUs) return speakerByUs
        if (on) {
            speakerByUs = audio.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?.let { audio.setCommunicationDevice(it) } ?: false
        } else {
            audio.clearCommunicationDevice()
            speakerByUs = false
        }
        return speakerByUs
    }

    /** Speaker for a call taken mid-game, unless a headset already carries the audio. */
    fun speakerIfNoHeadset(): Boolean = if (headset()) speakerByUs else setSpeaker(true)

    private fun headset() = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
    }

    internal companion object {
        const val MODE_NONE = 0
        const val MODE_ANSWER = 1
        const val MODE_REJECT = 2
    }
}
