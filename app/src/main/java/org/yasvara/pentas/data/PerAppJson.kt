/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.data

import org.json.JSONObject

/** `{"<pkg>": value}` maps for per-app Settings keys. A null value removes the entry; corrupt input reads as empty. */
object PerAppJson {

    private fun parse(json: String?): JSONObject = try {
        JSONObject(json ?: "{}")
    } catch (_: Exception) {
        JSONObject()
    }

    /** Returns the updated JSON document; null [value] removes the entry. */
    fun put(json: String?, pkg: String, value: Any?): String {
        val obj = parse(json)
        if (value == null) obj.remove(pkg) else obj.put(pkg, value)
        return obj.toString()
    }

    fun getString(json: String?, pkg: String, default: String): String =
        parse(json).optString(pkg, default).ifEmpty { default }

    fun getInt(json: String?, pkg: String, default: Int): Int =
        parse(json).optInt(pkg, default)

    fun getBoolean(json: String?, pkg: String, default: Boolean): Boolean =
        parse(json).optBoolean(pkg, default)

    fun has(json: String?, pkg: String): Boolean = parse(json).has(pkg)
}
