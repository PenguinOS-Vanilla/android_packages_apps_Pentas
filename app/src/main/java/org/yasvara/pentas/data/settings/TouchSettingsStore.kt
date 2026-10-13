/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.data.settings

import org.yasvara.pentas.data.SettingsIO
import javax.inject.Inject
import javax.inject.Singleton

/** Per-game touch settings: report rate (vendor HAL) and the edge/grip input filter. */
@Singleton
class TouchSettingsStore @Inject constructor(private val io: SettingsIO) {

    companion object {
        const val KEY_TOUCH_SUPER_REPORT = "pentas_touch_super_report"
        const val KEY_TOUCH_EDGE_FILTER_V2 = "pentas_touch_edge_filter"
        const val KEY_TOUCH_GRIP_SUPPRESSION = "pentas_touch_grip_suppression"
    }

    fun superReport(pkg: String) = io.getPerAppBoolean(KEY_TOUCH_SUPER_REPORT, pkg, true)
    fun setSuperReport(pkg: String, v: Boolean) = io.putPerApp(KEY_TOUCH_SUPER_REPORT, pkg, v)

    fun edgeFilter(pkg: String) =
        io.getPerAppInt(KEY_TOUCH_EDGE_FILTER_V2, pkg, 0).coerceIn(0, 3)

    fun setEdgeFilter(pkg: String, level: Int) =
        io.putPerApp(KEY_TOUCH_EDGE_FILTER_V2, pkg, level.coerceIn(0, 3))

    fun gripSuppression(pkg: String) =
        io.getPerAppInt(KEY_TOUCH_GRIP_SUPPRESSION, pkg, 0).coerceIn(0, 3)

    fun setGripSuppression(pkg: String, level: Int) =
        io.putPerApp(KEY_TOUCH_GRIP_SUPPRESSION, pkg, level.coerceIn(0, 3))

    fun removeAll(pkg: String) =
        (listOf(
            KEY_TOUCH_SUPER_REPORT,
            KEY_TOUCH_EDGE_FILTER_V2,
            KEY_TOUCH_GRIP_SUPPRESSION,
        )).forEach { io.putPerApp(it, pkg, null) }
}
