package com.fpt.ems.be.util;

import java.time.LocalDate;

public final class DueLabelCalculator {

    public static final String OVERDUE = "OVERDUE";
    public static final String DUE_SOON = "DUE_SOON";
    public static final String NORMAL = "NORMAL";
    public static final String NONE = "NONE";

    private static volatile int dueSoonDays = 7;

    private DueLabelCalculator() {}

    public static void setDueSoonDays(int days) {
        dueSoonDays = days;
    }

    public static int getDueSoonDays() {
        return dueSoonDays;
    }

    public static String calculate(LocalDate nextDueDate, LocalDate today) {
        if (nextDueDate == null) {
            return NONE;
        }
        if (today.isAfter(nextDueDate)) {
            return OVERDUE;
        }
        if (!today.isBefore(nextDueDate.minusDays(dueSoonDays))) {
            return DUE_SOON;
        }
        return NORMAL;
    }

    public static String calculate(LocalDate nextDueDate) {
        return calculate(nextDueDate, LocalDate.now());
    }

    public static String calculateDueLabel(LocalDate nextDueDate, LocalDate today) {
        return calculate(nextDueDate, today);
    }

    public static String calculateDueLabel(LocalDate nextDueDate) {
        return calculate(nextDueDate, LocalDate.now());
    }
}
