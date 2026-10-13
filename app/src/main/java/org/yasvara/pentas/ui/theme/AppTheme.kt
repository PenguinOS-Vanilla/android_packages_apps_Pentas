/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import org.yasvara.pentas.ui.kit.Kit

/** Every Pentas screen: system light/dark, Monet colour, pure black in dark, Google Sans Flex. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PentasTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(ctx).copy(background = Color.Black, surface = Color.Black)
    } else dynamicLightColorScheme(ctx)
    MaterialExpressiveTheme(scheme, MotionScheme.expressive(), typography = sansTypography(), content = content)
}

private fun TextStyle.sans() = copy(fontFamily = Kit.Sans)

fun sansTypography(): Typography = Typography().run {
    Typography(
        displayLarge.sans(), displayMedium.sans(), displaySmall.sans(),
        headlineLarge.sans(), headlineMedium.sans(), headlineSmall.sans(),
        titleLarge.sans(), titleMedium.sans(), titleSmall.sans(),
        bodyLarge.sans(), bodyMedium.sans(), bodySmall.sans(),
        labelLarge.sans(), labelMedium.sans(), labelSmall.sans(),
    )
}
