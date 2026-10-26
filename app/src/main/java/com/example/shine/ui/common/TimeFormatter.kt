package com.example.shine.ui.common

import java.text.DateFormat
import java.util.Date

object TimeFormatter {

    fun formatTimeAgo(
        seconds: Long?,
        nowSeconds: Long = System.currentTimeMillis() / 1000,
    ): String {
        if (seconds == null || seconds <= 0L) return ""
        val diff = (nowSeconds - seconds).coerceAtLeast(0)
        return when {
            diff < 60 -> "Just now"
            diff < 3600 -> "${diff / 60}m ago"
            diff < 86400 -> "${diff / 3600}h ago"
            diff < 86400 * 7 -> "${diff / 86400}d ago"
            else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(seconds * 1000))
        }
    }

    fun formatDateTime(seconds: Long?): String {
        if (seconds == null || seconds <= 0L) return ""
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(seconds * 1000))
    }
}
