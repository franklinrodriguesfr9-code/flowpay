package com.flowpay.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {
    @Test
    fun dueDateUsesLastDayWhenMonthIsShort() {
        assertEquals(LocalDate.of(2026, 2, 28), DateUtils.dueDate(2026, 2, 31))
    }

    @Test
    fun dueDateRespectsLeapYear() {
        assertEquals(LocalDate.of(2024, 2, 29), DateUtils.dueDate(2024, 2, 31))
    }

    @Test
    fun parsesBrazilianAndIsoDates() {
        assertEquals(LocalDate.of(2026, 5, 1), DateUtils.parseDateOrNull("2026-05-01"))
        assertEquals(LocalDate.of(2026, 5, 1), DateUtils.parseDateOrNull("01/05/2026"))
    }
}

