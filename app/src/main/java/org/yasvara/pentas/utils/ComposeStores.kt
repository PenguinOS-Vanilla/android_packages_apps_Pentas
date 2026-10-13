/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.yasvara.pentas.data.PerAppSettingStore
import org.yasvara.pentas.cache.AppIconCache
import org.yasvara.pentas.cache.BannerBitmapCache
import org.yasvara.pentas.utils.di.ServiceViewEntryPoint
import dagger.hilt.android.EntryPointAccessors

private fun ep(context: android.content.Context): ServiceViewEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, ServiceViewEntryPoint::class.java)

@Composable fun rememberPerAppStore(): PerAppSettingStore {
    val ctx = LocalContext.current
    return remember(ctx.applicationContext) { ep(ctx).perAppSettingStore() }
}
@Composable fun rememberAppIconCache(): AppIconCache {
    val ctx = LocalContext.current
    return remember(ctx.applicationContext) { ep(ctx).appIconCache() }
}
@Composable fun rememberBannerBitmapCache(): BannerBitmapCache {
    val ctx = LocalContext.current
    return remember(ctx.applicationContext) { ep(ctx).bannerBitmapCache() }
}
@Composable fun rememberInstalledAppsRepository(): org.yasvara.pentas.data.repo.InstalledAppsRepository {
    val ctx = LocalContext.current
    return remember(ctx.applicationContext) { ep(ctx).installedAppsRepository() }
}
