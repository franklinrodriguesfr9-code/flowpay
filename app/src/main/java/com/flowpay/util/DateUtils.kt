package com.flowpay.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import com.flowpay.data.ReminderRepeatUnit

object DateUtils {
    private val brDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    fun dueDate(year: Int, month: Int, dueDay: Int): LocalDate {
        val yearMonth = YearMonth.of(year, month)
        val safeDay = dueDay.coerceIn(1, yearMonth.lengthOfMonth())
        return yearMonth.atDay(safeDay)
    }

    fun dueAtMillis(year: Int, month: Int, dueDay: Int, hour: Int, minute: Int): Long {
        val date = dueDate(year, month, dueDay)
        return date.atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun millisForDate(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun endOfDayMillis(date: LocalDate): Long =
        date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun millisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun formatDate(millis: Long): String = millisToLocalDate(millis).format(brDate)

    fun parseDateOrNull(text: String): LocalDate? {
        val trimmed = text.trim()
        return runCatching { LocalDate.parse(trimmed, isoDate) }.getOrNull()
            ?: runCatching { LocalDate.parse(trimmed, brDate) }.getOrNull()
    }

    fun formatIsoDate(date: LocalDate): String = date.format(isoDate)

    fun monthBounds(year: Int, month: Int): Pair<Long, Long> {
        val start = YearMonth.of(year, month).atDay(1)
        val end = YearMonth.of(year, month).atEndOfMonth()
        return millisForDate(start) to endOfDayMillis(end)
    }

    fun nextReminderMillis(
        dueAt: Long,
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis(),
        repeatUnit: ReminderRepeatUnit = ReminderRepeatUnit.MINUTE,
        repeatInterval: Int = 5,
    ): Long {
        if (dueAt > nowMillis) return dueAt
        if (repeatInterval > 0) {
            return nowMillis + repeatMillis(repeatUnit, repeatInterval)
        }
        return nextDailyReminderMillis(hour, minute, nowMillis)
    }

    fun repeatMillis(unit: ReminderRepeatUnit, interval: Int): Long {
        val bounded = interval.coerceAtLeast(1).toLong()
        return when (unit) {
            ReminderRepeatUnit.MINUTE -> bounded * 60_000L
            ReminderRepeatUnit.HOUR -> bounded * 60 * 60_000L
            ReminderRepeatUnit.DAY -> bounded * 24 * 60 * 60_000L
        }
    }

    fun nextDailyReminderMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val todayAtTime = LocalDateTime.of(
            now.toLocalDate(),
            LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)),
        ).atZone(zone)
        val next = if (todayAtTime.toInstant().toEpochMilli() > nowMillis) {
            todayAtTime
        } else {
            todayAtTime.plusDays(1)
        }
        return next.toInstant().toEpochMilli()
    }

    fun daysLate(dueAt: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        val dueDate = millisToLocalDate(dueAt)
        val today = millisToLocalDate(nowMillis)
        return ChronoUnit.DAYS.between(dueDate, today).coerceAtLeast(0)
    }
}
