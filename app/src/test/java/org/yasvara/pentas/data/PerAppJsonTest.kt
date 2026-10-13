/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerAppJsonTest {

    private val pkg = "com.example.game"
    private val other = "com.other.game"

    @Test
    fun putThenGetRoundTripsAllTypes() {
        var json = PerAppJson.put(null, pkg, 3)
        assertEquals(3, PerAppJson.getInt(json, pkg, 0))

        json = PerAppJson.put(json, other, "0.5")
        assertEquals("0.5", PerAppJson.getString(json, other, "auto"))
        assertEquals(3, PerAppJson.getInt(json, pkg, 0))

        json = PerAppJson.put(json, pkg, true)
        assertTrue(PerAppJson.getBoolean(json, pkg, false))
    }

    @Test
    fun nullValueRemovesEntryOnly() {
        var json = PerAppJson.put(null, pkg, 2)
        json = PerAppJson.put(json, other, 4)
        json = PerAppJson.put(json, pkg, null)

        assertFalse(PerAppJson.has(json, pkg))
        assertTrue(PerAppJson.has(json, other))
        assertEquals(0, PerAppJson.getInt(json, pkg, 0))
        assertEquals(4, PerAppJson.getInt(json, other, 0))
    }

    @Test
    fun missingEntryFallsBackToDefault() {
        val json = PerAppJson.put(null, other, 1)
        assertEquals(7, PerAppJson.getInt(json, pkg, 7))
        assertEquals("auto", PerAppJson.getString(json, pkg, "auto"))
        assertTrue(PerAppJson.getBoolean(json, pkg, true))
    }

    @Test
    fun corruptJsonBehavesAsEmptyMap() {
        val corrupt = "{not json!!"
        assertEquals(5, PerAppJson.getInt(corrupt, pkg, 5))
        assertFalse(PerAppJson.has(corrupt, pkg))

        // Writing to a corrupt document starts a clean map instead of throwing
        val json = PerAppJson.put(corrupt, pkg, 2)
        assertEquals(2, PerAppJson.getInt(json, pkg, 0))
    }

    @Test
    fun nullAndEmptyInputAreEmptyMaps() {
        assertFalse(PerAppJson.has(null, pkg))
        assertEquals("x", PerAppJson.getString(null, pkg, "x"))
        val json = PerAppJson.put(null, pkg, null)
        assertEquals("{}", json)
    }
}
