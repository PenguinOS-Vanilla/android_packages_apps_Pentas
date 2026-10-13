/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.overlay.capture

import android.util.Log

/** Overlay windows register here so they can be hidden around a screenshot. */
object GameScreenshotUtil {

    private const val TAG = "GameScreenshotUtil"

    private val overlayHooks = mutableListOf<Pair<() -> Unit, () -> Unit>>()

    fun registerOverlay(hide: () -> Unit, show: () -> Unit) {
        synchronized(overlayHooks) {
            overlayHooks += hide to show
        }
    }

    fun hideOverlays() {
        synchronized(overlayHooks) {
            overlayHooks.forEach { (hide, _) ->
                runCatching { hide() }.onFailure {
                    Log.w(TAG, "failed hiding overlay", it)
                }
            }
        }
    }

    fun showOverlays() {
        synchronized(overlayHooks) {
            overlayHooks.asReversed().forEach { (_, show) ->
                runCatching { show() }.onFailure {
                    Log.w(TAG, "failed restoring overlay", it)
                }
            }
        }
    }
}