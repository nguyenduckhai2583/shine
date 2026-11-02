package com.example.shine.data.remote.socket

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles socket event deduplication using a Time-To-Live (TTL) cache.
 * Protects against duplicate event delivery when the server replays buffered events
 * during Connection State Recovery.
 */
interface SocketEventDeduplicator {
    fun isDuplicate(eventId: String?): Boolean
    fun clear()
}

@Singleton
class SocketEventDeduplicatorImpl @Inject constructor() : SocketEventDeduplicator {

    private val seenEvents = mutableMapOf<String, Long>()

    override fun isDuplicate(eventId: String?): Boolean {
        if (eventId.isNullOrEmpty()) return false
        val now = System.currentTimeMillis()
        pruneExpired(now)

        if (seenEvents.containsKey(eventId)) return true

        seenEvents[eventId] = now
        return false
    }

    override fun clear() {
        seenEvents.clear()
    }

    private fun pruneExpired(now: Long) {
        val iterator = seenEvents.entries.iterator()
        while (iterator.hasNext()) {
            val (_, timestamp) = iterator.next()
            if ((now - timestamp) > TTL_MS) {
                iterator.remove()
            }
        }
    }

    companion object {
        const val TTL_MS = 5 * 60 * 1000L // 5 minutes
    }
}
