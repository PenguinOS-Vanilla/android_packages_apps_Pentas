/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.session.danmaku

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification

/**
 * Feeds new, dismissible notifications to [DanmakuOverlay] as one-line banners. It is registered
 * with the system only for the length of a game session, never declared in the manifest.
 */
class DanmakuListener(
    private val overlay: DanmakuOverlay,
    private val game: String,
) : NotificationListenerService() {
    private val seen = HashSet<String>()

    override fun onListenerConnected() {
        // What is already in the shade must not replay as banners.
        // A session that ends first has already unregistered us, and the call is then refused.
        try {
            activeNotifications.orEmpty().forEach { sbn -> textOf(sbn)?.let(seen::add) }
        } catch (_: SecurityException) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        if (!popsUp(sbn, rankingMap)) return
        val text = textOf(sbn) ?: return
        if (seen.add(text)) overlay.show(text)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        textOf(sbn)?.let(seen::remove)
    }

    /**
     * Only what the heads-up being replaced would have shown: important enough to pop up and let
     * through by DND. Sensitive content (OTPs and the like) and secret notifications stay in the
     * shade, since banners can end up in recordings and streams; so do the game's own posts.
     */
    private fun popsUp(sbn: StatusBarNotification, rankingMap: RankingMap): Boolean {
        if (sbn.packageName == game || sbn.notification.visibility == Notification.VISIBILITY_SECRET) return false
        val ranking = Ranking()
        if (!rankingMap.getRanking(sbn.key, ranking)) return false
        return ranking.matchesInterruptionFilter() &&
            ranking.importance >= NotificationManager.IMPORTANCE_HIGH &&
            !ranking.hasSensitiveContent()
    }

    private fun textOf(sbn: StatusBarNotification): String? {
        if (!sbn.isClearable || sbn.isOngoing) return null
        val extras = sbn.notification.extras
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG))?.toString()?.trim().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        // A group summary repeats the app name as its title; the child posts carry the content.
        if (sbn.isGroup && title.isNotEmpty() && title.contains(overlay.labelOf(sbn.packageName), ignoreCase = true)) return null
        return buildString {
            if (title.isNotEmpty()) append('[').append(title).append(']')
            if (body.isNotEmpty()) { if (isNotEmpty()) append(' '); append(body) }
        }.takeIf { it.isNotBlank() }
    }
}
