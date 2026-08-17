package com.identusbook.flighttix.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date

/**
 * Date helpers mirroring the iOS `Date+APIFormatter.swift` and `DateStuff.swift`.
 */
object DateUtils {

    // iOS: "yyyy-MM-dd'T'HH:mm'Z'" in UTC — the exact shape claims are sent in at issuance.
    private val apiFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneOffset.UTC)

    /** ISO-8601 string ("yyyy-MM-dd'T'HH:mm'Z'", UTC) — equivalent to Date.iso8601String(). */
    fun iso8601String(date: Date = Date()): String =
        apiFormatter.format(date.toInstant())

    /** Parse the "yyyy-MM-dd'T'HH:mm'Z'" shape back into a Date — equivalent to stringToDate(). */
    fun stringToDate(iso8601String: String): Date? = try {
        Date.from(Instant.from(apiFormatter.parse(iso8601String)))
    } catch (e: DateTimeParseException) {
        null
    }

    /**
     * Best-effort ISO string -> human display, mirroring DateStuff.displayISODateAsString().
     * Tries several ISO parsers, falling back to date-only, then a literal.
     */
    fun displayISODateAsString(isoString: String, showTime: Boolean = false): String {
        val instant: Instant? = parseFlexibleInstant(isoString)
        if (instant == null) return "No Date Format"
        val zoned = instant.atZone(ZoneId.systemDefault())
        val pattern = if (showTime) "MMMM d, yyyy 'at' h:mm a" else "MMMM d, yyyy"
        return DateTimeFormatter.ofPattern(pattern).format(zoned)
    }

    /** Format a Date as a plain date, e.g. "August 17, 2026". */
    fun displayDate(date: Date): String =
        DateTimeFormatter.ofPattern("MMMM d, yyyy")
            .format(date.toInstant().atZone(ZoneId.systemDefault()))

    private fun parseFlexibleInstant(isoString: String): Instant? {
        // Full ISO instants (with or without fractional seconds / offset).
        try {
            return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(isoString))
        } catch (_: DateTimeParseException) {
        } catch (_: Exception) {
        }
        // The app's own "yyyy-MM-dd'T'HH:mm'Z'" shape.
        try {
            return Instant.from(apiFormatter.parse(isoString))
        } catch (_: DateTimeParseException) {
        }
        // Date-only "yyyy-MM-dd".
        try {
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
            return java.time.LocalDate.parse(isoString, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(ZoneOffset.UTC).toInstant()
        } catch (_: Exception) {
        }
        return null
    }
}
