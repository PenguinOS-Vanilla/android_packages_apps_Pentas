/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.overlay.dock

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log

private const val FREEFORM_PACKAGE = "com.libremobileos.freeform"
private const val ACTION_START_FREEFORM = "com.libremobileos.freeform.START_FREEFORM"

/** Opens [pkg] as an LMOFreeform floating window; LMOFreeform owns size and position. */
fun openInWindow(context: Context, pkg: String) {
    val activity = runCatching {
        context.packageManager.getLaunchIntentForPackage(pkg)?.component?.className
    }.getOrNull()
    if (activity == null) {
        Log.w("Pentas", "No launcher activity for $pkg")
        return
    }
    runCatching {
        context.sendBroadcast(
            Intent(ACTION_START_FREEFORM).setPackage(FREEFORM_PACKAGE)
                .putExtra("packageName", pkg)
                .putExtra("activityName", activity)
                .putExtra("userId", UserHandle.myUserId()),
        )
    }.onFailure { Log.w("Pentas", "Freeform launch of $pkg failed", it) }
}
