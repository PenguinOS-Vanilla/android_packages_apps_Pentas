/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.yasvara.pentas.R
import org.yasvara.pentas.cache.BannerBitmapCache
import org.yasvara.pentas.data.AppSettings
import org.yasvara.pentas.data.SystemSettings
import org.yasvara.pentas.data.banner.BannerSource
import org.yasvara.pentas.data.banner.GameBannerRepository
import org.yasvara.pentas.data.model.AppRef
import org.yasvara.pentas.data.repo.InstalledAppsRepository
import org.yasvara.pentas.session.SessionService
import org.yasvara.pentas.settings.fpsstats.FpsStatsActivity
import org.yasvara.pentas.ui.kit.Glyph
import org.yasvara.pentas.ui.kit.RoundButton
import org.yasvara.pentas.ui.kit.label
import org.yasvara.pentas.ui.theme.PentasTheme

/** The launcher entry: the game library. */
class LibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { PentasTheme { Library() } }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LibraryDeps {
    fun system(): SystemSettings
    fun settings(): AppSettings
    fun apps(): InstalledAppsRepository
    fun banners(): GameBannerRepository
    fun bitmaps(): BannerBitmapCache
}

private fun gamesOf(ctx: Context, system: SystemSettings): List<String> {
    val pm = ctx.packageManager
    system.syncDetected(pm)
    return system.games
        .filter { runCatching { pm.getApplicationInfo(it, 0) }.isSuccess }
        .sortedBy { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString().lowercase() }.getOrDefault(it) }
}

@Composable
private fun Library() {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val deps = remember { EntryPointAccessors.fromApplication(ctx.applicationContext, LibraryDeps::class.java) }
    var rev by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    val games by produceState(emptyList<String>(), rev) { value = withContext(Dispatchers.IO) { gamesOf(ctx, deps.system()) } }
    val live = remember(rev) { Settings.System.getString(ctx.contentResolver, SessionService.KEY_SESSION_LIVE).orEmpty() }
    Screen("Games", onBack = null, actions = {
        RoundButton(R.drawable.sym_monitoring, "Sessions", { ctx.startActivity(Intent(ctx, FpsStatsActivity::class.java)) }, size = 40.dp)
        RoundButton(R.drawable.sym_tune, "Global clocks", { ctx.startActivity(Intent(ctx, GlobalActivity::class.java)) }, size = 40.dp)
        RoundButton(R.drawable.sym_settings, "Settings", { ctx.startActivity(Intent(ctx, SettingsActivity::class.java)) }, size = 40.dp)
    }) {
        games.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { pkg -> GameCard(pkg, pkg == live, deps, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Row(
            Modifier.fillMaxWidth().height(56.dp).clip(CircleShape).background(cs.primaryContainer).clickable { adding = true }
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Glyph(R.drawable.sym_add, cs.onPrimaryContainer, 22.dp)
            Text("Add a game", style = label(15.sp, 600), color = cs.onPrimaryContainer)
        }
    }
    if (adding) AddGameSheet(deps, games.toSet(), onDone = { adding = false; rev++ })
}

@Composable
private fun GameCard(pkg: String, live: Boolean, deps: LibraryDeps, modifier: Modifier) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val pm = ctx.packageManager
    val label = remember(pkg) { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg) }
    val icon = remember(pkg) { runCatching { pm.getApplicationIcon(pkg).toBitmap(144, 144).asImageBitmap() }.getOrNull() }
    val banner by produceState(deps.banners().getCached(pkg)?.localPath?.let { deps.bitmaps().get(it) }, pkg) {
        if (value == null && deps.settings().bannerAutoFetch) {
            value = deps.banners().getOrFetch(pkg, BannerSource.fromKey(deps.settings().bannerSource))
                ?.localPath?.let { withContext(Dispatchers.IO) { deps.bitmaps().get(it) } }
        }
    }
    Box(
        modifier.aspectRatio(1.25f).clip(RoundedCornerShape(28.dp)).background(cs.surfaceContainer)
            .clickable { GameActivity.start(ctx, pkg) },
    ) {
        banner?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to cs.surfaceContainer)))
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            icon?.let { Image(it, null, Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))) }
            Text(label, style = label(14.sp, 600), color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (live) Text(
            "Playing", Modifier.align(Alignment.TopEnd).padding(10.dp).clip(CircleShape).background(cs.primary)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            style = label(11.sp, 600), color = cs.onPrimary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGameSheet(deps: LibraryDeps, existing: Set<String>, onDone: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var all by remember { mutableStateOf(emptyList<AppRef>()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { all = deps.apps().launchableApps().filter { it.packageName !in existing } }
    ModalBottomSheet(onDismissRequest = onDone, containerColor = cs.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(cs.surfaceContainerHigh).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Glyph(R.drawable.sym_search, cs.onSurfaceVariant, 20.dp)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search apps", style = label(15.sp), color = cs.onSurfaceVariant)
                    BasicTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                        textStyle = label(15.sp).copy(color = cs.onSurface), cursorBrush = SolidColor(cs.primary))
                }
            }
            all.filter { query.isBlank() || it.label.contains(query, true) }.take(60).forEach { app ->
                val bmp = remember(app.packageName) { app.icon?.toBitmap(96, 96)?.asImageBitmap() }
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                        .clickable { deps.system().addGame(app.packageName); onDone() }.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    bmp?.let { Image(it, null, Modifier.size(40.dp)) }
                    Text(app.label, style = label(15.sp, 550), color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
