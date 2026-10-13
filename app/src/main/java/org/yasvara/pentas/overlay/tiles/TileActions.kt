/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.overlay.tiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import org.yasvara.pentas.utils.rememberDrawablePainter
import kotlinx.coroutines.delay

/**
 * Resolves the visible painter for a tile: live drawable (TileService tiles)
 * when present, otherwise the static resource drawable.
 */
@Composable
fun tilePainter(tile: TileAction): Painter {
    val drawable = tile.iconDrawable
    return if (drawable != null) rememberDrawablePainter(drawable)
    else painterResource(tile.icon)
}

/** Base tile interface for overlay quick toggles and tool grid items. */
sealed interface TileAction {
    val id: String
    val label: String
    val icon: Int

    /** Live drawable icon (TileService tiles); when non-null it wins over [icon]. */
    val iconDrawable: android.graphics.drawable.Drawable? get() = null

    /** Grouping label shown in the tile pool, e.g. "System" or an app name. */
    val group: String get() = ""

    @Composable fun observeEnabled(): State<Boolean>
    fun toggle() {}
    fun onLongClick() {}
}

/** Toggleable tile backed by a boolean state flow/mutableState. */
class ToggleableTile(
    override val id: String,
    override val label: String,
    override val icon: Int,
    val state: State<Boolean>,
    val setter: (Boolean) -> Unit,
    val onLongClickAction: (() -> Unit)? = null,
) : TileAction {
    @Composable
    override fun observeEnabled(): State<Boolean> = state

    override fun toggle() { setter(!state.value) }

    override fun onLongClick() {
        onLongClickAction?.invoke()
    }
}

/** One-shot action tile (screenshot, cast). */
class FixedActionTile(
    override val id: String,
    override val label: String,
    override val icon: Int,
    private val onLongClickAction: (() -> Unit)? = null,
    val action: () -> Unit,
) : TileAction {
    private val alwaysOff = mutableStateOf(false)

    @Composable
    override fun observeEnabled(): State<Boolean> = alwaysOff

    override fun toggle() { action() }

    override fun onLongClick() { onLongClickAction?.invoke() }
}

/**
 * Live system-state toggle backed by a real platform switch (Wi-Fi / Mobile Data).
 * State is read directly from the platform and refreshed once a second while the
 * chip is composed, so it tracks external changes (QS panel, settings, etc.).
 */
abstract class PollingToggleTile(
    override val id: String,
    override val label: String,
    override val icon: Int,
) : TileAction {
    private val state = mutableStateOf(readState())

    protected abstract fun readState(): Boolean
    protected abstract fun writeState(enabled: Boolean)

    @Composable
    override fun observeEnabled(): State<Boolean> {
        LaunchedEffect(Unit) {
            while (true) {
                state.value = readState()
                delay(1000)
            }
        }
        return state
    }

    override fun toggle() {
        writeState(!readState())
        state.value = readState()
    }
}

/** Wi-Fi quick toggle — flips WifiManager.isWifiEnabled directly. */
class WifiTile(
    override val id: String,
    override val label: String,
    override val icon: Int,
    private val wifiManager: android.net.wifi.WifiManager,
    private val onDetails: () -> Unit,
) : PollingToggleTile(id, label, icon) {
    override fun onLongClick() = onDetails()

    override fun readState(): Boolean =
        runCatching { wifiManager.isWifiEnabled }.getOrDefault(false)

    override fun writeState(enabled: Boolean) {
        runCatching { wifiManager.setWifiEnabled(enabled) }
    }
}

/** Mobile Data quick toggle — flips TelephonyManager.setDataEnabled directly. */
class MobileDataTile(
    override val id: String,
    override val label: String,
    override val icon: Int,
    private val telephonyManager: android.telephony.TelephonyManager,
) : PollingToggleTile(id, label, icon) {
    override fun readState(): Boolean =
        runCatching { telephonyManager.isDataEnabled }.getOrDefault(false)

    @Suppress("DEPRECATION")
    override fun writeState(enabled: Boolean) {
        runCatching { telephonyManager.setDataEnabled(enabled) }
    }
}