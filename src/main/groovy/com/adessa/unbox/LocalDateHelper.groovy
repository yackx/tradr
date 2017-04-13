package com.adessa.unbox

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Helper methods related {@link LocalDate} and {@link LocalDateTime}.
 * <p/>
 * All methods in this class are {@code null}-safe.
 */
class LocalDateHelper {

    static DateTimeFormatter DATE_SHORT_FORMAT = DateTimeFormatter.ofPattern('dd/MM/yyyy')
    static DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    /**
     * Convert a ISO-8061 date into a {@link LocalDate}
     * @param s Date as a ISO-8061 string
     * @return Corresponding {@link LocalDate}
     */
    static LocalDate toLocalDate(String s) {
        if (!s) return null
        LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /**
     * Convert a {@link LocalDate} to a ISO-8061 string
     * @param date Local date
     * @return Corresponding {@link String} representation
     */
    static String fromLocalDate(LocalDate date) {
        if (!date) return null
        date.toString()
    }

    /**
     * Convert a {@link LocalDate} to a human-readable short date as a string
     * @param date Local date
     * @return Corresponding {@link String} representation
     */
    static String shortenLocalDate(LocalDate date) {
        if (!date) return null
        date.format(DATE_SHORT_FORMAT)
    }

    /**
     * Convert a {@link LocalDateTime} to a human-readable short date as a string
     * @param date Local datetime
     * @return Corresponding {@link String} representation
     */
    static String shortenLocalDateTime(LocalDateTime dateTime) {
        if (!dateTime) return null
        dateTime.format(DATE_SHORT_FORMAT)
    }

    /**
     * Convert a ISO-8061 datetime into a {@link LocalDateTime}
     * <p/>
     * This method is lenient and will also accept a {@code date} as a parameter.
     * In that case, the time will be set to midnight.
     *
     * @param s Date as a ISO-8061 string
     * @return Corresponding {@link LocalDateTime}
     */
    static LocalDateTime toLocalDateTime(String s) {
        if (!s) return null
        try {
            LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (DateTimeParseException ignored) {
            // Fallback on Date without Time
            LocalDate dateTime = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
            dateTime.atTime(0, 0, 0)
        }
    }

    /**
     * Convert a {@link LocalDateTime} to a ISO-8061 string
     * @param date Local date time
     * @return Corresponding {@link String} representation
     */
    static String fromLocalDateTime(LocalDateTime dateTime) {
        if (!dateTime) return null
        dateTime.format(DATE_TIME_FORMAT)
    }
}
