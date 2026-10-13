/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.yasvara.pentas.R
import org.yasvara.pentas.ui.kit.Kit
import org.yasvara.pentas.ui.kit.ListItem
import org.yasvara.pentas.ui.kit.RoundButton
import org.yasvara.pentas.ui.kit.label

/** App screen scaffold: back, large title, actions, scrolling content on pure background. */
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(cs.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onBack?.let { RoundButton(R.drawable.sym_arrow_back, "Back", it, size = 40.dp) }
            Spacer(Modifier.weight(1f))
            actions()
        }
        Text(title, style = label(34.sp, 560), color = cs.onSurface, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

/** A navigation row inside a connected group. */
@Composable
fun NavRow(icon: ImageVector, title: String, value: String?, index: Int, count: Int, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    ListItem(
        title = title, subtitle = value, shape = Kit.connected(index, count, 24.dp, 6.dp),
        modifier = Modifier.height(64.dp),
        leading = { Icon(icon, null, tint = cs.primary) },
        trailing = { Icon(org.yasvara.pentas.ui.kit.Sym.KeyboardArrowRight, null, tint = cs.onSurfaceVariant) },
        onClick = onClick,
    )
}

