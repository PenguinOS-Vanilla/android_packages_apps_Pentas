/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.data

import android.content.pm.ApplicationInfo

/** App kind detection, matching system_server's PentasService. */
object AppClassifier {

    @Suppress("DEPRECATION")
    fun isGame(info: ApplicationInfo): Boolean =
        info.category == ApplicationInfo.CATEGORY_GAME ||
            (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    /** Moved out of PentasService.isVideoPackageName — same list, one home. */
    private val VIDEO_PKG_HINTS = listOf(
        "youtube", "netflix", "vlc", "mxplayer", "primevideo", "disney",
        "bilibili", "tiktok", "twitch", "iqiyi", "videoplayer", "hotstar",
        "hulu", "hbomax",
    )
}
