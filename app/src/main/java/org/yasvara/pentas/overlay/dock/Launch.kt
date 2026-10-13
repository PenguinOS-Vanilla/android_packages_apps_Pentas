/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.overlay.dock

import android.app.yasvara.Layang
import android.content.ComponentName
import android.os.Bundle
import android.os.Process
import android.util.Log

/** Opens [pkg] as a Layang floating window; Layang owns size and position. */
fun openInWindow(pkg: String) {
    Thread {
        val result = runCatching {
            Layang.service()?.openApp(ComponentName(pkg, ""), Process.myUserHandle().identifier, Bundle())
        }.getOrNull() ?: Layang.RESULT_FAILED
        if (result != Layang.RESULT_OK) Log.w("Pentas", "Layang refused $pkg: $result")
    }.start()
}
