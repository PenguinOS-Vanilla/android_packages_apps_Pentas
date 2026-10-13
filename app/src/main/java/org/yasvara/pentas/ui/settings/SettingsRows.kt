/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.ui.settings

import org.yasvara.pentas.ui.kit.Sym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ── Section header (uppercase label, primary color) ────────────────────

// ── Grouped card (satu card per grup, rows packed di dalam) ────────────

// ── Switch row ─────────────────────────────────────────────────────────

// ── Segmented row (≤4 opsi) ────────────────────────────────────────────

// ── Dropdown row (banyak opsi / label panjang) ─────────────────────────

@Composable
fun <T> SettingsDropdownRow(
    label: String,
    valueText: String,
    valueColor: Color = MaterialTheme.colorScheme.primary,
    options: List<T>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    leading: @Composable () -> Unit = {},
    renderItem: @Composable (T) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var anchorWidthPx by remember { mutableStateOf(0) }
    val menuModifier = if (anchorWidthPx > 0) {
        Modifier.width(with(density) { anchorWidthPx.toDp() })
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { anchorWidthPx = it.size.width },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) valueColor
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Sym.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = menuModifier,
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { renderItem(opt) },
                    onClick = { onSelect(opt); expanded = false },
                    trailingIcon = if (isSelected(opt)) {
                        {
                            Icon(
                                Sym.Check, null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else null,
                )
            }
        }
    }
}

// ── Slider row (continuous) ────────────────────────────────────────────

// ── Int slider row (discrete) ──────────────────────────────────────────

@Composable
fun SettingsIntSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    onCommit: (Int) -> Unit,
    valueText: (Int) -> String = { it.toString() },
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var pos by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val cur = pos.roundToInt().coerceIn(range.first, range.last)
    val steps = (range.last - range.first - 1).coerceAtLeast(0)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = valueText(cur),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = pos,
            onValueChange = { pos = it },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps,
            enabled = enabled,
            onValueChangeFinished = {
                onCommit(pos.roundToInt().coerceIn(range.first, range.last))
            },
            modifier = Modifier.fillMaxWidth().height(30.dp),
        )
    }
}

// ── Link row (navigasi ke layar lain) ──────────────────────────────────

// ── Info row (label + value, read-only) ────────────────────────────────

// ── Float slider with custom value display ──────────────────────────
@Composable
fun SettingsValueSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onCommit: (Float) -> Unit,
    valueText: (Float) -> String = { "%.1f".format(it) },
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var pos by remember(value) { mutableFloatStateOf(value) }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText(pos),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = pos,
            onValueChange = { pos = it },
            valueRange = range,
            enabled = enabled,
            onValueChangeFinished = { onCommit(pos) },
            modifier = Modifier.fillMaxWidth().height(28.dp),
        )
    }
}

// ── Int slider with suffix (covers StudioSlider) ────────────────────
@Composable
fun SettingsSuffixSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String = "",
    onCommit: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    SettingsIntSliderRow(
        label = label,
        value = value,
        range = range,
        onCommit = onCommit,
        valueText = { "$it$suffix" },
        enabled = enabled,
        modifier = modifier,
    )
}

// ── Frequency picker dropdown (covers FreqPicker) ───────────────────
@Composable
fun SettingsFreqPickerRow(
    label: String,
    selectedValue: Long,
    table: List<Long>,
    isMhz: Boolean = false,
    onSelect: (Long) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val formatFn: (Long) -> String = if (isMhz) ::formatMhzLocal else ::formatKhzLocal
    val options = table.sortedDescending()
    SettingsDropdownRow(
        label = label,
        valueText = formatFn(selectedValue),
        options = options,
        isSelected = { it == selectedValue },
        onSelect = onSelect,
        enabled = enabled,
        modifier = modifier,
        renderItem = { v ->
            Text(
                text = formatFn(v),
                fontWeight = if (v == selectedValue) FontWeight.Bold else FontWeight.Normal,
                color = if (v == selectedValue) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

// Local formatters to avoid importing thermal.custom package
private fun formatKhzLocal(khz: Long): String =
    if (khz >= 1_000_000L) "%.2f GHz".format(khz / 1_000_000.0)
    else "%.0f MHz".format(khz / 1000.0)

private fun formatMhzLocal(mhz: Long): String =
    if (mhz >= 1000L) "%.2f GHz".format(mhz / 1000.0)
    else "$mhz MHz"

