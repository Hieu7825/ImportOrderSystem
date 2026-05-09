package com.importorder.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "--";
    }

    public static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DATETIME_FMT) : "--";
    }

    public static String generateBatchId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq  = String.format("%03d", (int)(Math.random() * 999) + 1);
        return "BATCH-" + date + "-" + seq;
    }

    public static String generateSiteOrderId(String siteCode) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq  = String.format("%03d", (int)(Math.random() * 999) + 1);
        return "SORDER-" + date + "-" + siteCode + "-" + seq;
    }

    /** Sub-batch ID: SUB-{batchId}-{seq} */
    public static String generateSubBatchId(String parentBatchId) {
        String seq = String.format("%02d", (int)(Math.random() * 99) + 1);
        return "SUB-" + parentBatchId + "-" + seq;
    }

    /** Invite ID: INV-{date}-{seq} */
    public static String generateInviteId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq  = String.format("%03d", (int)(Math.random() * 999) + 1);
        return "INV-" + date + "-" + seq;
    }

    public static long daysBetween(LocalDate from, LocalDate to) {
        return java.time.temporal.ChronoUnit.DAYS.between(from, to);
    }
}