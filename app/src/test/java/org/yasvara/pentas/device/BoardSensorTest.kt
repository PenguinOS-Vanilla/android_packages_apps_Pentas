/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardSensorTest {
    private fun zones(vararg types: String) =
        types.mapIndexed { i, type -> ThermalZoneSpec(i, type, "/sys/class/thermal/thermal_zone$i/temp") }

    @Test fun skipsDroopMonitorListedFirst() {
        val picked = BoardSensor.pick(zones("socd", "pm8350b-bcl-lvl0", "cpu-0-0", "quiet_therm", "sys_therm1"))
        assertEquals("quiet_therm", picked?.type)
        assertEquals("Board", BoardSensor.label(picked!!.type))
    }

    @Test fun prefersBoardOverPackAndPmic() {
        assertEquals("board-sensor", BoardSensor.pick(zones("pmic_tz", "batt_therm", "board-sensor"))?.type)
        assertEquals("batt_therm", BoardSensor.pick(zones("pmic_tz", "batt_therm"))?.type)
    }

    @Test fun labelsPackAndPmic() {
        assertEquals("Battery", BoardSensor.label("batt_therm"))
        assertEquals("PMIC", BoardSensor.label("pmic_tz"))
    }

    @Test fun noTemperatureZoneMeansNoSensor() {
        assertNull(BoardSensor.pick(zones("socd", "soc", "pm8350b-ibat-lvl0", "vbat_lvl")))
    }
}
