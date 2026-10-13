/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.data

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The few platform settings Pentas reads or writes, plus its own game list. */
@Singleton
class SystemSettings @Inject constructor(@ApplicationContext context: Context) {
    private val resolver = context.contentResolver

    var autoBrightness: Boolean
        get() = Settings.System.getIntForUser(
            resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC, UserHandle.USER_CURRENT,
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        set(auto) {
            Settings.System.putIntForUser(
                resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                UserHandle.USER_CURRENT,
            )
        }

    /** Games the user added; system_server's PentasService reads the same key. */
    var games: List<String>
        get() = Settings.System.getStringForUser(resolver, KEY_GAME_LIST, UserHandle.USER_CURRENT)
            .orEmpty().split(';').map { it.substringBefore('=') }.filter { it.isNotEmpty() }
        set(list) {
            Settings.System.putStringForUser(
                resolver, KEY_GAME_LIST, list.distinct().joinToString(";"), UserHandle.USER_CURRENT,
            )
        }

    fun addGame(pkg: String) { if (pkg !in games) games = games + pkg }
    fun removeGame(pkg: String) { games = games - pkg }

    var autoGameDetect: Boolean
        get() = Settings.System.getIntForUser(resolver, KEY_AUTO_DETECT, 1, UserHandle.USER_CURRENT) == 1
        set(on) { Settings.System.putIntForUser(resolver, KEY_AUTO_DETECT, if (on) 1 else 0, UserHandle.USER_CURRENT) }

    companion object {
        const val KEY_GAME_LIST = "pentas_game_list"
        const val KEY_AUTO_DETECT = "pentas_auto_game_detect"
    }
}
