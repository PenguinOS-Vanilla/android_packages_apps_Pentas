/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.device

/** Picks a board thermistor from the kernel's thermal zones, independent of sysfs access. */
internal object BoardSensor {
    /** Most specific first: Xiaomi board thermistors, then generic board, crystal and pack sensors. */
    private val PREFERRED = listOf("quiet_therm", "sys_therm", "board", "xo_therm", "batt_therm", "pmic")

    /** BCL zones report droop levels and current limits, not temperatures. */
    private val NOT_TEMPERATURES = listOf("socd", "bcl", "ibat", "vbat", "lvl", "trip")

    fun pick(zones: List<ThermalZoneSpec>): ThermalZoneSpec? {
        val candidates = zones.filterNot { zone ->
            val type = zone.type.lowercase()
            NOT_TEMPERATURES.any(type::contains)
        }
        return PREFERRED.firstNotNullOfOrNull { key ->
            candidates.firstOrNull { it.type.lowercase().contains(key) }
        }
    }

    fun label(type: String): String {
        val lower = type.lowercase()
        return when {
            lower.contains("pmic") -> "PMIC"
            lower.contains("batt") -> "Battery"
            else -> "Board"
        }
    }
}
