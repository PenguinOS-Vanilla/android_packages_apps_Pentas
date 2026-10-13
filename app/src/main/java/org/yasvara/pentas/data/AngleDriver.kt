/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.data

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-app GLES driver: the platform's own ANGLE selection lists. Takes effect when the app
 * next starts.
 */
@Singleton
class AngleDriver @Inject constructor(@ApplicationContext context: Context) {
    private val resolver = context.contentResolver

    /** ANGLE ships as system libraries on this ROM; there is no APK to probe. */
    val available: Boolean by lazy { File("/system/lib64/libEGL_angle.so").exists() }

    fun get(pkg: String): String {
        val pkgs = list(KEY_PKGS)
        val i = pkgs.indexOf(pkg)
        return list(KEY_VALUES).getOrNull(i)?.takeIf { i >= 0 } ?: DEFAULT
    }

    fun set(pkg: String, choice: String) {
        val pkgs = list(KEY_PKGS).toMutableList()
        val values = list(KEY_VALUES).toMutableList()
        val i = pkgs.indexOf(pkg)
        if (i >= 0) { pkgs.removeAt(i); if (i < values.size) values.removeAt(i) }
        if (choice != DEFAULT) { pkgs += pkg; values += choice }
        Settings.Global.putString(resolver, KEY_PKGS, pkgs.joinToString(","))
        Settings.Global.putString(resolver, KEY_VALUES, values.joinToString(","))
    }

    private fun list(key: String) =
        Settings.Global.getString(resolver, key).orEmpty().split(',').filter { it.isNotEmpty() }

    companion object {
        private const val KEY_PKGS = "angle_gl_driver_selection_pkgs"
        private const val KEY_VALUES = "angle_gl_driver_selection_values"
        const val DEFAULT = "default"
        const val ANGLE = "angle"
        const val NATIVE = "native"
    }
}
