package com.mt5dashboard.ui;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Central persistent application settings provider.
 * Display settings and Trigger History retention are user settings and
 * therefore survive an application restart. Scenario/trigger state remains
 * runtime-only elsewhere in the application.
 */
public class SettingsProvider {

    private static final String PREFS_NAME = "mt5dashboard_settings";

    private static final String KEY_DISPLAY_DURATION_MINUTES = "display_duration_minutes";
    private static final int DEFAULT_DISPLAY_DURATION_MINUTES = 60;

    private static final String KEY_TRIGGER_HISTORY_RETENTION_HOURS =
            "trigger_history_retention_hours";
    private static final int DEFAULT_TRIGGER_HISTORY_RETENTION_HOURS = 24;

    private final SharedPreferences prefs;

    public SettingsProvider(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getDisplayDurationMinutes() {
        return prefs.getInt(KEY_DISPLAY_DURATION_MINUTES, DEFAULT_DISPLAY_DURATION_MINUTES);
    }

    public void setDisplayDurationMinutes(int minutes) {
        if (minutes < 1) {
            throw new IllegalArgumentException("Display Duration باید حداقل ۱ دقیقه باشد");
        }
        prefs.edit().putInt(KEY_DISPLAY_DURATION_MINUTES, minutes).apply();
    }

    public int getTriggerHistoryRetentionHours() {
        return prefs.getInt(
                KEY_TRIGGER_HISTORY_RETENTION_HOURS,
                DEFAULT_TRIGGER_HISTORY_RETENTION_HOURS);
    }

    public void setTriggerHistoryRetentionHours(int hours) {
        if (hours < 1) {
            throw new IllegalArgumentException(
                    "Trigger History Retention باید حداقل ۱ ساعت باشد");
        }
        prefs.edit().putInt(KEY_TRIGGER_HISTORY_RETENTION_HOURS, hours).apply();
    }
}
