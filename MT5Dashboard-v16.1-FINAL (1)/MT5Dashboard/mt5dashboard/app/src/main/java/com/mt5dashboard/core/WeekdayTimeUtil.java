package com.mt5dashboard.core;

import java.util.Calendar;

public final class WeekdayTimeUtil {

    private WeekdayTimeUtil() {
    }

    public static long weekdayMillisBetween(long startMillis, long endMillis) {
        if (endMillis <= startMillis) {
            return 0L;
        }

        long total = endMillis - startMillis;
        long weekendMillis = 0L;

        Calendar dayCursor = Calendar.getInstance();
        dayCursor.setTimeInMillis(startMillis);
        dayCursor.set(Calendar.HOUR_OF_DAY, 0);
        dayCursor.set(Calendar.MINUTE, 0);
        dayCursor.set(Calendar.SECOND, 0);
        dayCursor.set(Calendar.MILLISECOND, 0);

        long dayStart = dayCursor.getTimeInMillis();

        while (dayStart < endMillis) {
            dayCursor.setTimeInMillis(dayStart);
            int dayOfWeek = dayCursor.get(Calendar.DAY_OF_WEEK);

            dayCursor.add(Calendar.DAY_OF_MONTH, 1);
            long dayEnd = dayCursor.getTimeInMillis();

            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                long overlapStart = Math.max(dayStart, startMillis);
                long overlapEnd = Math.min(dayEnd, endMillis);
                if (overlapEnd > overlapStart) {
                    weekendMillis += (overlapEnd - overlapStart);
                }
            }

            dayStart = dayEnd;
        }

        return total - weekendMillis;
    }
}
