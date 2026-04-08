package com.example.toycity.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object Formatter {
    private val numberFormat = DecimalFormat("#,##0.00")
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())

    fun formatCurrency(value: Double?): String {
        val amount = if (value == null || value.isNaN() || value.isInfinite()) 0.0 else value
        return "Rs ${numberFormat.format(amount)}"
    }

    fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }

    fun formatMonth(date: Date): String {
        return monthFormat.format(date)
    }

    fun parseMonth(monthStr: String): Date? {
        return try {
            monthFormat.parse(monthStr)
        } catch (e: Exception) {
            null
        }
    }
}
