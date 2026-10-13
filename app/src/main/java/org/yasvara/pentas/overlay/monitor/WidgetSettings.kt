/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.overlay.monitor

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class Field(val key: String, val label: String) {
    FPS("fps", "FPS"), CPU("cpu", "CPU"), GPU("gpu", "GPU"), RAM("ram", "RAM"),
    TEMP("temp", "Temp"), NET("network", "Net"), BATTERY("battery", "Battery"),
    SYSTEM("system", "System processes"),
}

/** The floating widgets drawn over the game. [key] keeps the stored preferences stable. */
enum class Widget(val key: String, val label: String, val fields: List<Field>, val defaults: Set<Field>) {
    CLASSIC("classical", "Classic", listOf(Field.FPS, Field.CPU, Field.GPU, Field.RAM, Field.TEMP, Field.NET),
        setOf(Field.FPS, Field.CPU, Field.GPU, Field.RAM, Field.TEMP, Field.NET)),
    MINI("mini", "Mini", listOf(Field.FPS, Field.CPU, Field.GPU, Field.RAM),
        setOf(Field.FPS, Field.CPU, Field.GPU, Field.RAM)),
    PROCESSES("processes", "Processes", listOf(Field.SYSTEM), emptySet()),
    TEMP("temp", "Temperature", listOf(Field.CPU, Field.GPU, Field.BATTERY),
        setOf(Field.CPU, Field.GPU, Field.BATTERY)),
}

@Singleton
class WidgetSettings @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("monitor_settings", Context.MODE_PRIVATE)

    private val on = mutableStateMapOf<Widget, Boolean>().apply {
        Widget.entries.forEach { put(it, prefs.getBoolean("${it.key}_monitor_enabled", false)) }
    }
    private val shown = mutableStateMapOf<Pair<Widget, Field>, Boolean>().apply {
        Widget.entries.forEach { w ->
            w.fields.forEach { f -> put(w to f, prefs.getBoolean("${w.key}_show_${f.key}", f in w.defaults)) }
        }
    }
    private val _opacity = mutableFloatStateOf(prefs.getFloat("monitors_opacity", 0.85f))

    fun enabled(w: Widget) = on[w] == true
    fun setEnabled(w: Widget, value: Boolean) {
        on[w] = value; prefs.edit { putBoolean("${w.key}_monitor_enabled", value) }
    }

    fun shows(w: Widget, f: Field) = shown[w to f] == true
    fun setShows(w: Widget, f: Field, value: Boolean) {
        shown[w to f] = value; prefs.edit { putBoolean("${w.key}_show_${f.key}", value) }
    }

    var opacity: Float
        get() = _opacity.floatValue
        set(v) { _opacity.floatValue = v; prefs.edit { putFloat("monitors_opacity", v) } }

    val count: Int get() = Widget.entries.count(::enabled)
    val needsFps: Boolean get() = (enabled(Widget.CLASSIC) && shows(Widget.CLASSIC, Field.FPS)) ||
        (enabled(Widget.MINI) && shows(Widget.MINI, Field.FPS))

    fun position(w: Widget): Pair<Int, Int> =
        prefs.getFloat("${w.key}_x", 100f).toInt() to prefs.getFloat("${w.key}_y", 100f + 120f * w.ordinal).toInt()

    fun savePosition(w: Widget, x: Int, y: Int) =
        prefs.edit { putFloat("${w.key}_x", x.toFloat()); putFloat("${w.key}_y", y.toFloat()) }
}
