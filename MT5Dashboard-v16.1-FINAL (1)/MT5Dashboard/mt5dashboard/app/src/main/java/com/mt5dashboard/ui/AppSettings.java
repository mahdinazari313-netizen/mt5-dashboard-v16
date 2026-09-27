package com.mt5dashboard.ui;

import android.content.Context;

/**
 * Backward-compatible alias for the shared settings provider.
 * New code should use SettingsProvider directly.
 */
@Deprecated
public class AppSettings extends SettingsProvider {
    public AppSettings(Context context) {
        super(context);
    }
}
