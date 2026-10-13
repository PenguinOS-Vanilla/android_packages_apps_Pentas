/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.cache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Byte-budgeted (24 MB) banner cache; decodes downsampled to RGB_565 off the main thread. */
@Singleton
class BannerBitmapCache @Inject constructor() {

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    /** Sampled fetch. [reqW] is the max width needed on screen (px). */
    fun get(path: String, reqW: Int = 600): Bitmap? {
        val f = File(path)
        if (!f.exists()) return null
        val key = "${f.absolutePath}:${f.lastModified()}:$reqW"
        cache.get(key)?.let { return it }
        val bmp = decodeSampled(f.absolutePath, reqW) ?: return null
        cache.put(key, bmp)
        return bmp
    }


    fun invalidate(path: String) {
        // Remove all sampled variants for this file (LruCache has no snapshot() on this version).
        // Brute-force: try common reqW variants + bare path.
        cache.remove(path)
        cache.remove("$path:${File(path).lastModified()}:600")
        // Fallback: evict by exact key if caller passed absolute path.
        try { cache.remove(key(File(path))) } catch (_: Exception) {}
    }

    fun evictAll() = cache.evictAll()
    fun trimToSize(maxBytes: Int) = cache.trimToSize(maxBytes)

    private fun decodeSampled(absPath: String, reqW: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(absPath, bounds)
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) {
                // Fallback: file is not an image or header unreadable.
                return runCatching { BitmapFactory.decodeFile(absPath) }.getOrNull()
            }
            var sample = 1
            // Power-of-two sampling keeps decoder fast; any integer works on modern Android.
            while (w / (sample * 2) >= reqW && h / (sample * 2) >= reqW / 2) sample *= 2
            // Refine for non-power-of-two remainders (e.g. 1920→600 needs 3, not 2).
            val exactSample = maxOf(1, minOf(w / reqW.coerceAtLeast(1), h / (reqW / 2).coerceAtLeast(1)))
            if (exactSample > sample) sample = exactSample
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.RGB_565
                inJustDecodeBounds = false
            }
            runCatching { BitmapFactory.decodeFile(absPath, opts) }.getOrNull()
                ?: runCatching { BitmapFactory.decodeFile(absPath) }.getOrNull()
        } catch (_: OutOfMemoryError) {
            // OOM on decode: evict half the cache and give up on this banner.
            try { cache.trimToSize(cache.maxSize() / 2) } catch (_: Exception) {}
            null
        } catch (_: Exception) { null }
    }

    private fun key(f: File) = "${f.absolutePath}:${f.lastModified()}"
}
