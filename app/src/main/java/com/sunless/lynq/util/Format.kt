package com.sunless.lynq.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormat = DateTimeFormatter.ofPattern("HH:mm · d MMM")

/** `18:42` */
fun Long.asClock(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(clockFormat)

/** `18:42 · 30 Oct` */
fun Long.asDateTime(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(dateTimeFormat)

/** `just now`, `4 min ago`, `2 h ago`. */
fun Long.asRelative(): String {
    val delta = System.currentTimeMillis() - this
    return when {
        delta < 30_000 -> "just now"
        delta < 3_600_000 -> "${delta / 60_000} min ago"
        delta < 86_400_000 -> "${delta / 3_600_000} h ago"
        else -> "${delta / 86_400_000} d ago"
    }
}

/** First 8 characters of a fingerprint, for compact display. */
fun String.shortId(): String = if (length <= 8) this else take(8)

/** `A → B → C` */
fun List<String>.asRoute(): String = if (isEmpty()) "direct" else joinToString(" → ")
