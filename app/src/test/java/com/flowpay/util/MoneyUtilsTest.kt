package com.flowpay.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyUtilsTest {
    @Test
    fun parsesBrazilianCurrencyText() {
        assertEquals(123456L, MoneyUtils.parseToCents("R$ 1.234,56"))
    }

    @Test
    fun parsesSimpleDecimalText() {
        assertEquals(9990L, MoneyUtils.parseToCents("99,90"))
    }

    @Test
    fun parsesDotDecimalText() {
        assertEquals(9990L, MoneyUtils.parseToCents("99.90"))
    }
}
