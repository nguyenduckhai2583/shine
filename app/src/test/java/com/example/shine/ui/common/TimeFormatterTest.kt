package com.example.shine.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.DateFormat
import java.util.Date

class TimeFormatterTest {

    private val fixedNow = 1_700_000_000L

    @Test
    fun formatTimeAgo_nullOrNonPositive_returnsEmpty() {
        assertEquals("", TimeFormatter.formatTimeAgo(null, nowSeconds = fixedNow))
        assertEquals("", TimeFormatter.formatTimeAgo(0L, nowSeconds = fixedNow))
        assertEquals("", TimeFormatter.formatTimeAgo(-100L, nowSeconds = fixedNow))
    }

    @Test
    fun formatTimeAgo_lessThan60Seconds_returnsJustNow() {
        assertEquals("Just now", TimeFormatter.formatTimeAgo(fixedNow, nowSeconds = fixedNow))
        assertEquals("Just now", TimeFormatter.formatTimeAgo(fixedNow - 30, nowSeconds = fixedNow))
        assertEquals("Just now", TimeFormatter.formatTimeAgo(fixedNow - 59, nowSeconds = fixedNow))
    }

    @Test
    fun formatTimeAgo_between1And59Minutes_returnsMinutesAgo() {
        assertEquals("1m ago", TimeFormatter.formatTimeAgo(fixedNow - 60, nowSeconds = fixedNow))
        assertEquals("5m ago", TimeFormatter.formatTimeAgo(fixedNow - 300, nowSeconds = fixedNow))
        assertEquals("59m ago", TimeFormatter.formatTimeAgo(fixedNow - 3599, nowSeconds = fixedNow))
    }

    @Test
    fun formatTimeAgo_between1And23Hours_returnsHoursAgo() {
        assertEquals("1h ago", TimeFormatter.formatTimeAgo(fixedNow - 3600, nowSeconds = fixedNow))
        assertEquals("2h ago", TimeFormatter.formatTimeAgo(fixedNow - 7200, nowSeconds = fixedNow))
        assertEquals("23h ago", TimeFormatter.formatTimeAgo(fixedNow - 86399, nowSeconds = fixedNow))
    }

    @Test
    fun formatTimeAgo_between1And6Days_returnsDaysAgo() {
        assertEquals("1d ago", TimeFormatter.formatTimeAgo(fixedNow - 86400, nowSeconds = fixedNow))
        assertEquals("6d ago", TimeFormatter.formatTimeAgo(fixedNow - 86400 * 6, nowSeconds = fixedNow))
    }

    @Test
    fun formatTimeAgo_7DaysOrMore_returnsShortFormattedDate() {
        val targetSeconds = fixedNow - 86400 * 10
        val expected = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(targetSeconds * 1000))
        assertEquals(expected, TimeFormatter.formatTimeAgo(targetSeconds, nowSeconds = fixedNow))
    }

    @Test
    fun formatDateTime_formatsCorrectly() {
        assertEquals("", TimeFormatter.formatDateTime(null))
        assertEquals("", TimeFormatter.formatDateTime(0L))

        val seconds = 1_700_000_000L
        val expected = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(seconds * 1000))
        assertEquals(expected, TimeFormatter.formatDateTime(seconds))
    }
}
