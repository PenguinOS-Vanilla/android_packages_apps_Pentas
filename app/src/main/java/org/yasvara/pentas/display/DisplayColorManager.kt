/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.display

import android.content.Context
import android.hardware.display.ColorDisplayManager
import org.yasvara.pentas.device.DeviceProfile
import org.yasvara.pentas.device.DeviceProfiles

/** Applies a game's display colour mode and restores the user's mode afterwards. */
class DisplayColorManager private constructor(
    context: Context,
    private val deviceProfile: DeviceProfile,
) {
    private val cdm: ColorDisplayManager? = runCatching {
        context.getSystemService(ColorDisplayManager::class.java)
    }.getOrNull()

    private var originalColorMode: Int? = null

    fun applyForGame(style: Int) {
        if (cdm == null) return
        if (originalColorMode == null) {
            originalColorMode = runCatching { cdm?.colorMode }.getOrNull()
        }
        applyStyle(style)
    }

    fun applyStyle(style: Int) {
        if (cdm == null) return
        val target = deviceProfile.colorModeMap[style] ?: return
        runCatching { cdm?.setColorMode(target) }
    }

    fun restore() {
        if (cdm == null) return
        originalColorMode?.let {
            runCatching { cdm?.setColorMode(it) }
        }
        originalColorMode = null
    }

    companion object {
        @Volatile
        private var INSTANCE: DisplayColorManager? = null

        fun get(context: Context): DisplayColorManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DisplayColorManager(
                    context.applicationContext,
                    DeviceProfiles.get(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }
}
