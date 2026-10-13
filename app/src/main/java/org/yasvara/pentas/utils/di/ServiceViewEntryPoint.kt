/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.utils.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.yasvara.pentas.data.AppSettings
import org.yasvara.pentas.data.SystemSettings
import org.yasvara.pentas.thermal.PerfTuner


@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceViewEntryPoint {
    fun appSettings(): AppSettings
    fun systemSettings(): SystemSettings
    fun perfTuner(): PerfTuner
    fun chargingProfileRepository(): org.yasvara.pentas.charging.ChargingProfileRepository
    fun telemetryBus(): org.yasvara.pentas.telemetry.TelemetryBus
    fun bannerRepository(): org.yasvara.pentas.data.banner.GameBannerRepository
    fun settingsIO(): org.yasvara.pentas.data.SettingsIO
    fun perAppSettingStore(): org.yasvara.pentas.data.PerAppSettingStore
    fun activeSessionStore(): org.yasvara.pentas.session.ActiveSessionStore
    fun appIconCache(): org.yasvara.pentas.cache.AppIconCache
    fun bannerBitmapCache(): org.yasvara.pentas.cache.BannerBitmapCache
    fun installedAppsRepository(): org.yasvara.pentas.data.repo.InstalledAppsRepository
}
