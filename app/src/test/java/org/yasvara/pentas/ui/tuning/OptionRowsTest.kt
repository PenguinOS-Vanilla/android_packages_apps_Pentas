/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.ui.tuning

import org.junit.Assert.assertEquals
import org.junit.Test

class OptionRowsTest {
    @Test fun splitsEvenlyWithinRowLimit() {
        assertEquals(listOf(3, 2, 2), (1..7).toList().balancedRows().map { it.size })
        assertEquals(listOf(3, 3, 2), (1..8).toList().balancedRows().map { it.size })
        assertEquals(listOf(2, 2), (1..4).toList().balancedRows().map { it.size })
        assertEquals(listOf(3, 2), (1..5).toList().balancedRows().map { it.size })
    }

    @Test fun keepsOrder() {
        assertEquals((1..7).toList(), (1..7).toList().balancedRows().flatten())
    }

    @Test fun emptyHasNoRows() {
        assertEquals(emptyList<List<Int>>(), emptyList<Int>().balancedRows())
    }
}
