/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.ui.tuning

/** Split segmented options into rows of at most [max], as even as possible (7 → 3, 2, 2). */
internal fun <T> List<T>.balancedRows(max: Int = 3): List<List<T>> {
    if (isEmpty()) return emptyList()
    val rows = (size + max - 1) / max
    val base = size / rows
    val extra = size % rows
    var start = 0
    return List(rows) { row ->
        val end = start + base + if (row < extra) 1 else 0
        subList(start, end).also { start = end }
    }
}
