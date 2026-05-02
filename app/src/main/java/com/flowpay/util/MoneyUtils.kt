package com.flowpay.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

object MoneyUtils {
    private val brCurrency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    fun format(cents: Long): String = brCurrency.format(cents / 100.0)

    fun parseToCents(text: String): Long {
        val cleaned = text
            .replace("R$", "")
            .filter { it.isDigit() || it == ',' || it == '.' || it == '-' }
            .trim()
        val normalized = when {
            cleaned.contains(',') -> cleaned.replace(".", "").replace(",", ".")
            cleaned.count { it == '.' } == 1 && cleaned.substringAfter('.').length <= 2 -> cleaned
            else -> cleaned.replace(".", "")
        }
        if (normalized.isBlank()) return 0
        return ((normalized.toDoubleOrNull() ?: 0.0) * 100).roundToLong()
    }
}
