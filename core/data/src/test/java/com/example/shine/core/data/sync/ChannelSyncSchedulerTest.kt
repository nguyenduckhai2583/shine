package com.example.shine.core.data.sync

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ChannelSyncSchedulerTest {

    private val context = mock(Context::class.java)
    private val scheduler = ChannelSyncScheduler(context)

    @Test
    fun calculateDelayUntilNextTargetTime_whenNowIsBeforeTarget_calculatesDelayForSameDay() {
        val now = LocalDateTime.of(2026, 11, 13, 5, 0, 0, 0)

        val delayMillis = scheduler.calculateDelayUntilNextTargetTime(
            targetHour = 7,
            targetMinute = 0,
            now = now,
        )

        val expectedDelayMillis = TimeUnit.HOURS.toMillis(2)
        assertEquals(expectedDelayMillis, delayMillis)
    }

    @Test
    fun calculateDelayUntilNextTargetTime_whenNowIsAfterTarget_calculatesDelayForNextDay() {
        val now = LocalDateTime.of(2026, 11, 13, 14, 30, 0, 0)

        val delayMillis = scheduler.calculateDelayUntilNextTargetTime(
            targetHour = 7,
            targetMinute = 0,
            now = now,
        )

        // From 14:30 today to 07:00 tomorrow = 16h30m
        val expectedDelayMillis = TimeUnit.HOURS.toMillis(16) + TimeUnit.MINUTES.toMillis(30)
        assertEquals(expectedDelayMillis, delayMillis)
    }

    @Test
    fun calculateDelayUntilNextTargetTime_whenNowIsExactlyTargetTime_calculatesDelayForNextDay() {
        // 07:00:00.001 (1 millisecond after 07:00:00)
        val now = LocalDateTime.of(2026, 11, 13, 7, 0, 0, 1_000_000)

        val delayMillis = scheduler.calculateDelayUntilNextTargetTime(
            targetHour = 7,
            targetMinute = 0,
            now = now,
        )

        val expectedDelayMillis = TimeUnit.HOURS.toMillis(24) - 1
        assertEquals(expectedDelayMillis, delayMillis)
    }

    @Test
    fun calculateDelayUntilNextTargetTime_customTargetHourAndMinute() {
        val now = LocalDateTime.of(2026, 11, 13, 8, 0, 0, 0)

        val delayMillis = scheduler.calculateDelayUntilNextTargetTime(
            targetHour = 8,
            targetMinute = 45,
            now = now,
        )

        val expectedDelayMillis = TimeUnit.MINUTES.toMillis(45)
        assertEquals(expectedDelayMillis, delayMillis)
    }

    @Test
    fun calculateDelayUntilNextTargetTime_invalidHour_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            scheduler.calculateDelayUntilNextTargetTime(targetHour = 24, targetMinute = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            scheduler.calculateDelayUntilNextTargetTime(targetHour = -1, targetMinute = 0)
        }
    }

    @Test
    fun calculateDelayUntilNextTargetTime_invalidMinute_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            scheduler.calculateDelayUntilNextTargetTime(targetHour = 7, targetMinute = 60)
        }
        assertThrows(IllegalArgumentException::class.java) {
            scheduler.calculateDelayUntilNextTargetTime(targetHour = 7, targetMinute = -1)
        }
    }
}
