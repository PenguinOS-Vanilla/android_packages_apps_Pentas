/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.device

import android.os.SystemProperties

/** What the device ships beyond the stock vendor thermal stack. */
object DeviceSupport {
    /**
     * Yasvara's mi_thermal_engine reads the custom thermal curves and charging curves Pentas
     * publishes. Without it those controls change nothing, and a custom thermal profile would
     * leave the no-limits vendor config in place with no curve behind it.
     */
    val thermalDaemon: Boolean by lazy { SystemProperties.getBoolean("ro.pentas.thermal_daemon", false) }
}
