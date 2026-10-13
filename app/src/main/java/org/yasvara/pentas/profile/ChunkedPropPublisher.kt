/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.profile

import org.yasvara.pentas.data.SettingsIO
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes JSON larger than the system-property limit as chunks. Order: chunks, side props,
 * count, then the `seq` bump the daemon waits for, then labels. `seq` is read-modify-written so a
 * restart never rewinds it.
 */
@Singleton
class ChunkedPropPublisher @Inject constructor(
    private val io: SettingsIO,
) {
    /**
     * Publish [payload] as chunks. Returns false (and writes nothing) if the
     * payload would need more than [maxChunks] chunks.
     */
    fun publish(
        payload: String,
        chunkPrefix: String,
        countProp: String,
        seqProp: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        maxChunks: Int = DEFAULT_MAX_CHUNKS,
        preCommitProps: Map<String, String> = emptyMap(),
        postCommitProps: Map<String, String> = emptyMap(),
    ): Boolean {
        val chunks = payload.chunked(chunkSize)
        if (chunks.size > maxChunks) return false
        // 1. Chunks, then blank stale ones from a longer previous payload.
        chunks.forEachIndexed { i, chunk -> io.setProp("$chunkPrefix$i", chunk) }
        val previousCount = io.readPropLong(countProp, 0L).toInt()
        for (i in chunks.size until previousCount.coerceAtMost(maxChunks)) {
            io.setProp("$chunkPrefix$i", "")
        }
        // 2. side channels — written before the commit so the daemon never sees
        //    a half-updated set when it wakes on the seq bump
        preCommitProps.forEach { (k, v) -> io.setProp(k, v) }
        // 3. count
        io.setProp(countProp, chunks.size.toString())
        // 4. commit (read-modify-write, restart-safe)
        val nextSeq = io.readPropLong(seqProp, 0L) + 1L
        io.setProp(seqProp, nextSeq.toString())
        // 5. cosmetic labels
        postCommitProps.forEach { (k, v) -> io.setProp(k, v) }
        return true
    }

    /** Switch a mode selector without touching the chunked payload. */
    fun selectMode(modeProp: String, mode: String) {
        io.setProp(modeProp, mode)
    }

    companion object {
        const val DEFAULT_CHUNK_SIZE = 88
        const val DEFAULT_MAX_CHUNKS = 64
    }
}