package com.example.shine.core.network.socket

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SocketEventDeduplicatorTest {

    private lateinit var deduplicator: SocketEventDeduplicatorImpl

    @Before
    fun setUp() {
        deduplicator = SocketEventDeduplicatorImpl()
    }

    @Test
    fun nullOrEmptyEventId_neverConsideredDuplicate() {
        assertFalse(deduplicator.isDuplicate(null))
        assertFalse(deduplicator.isDuplicate(null))
        assertFalse(deduplicator.isDuplicate(""))
        assertFalse(deduplicator.isDuplicate(""))
    }

    @Test
    fun firstEventId_notDuplicate_secondWithSameId_isDuplicate() {
        assertFalse(deduplicator.isDuplicate("evt-1"))
        assertTrue(deduplicator.isDuplicate("evt-1"))
        assertFalse(deduplicator.isDuplicate("evt-2"))
    }

    @Test
    fun clear_resetsSeenEvents() {
        assertFalse(deduplicator.isDuplicate("evt-1"))
        assertTrue(deduplicator.isDuplicate("evt-1"))

        deduplicator.clear()

        assertFalse(deduplicator.isDuplicate("evt-1"))
    }
}
