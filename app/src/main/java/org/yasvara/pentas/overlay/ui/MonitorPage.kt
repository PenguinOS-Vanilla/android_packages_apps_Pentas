/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.overlay.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.yasvara.pentas.R
import org.yasvara.pentas.overlay.LocalOverlay
import org.yasvara.pentas.overlay.monitor.Widget
import org.yasvara.pentas.ui.kit.Block
import org.yasvara.pentas.ui.kit.Glyph
import org.yasvara.pentas.ui.kit.Kit
import org.yasvara.pentas.ui.kit.RoundButton
import org.yasvara.pentas.ui.kit.SettingRow
import org.yasvara.pentas.ui.kit.Slider
import org.yasvara.pentas.ui.kit.Toggle
import org.yasvara.pentas.ui.kit.label
import org.yasvara.pentas.ui.kit.num

/** The FPS tile opened up: the frame-rate graph and the load behind it. */
@Composable
fun MonitorPage() {
    val o = LocalOverlay.current
    val cs = MaterialTheme.colorScheme
    val s by o.bus.snapshot.collectAsState()
    val fps by o.fps.realFpsHistory.collectAsState()
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(Kit.Gap)) {
        Block(padding = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${fps.lastOrNull()?.toInt() ?: 0}", style = num(28.sp, 620), color = cs.primary)
                Text("fps", style = label(11.sp), color = cs.onSurfaceVariant)
                if (s.thermalHeadroomReduced) Text(
                    "Throttled", Modifier.clip(CircleShape).background(cs.errorContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                    style = label(10.5.sp, 600), color = cs.onErrorContainer,
                )
                Spacer(Modifier.weight(1f))
                RoundButton(R.drawable.sym_close_fullscreen, "Collapse", { o.state.home() })
            }
            val shown = fps.filter { it > 0 }
            if (shown.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Min" to shown.min(), "Avg" to shown.average().toFloat(), "Max" to shown.max()).forEach { (k, v) ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$k ", style = label(10.5.sp), color = cs.onSurfaceVariant)
                        Text("${v.toInt()}", style = num(12.sp, 600), color = cs.onSurface)
                    }
                }
            }
            FpsGraph(fps, o.fps.maxRefreshRate)
        }
        Block(padding = 10.dp) {
            LoadRow("CPU", "${s.cpuUsageTotal}%", "%.1f GHz".format(s.cpuFreqMaxKhz / 1e6f), s.cpuTempC, 70f)
            LoadRow("GPU", "${s.gpuUsage}%", "${s.gpuFreqMhz} MHz", s.gpuTempC, 70f)
            val swapFull = s.swapTotalGb > 0 && s.swapUsedGb / s.swapTotalGb > 0.95f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RAM", Modifier.width(36.dp), style = label(11.sp, 600), color = cs.onSurfaceVariant)
                Text("%.1f / %.0f GB".format(s.ramUsedGb, kotlin.math.ceil(s.ramTotalGb)), style = num(12.sp, 600), color = cs.onSurface)
                Spacer(Modifier.weight(1f))
                if (s.swapTotalGb > 0) Text(
                    if (swapFull) "swap full" else "swap %.1f".format(s.swapUsedGb),
                    style = label(11.sp), color = if (swapFull) cs.error else cs.onSurfaceVariant,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("On screen", style = label(11.sp, 600), color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.weight(1f))
            Widget.entries.forEach { w ->
                val on = o.widgets.settings.enabled(w)
                Text(
                    if (w == Widget.PROCESSES) "Proc" else if (w == Widget.TEMP) "Temp" else w.label,
                    Modifier.clip(RoundedCornerShape(if (on) 14.dp else 8.dp))
                        .background(if (on) cs.secondaryContainer else cs.surfaceContainerHigh)
                        .clickable { o.widgets.settings.setEnabled(w, !on) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    style = label(11.sp, if (on) 600 else 500),
                    color = if (on) cs.onSecondaryContainer else cs.onSurfaceVariant,
                )
            }
            RoundButton(R.drawable.sym_edit, "Widget settings", { o.state.open(org.yasvara.pentas.overlay.Page.WIDGETS) }, size = 28.dp)
        }
    }
}

@Composable
private fun LoadRow(name: String, load: String, clock: String, temp: Float, hot: Float) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.width(36.dp), style = label(11.sp, 600), color = cs.onSurfaceVariant)
        Text(load, Modifier.width(44.dp), style = num(12.sp, 600), color = cs.onSurface)
        Text(clock, style = num(12.sp, 500), color = cs.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("%.1f°".format(temp), style = num(12.sp, 600), color = if (temp >= hot) cs.error else cs.onSurface)
    }
}

/** FPS over the last minute with 30/45/60 guides and a dashed average. */
@Composable
private fun FpsGraph(history: List<Float>, hz: Float) {
    val cs = MaterialTheme.colorScheme
    val top = maxOf(60f, history.maxOrNull() ?: 0f, if (hz <= 90f) hz else 90f) + 5f
    Box(Modifier.fillMaxWidth().height(96.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            fun y(v: Float) = size.height - v / top * size.height
            listOf(30f, 45f, 60f).filter { it < top }.forEach { g ->
                drawLine(cs.outlineVariant, Offset(0f, y(g)), Offset(size.width, y(g)), 1.dp.toPx())
            }
            if (history.size < 2) return@Canvas
            val dx = size.width / (history.size - 1)
            val path = Path()
            history.forEachIndexed { i, v -> if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * dx, y(v)) }
            val avg = history.average().toFloat()
            drawLine(cs.tertiary, Offset(0f, y(avg)), Offset(size.width, y(avg)), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
            drawPath(path, cs.primary, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Settings for the floating widgets; open widgets can be dragged meanwhile. */
@Composable
fun WidgetsPage() {
    val o = LocalOverlay.current
    val cs = MaterialTheme.colorScheme
    val set = o.widgets.settings
    DisposableEffect(Unit) {
        o.widgets.editing.value = true
        onDispose { o.widgets.editing.value = false }
    }
    SubPage("On-screen widgets", trailing = {
        Text("${set.count} on", style = label(11.sp), color = cs.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
    }) {
        Widget.entries.forEachIndexed { i, w ->
            Block(shape = Kit.connected(i, Widget.entries.size), padding = 10.dp) {
                SettingRow(w.label) { Toggle(set.enabled(w), { set.setEnabled(w, it) }) }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    w.fields.forEach { f ->
                        val on = set.shows(w, f)
                        Text(
                            f.label,
                            Modifier.clip(RoundedCornerShape(if (on) 10.dp else 6.dp))
                                .background(if (on) cs.secondaryContainer else cs.surfaceContainerHigh)
                                .clickable { set.setShows(w, f, !on) }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            style = label(10.5.sp, if (on) 600 else 500),
                            color = if (on) cs.onSecondaryContainer else cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Block(padding = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Opacity", style = label(12.sp), color = cs.onSurface)
                Slider(set.opacity, { set.opacity = it }, Modifier.weight(1f), range = 0.3f..1f)
                Text("${(set.opacity * 100).toInt()}%", style = num(12.sp, 600), color = cs.onSurface)
            }
        }
        Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Glyph(R.drawable.sym_drag_pan, cs.onSurfaceVariant, 14.dp)
            Text("Drag a widget to move it", style = label(11.sp), color = cs.onSurfaceVariant)
        }
    }
}

