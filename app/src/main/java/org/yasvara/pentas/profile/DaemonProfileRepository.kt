/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.profile

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/** A profile persisted to Settings.System and applied to the Rust daemon. */
interface DaemonProfile {
    val id: String
    val name: String
}

/** Where/what to write for the chunked IPC handshake. */
data class DaemonContract(
    val seqProp: String,
    val countProp: String,
    val chunkPrefix: String,
    val chunkSize: Int = ChunkedPropPublisher.DEFAULT_CHUNK_SIZE,
    val maxChunks: Int = ChunkedPropPublisher.DEFAULT_MAX_CHUNKS,
    /** If set, applyToDaemon(null) selects this mode (e.g. "global"). */
    val modeProp: String? = null,
    val globalModeValue: String = "global",
)

/** Daemon profiles stored as a Settings.System JSON array and published as chunked properties. */
abstract class DaemonProfileRepository<T : DaemonProfile>(
    protected val context: Context,
    protected val propPublisher: ChunkedPropPublisher,
    private val storageKey: String,
    private val contract: DaemonContract,
) {
    protected abstract val tag: String
    protected abstract fun serialize(profile: T): JSONObject
    protected abstract fun deserialize(json: JSONObject): T?
    /** JSON handed to the daemon (no id/name needed by the daemon). */
    protected abstract fun daemonPayload(profile: T): String

    /** Snaps a profile to what hardware and daemon accept. Pure and idempotent. */
    protected abstract fun normalize(profile: T): T
    protected open fun preCommitProps(profile: T): Map<String, String> = emptyMap()
    protected open fun postCommitProps(): Map<String, String> = emptyMap()
    /** Hook after delete (e.g. clear dangling references). */
    protected open fun onDeleted(id: String) {}

    private val resolver get() = context.contentResolver

    fun loadAll(): List<T> {
        val raw = Settings.System.getStringForUser(resolver, storageKey, UserHandle.USER_CURRENT)
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { deserialize(arr.getJSONObject(i)) }.getOrNull()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse $storageKey", e)
            emptyList()
        }
    }

    fun getById(id: String): T? = loadAll().firstOrNull { it.id == id }

    fun save(profile: T) {
        val clean = normalize(profile)
        val all = loadAll().toMutableList()
        val idx = all.indexOfFirst { it.id == clean.id }
        if (idx >= 0) all[idx] = clean else all.add(clean)
        writeAll(all)
        Log.i(tag, "Profile saved: ${clean.name} (${clean.id})")
    }

    /** Drop every stored profile. Callers own any selection cleanup. */
    fun deleteAll() {
        Settings.System.putStringForUser(resolver, storageKey, "", UserHandle.USER_CURRENT)
        Log.i(tag, "All profiles deleted")
    }

    fun delete(id: String) {
        writeAll(loadAll().filter { it.id != id })
        onDeleted(id)
        Log.i(tag, "Profile deleted: $id")
    }

    private fun writeAll(profiles: List<T>) {
        val arr = JSONArray()
        profiles.forEach { arr.put(serialize(it)) }
        Settings.System.putStringForUser(resolver, storageKey, arr.toString(), UserHandle.USER_CURRENT)
    }

    /** Publishes [profile] as chunked properties; null selects the vendor default where supported. */
    fun applyToDaemon(profile: T?) {
        if (profile == null) {
            contract.modeProp?.let { propPublisher.selectMode(it, contract.globalModeValue) }
            Log.i(tag, "Daemon -> default/global")
            return
        }
        val clean = normalize(profile)
        val payload = daemonPayload(clean)
        val published = propPublisher.publish(
            payload = payload,
            chunkPrefix = contract.chunkPrefix,
            countProp = contract.countProp,
            seqProp = contract.seqProp,
            chunkSize = contract.chunkSize,
            maxChunks = contract.maxChunks,
            preCommitProps = preCommitProps(clean),
            postCommitProps = postCommitProps(),
        )
        if (published) Log.i(tag, "Profile '${clean.name}' applied to daemon")
        else Log.e(tag, "Profile too large: ${payload.length} bytes")
    }

    private companion object {
        const val TAG = "DaemonProfile"
    }
}