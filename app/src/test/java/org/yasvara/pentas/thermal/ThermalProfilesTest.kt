/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.thermal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalProfilesTest {
    @Test fun everyBuiltinShowsAsAnOfferedPreset() {
        ThermalProfiles.PROFILE_NAMES.indices.forEach { index ->
            val shown = ThermalProfiles.canonicalIndex(index)
            assertTrue("$index → $shown", shown in ThermalProfiles.UNIQUE_PROFILE_INDICES)
        }
    }

    @Test fun aliasesKeepTheirSconfig() {
        ThermalProfiles.PROFILE_NAMES.indices.forEach { index ->
            assertEquals(ThermalProfiles.PROFILE_SCONFIG[index],
                ThermalProfiles.PROFILE_SCONFIG[ThermalProfiles.canonicalIndex(index)])
        }
    }

    @Test fun customProfilesPassThrough() {
        val custom = ThermalProfiles.CUSTOM_PROFILE_BASE + 2
        assertEquals(custom, ThermalProfiles.canonicalIndex(custom))
    }
}
