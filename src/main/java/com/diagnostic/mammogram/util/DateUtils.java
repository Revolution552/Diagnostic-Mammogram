package com.diagnostic.mammogram.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter REPORT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");

    /**
     * Returns the current date and time formatted for reports
     * Example: "June 10, 2023 at 02:30 PM"
     */
    public static String getCurrentDateTimeFormatted() {
        return LocalDateTime.now().format(REPORT_DATE_FORMATTER);
    }

    /**
     * Formats a LocalDateTime object to the standard report format
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(REPORT_DATE_FORMATTER);
    }

    /**
     * Formats a date for patient records (YYYY-MM-DD)
     */
    public static String formatDateForRecords(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}