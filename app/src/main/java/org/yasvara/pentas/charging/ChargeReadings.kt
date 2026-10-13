/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.charging

import java.io.File

/** Live cell (fuel gauge) and board (PMIC thermistor) temperatures; the board reads ~6 °C higher. */
data class ChargeReadings(
    val cellC: Float?,
    val boardC: Float?,
    val pumpC: Float?,
) {
    /** Board minus cell, °C. Null when either sensor is unavailable. */
    val boardOffsetC: Float?
        get() = if (cellC != null && boardC != null) boardC - cellC else null

    companion object {
        /** Fuel gauge, deci-°C. */
        private const val GAUGE_PATH = "/sys/class/power_supply/battery/temp"
        private const val THERMAL_ROOT = "/sys/class/thermal"

        fun read(dynamics: ChargeDynamics): ChargeReadings = ChargeReadings(
            cellC = readSensor(dynamics.cellSensor),
            boardC = readSensor(dynamics.boardSensor),
            pumpC = readSensor(dynamics.pumpSensor),
        )

        /**
         * Resolve a sensor by *name*, never by zone index — zone numbering is
         * not stable across kernels, and `batt_therm` has moved before.
         */
        private fun readSensor(name: String): Float? {
            if (name == "battery") {
                return readFile(GAUGE_PATH)?.let { it / 10f }
            }
            val zone = zoneByType(name) ?: return null
            return readFile("$zone/temp")?.let { it / 1000f }
        }

        private fun zoneByType(type: String): String? =
            runCatching {
                File(THERMAL_ROOT).listFiles { f -> f.name.startsWith("thermal_zone") }
                    ?.firstOrNull { readText("${it.path}/type") == type }
                    ?.path
            }.getOrNull()

        private fun readText(path: String): String? =
            runCatching { File(path).readText().trim() }.getOrNull()

        private fun readFile(path: String): Float? =
            readText(path)?.toFloatOrNull()
    }
}
