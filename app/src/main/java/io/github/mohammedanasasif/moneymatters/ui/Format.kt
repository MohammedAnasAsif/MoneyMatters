package io.github.mohammedanasasif.moneymatters.ui

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val fullFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
private val shortFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)

/** ₹ with Indian digit grouping (1,23,456) and paise only when non-zero. */
fun formatInr(paise: Long): String {
    val positive = abs(paise)
    val rupees = (positive / 100).toString()
    val fraction = positive % 100
    val grouped = if (rupees.length <= 3) {
        rupees
    } else {
        val head = rupees.dropLast(3)
        val pairs = head.reversed().chunked(2).joinToString(",").reversed()
        "$pairs,${rupees.takeLast(3)}"
    }
    val decimals = if (fraction == 0L) "" else "." + fraction.toString().padStart(2, '0')
    return "₹$grouped$decimals"
}

fun formatDateTime(millis: Long): String =
    fullFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

fun formatDateTime(dateTime: LocalDateTime): String = fullFormat.format(dateTime)

fun formatShort(millis: Long): String =
    shortFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/** "120.5" -> 12050. Null for empty, zero, negative or more than two decimals. */
fun parseAmountPaise(text: String): Long? =
    text.trim().toBigDecimalOrNull()
        ?.takeIf { it > BigDecimal.ZERO && it.scale() <= 2 }
        ?.movePointRight(2)
        ?.toLong()
