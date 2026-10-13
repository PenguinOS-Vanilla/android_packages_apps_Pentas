/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.session

/** Session kinds; values match system_server's PentasService. */
object SidebarMode {
    const val MODE_NONE = 0
    const val MODE_GAME = 1
    const val MODE_VIDEO = 2

    fun toString(mode: Int) = when (mode) {
        MODE_GAME -> "GAME"
        MODE_VIDEO -> "VIDEO"
        else -> "NONE"
    }
}
