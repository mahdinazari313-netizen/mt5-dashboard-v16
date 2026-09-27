package com.mt5dashboard;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mt5dashboard.service.MonitoringForegroundService;
import com.mt5dashboard.ui.ActiveSignalsFragment;
import com.mt5dashboard.ui.SettingsProvider;
import com.mt5dashboard.ui.ScenarioBoardsFragment;
import com.mt5dashboard.ui.TriggerHistoryFragment;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 501;
    private static final String PREF_MONITORING_ENABLED = "monitoring_enabled";

    private SettingsProvider settingsProvider;
    private MenuItem toggleMonitoringMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsProvider = new SettingsProvider(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            if (item.getItemId() == R.id.nav_trigger_history) {
                fragment = new TriggerHistoryFragment();
            } else if (item.getItemId() == R.id.nav_scenarios) {
                fragment = new ScenarioBoardsFragment();
            } else {
                fragment = new ActiveSignalsFragment();
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_active_signals);
        }

        ensurePostNotificationPermission();
        startMonitoringService();
    }

    // ---------------- منوی سه‌نقطه ----------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        toggleMonitoringMenuItem = menu.findItem(R.id.action_toggle_monitoring);
        updateToggleMonitoringTitle();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showDisplayDurationDialog();
            return true;
        } else if (id == R.id.action_permissions) {
            showPermissionsDialog();
            return true;
        } else if (id == R.id.action_trigger_history_retention) {
            showTriggerHistoryRetentionDialog();
            return true;
        } else if (id == R.id.action_toggle_monitoring) {
            toggleMonitoring();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------------- تنظیمات نمایش (Display Duration - فقط صفحه اول) ----------------

    private void showDisplayDurationDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(settingsProvider.getDisplayDurationMinutes()));

        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_display_duration_label)
                .setView(input)
                .setPositiveButton(R.string.settings_save, (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            int minutes = Integer.parseInt(text);
                            if (minutes > 0) {
                                settingsProvider.setDisplayDurationMinutes(minutes);
                            }
                        } catch (NumberFormatException ignored) {
                            // مقدار نامعتبر، تنظیمات قبلی حفظ می‌شود
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------------- تاریخچه Trigger (B-26) ----------------

    private void showTriggerHistoryRetentionDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(settingsProvider.getTriggerHistoryRetentionHours()));

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_trigger_history_retention)
                .setMessage(R.string.trigger_history_retention_description)
                .setView(input)
                .setPositiveButton(R.string.settings_save, (dialog, which) -> {
                    try {
                        int hours = Integer.parseInt(input.getText().toString().trim());
                        if (hours < 1) throw new NumberFormatException();
                        settingsProvider.setTriggerHistoryRetentionHours(hours);
                    } catch (NumberFormatException ignored) {
                        // مقدار نامعتبر، تنظیمات قبلی حفظ می‌شود
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------------- مدیریت مجوزها (B-23: ارتقا با وضعیت زنده) ----------------

    /**
     * نسخه ارتقایافته (B-23): به‌جای یک پیام ثابت، وضعیت زنده هر مجوز را
     * نشان می‌دهد و فقط برای مجوزهای غیرفعال دکمه رفع نمایش می‌دهد.
     */
    private void showPermissionsDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        addPermissionRow(root, R.string.permission_notification_access_title,
                notificationListenerEnabled() ? R.string.status_enabled : R.string.status_disabled,
                !notificationListenerEnabled(),
                v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addPermissionRow(root, R.string.permission_post_notifications,
                    hasPostNotificationPermission() ? R.string.status_enabled : R.string.status_disabled,
                    !hasPostNotificationPermission(), v -> ensurePostNotificationPermission());
        } else {
            addPermissionRow(root, R.string.permission_post_notifications,
                    R.string.status_not_required, false, null);
        }

        boolean batteryExempt = batteryOptimizationExempt();
        addPermissionRow(root, R.string.permission_battery_title,
                batteryExempt ? R.string.status_enabled : R.string.status_disabled,
                !batteryExempt, v -> requestIgnoreBatteryOptimizations());

        addPermissionRow(root, R.string.permission_monitoring_service,
                MonitoringForegroundService.isRunning() ? R.string.status_running : R.string.status_stopped,
                false, null);

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_permissions)
                .setView(root)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void addPermissionRow(LinearLayout parent, int titleRes, int stateRes,
                                  boolean showButton, android.view.View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView text = new TextView(this);
        text.setText(getString(titleRes) + " — " + getString(stateRes));
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text);

        if (showButton && listener != null) {
            Button button = new Button(this);
            button.setText(titleRes == R.string.permission_post_notifications
                    ? R.string.permission_request
                    : titleRes == R.string.permission_battery_title
                    ? R.string.permission_request_exemption
                    : R.string.permission_open_settings);
            button.setOnClickListener(listener);
            row.addView(button);
        }
        parent.addView(row);
    }

    private boolean notificationListenerEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(getPackageName());
    }

    private boolean hasPostNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean batteryOptimizationExempt() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm == null || pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private int dp(int px) {
        return Math.round(px * getResources().getDisplayMetrics().density);
    }

    private void requestIgnoreBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "این برنامه از قبل از بهینه‌سازی باتری معاف است", Toast.LENGTH_SHORT).show();
        }
    }

    private void ensurePostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_POST_NOTIFICATIONS);
            }
        }
    }

    // ---------------- شروع/توقف سرویس مانیتورینگ ----------------

    private void startMonitoringService() {
        boolean enabled = getPreferences(MODE_PRIVATE).getBoolean(PREF_MONITORING_ENABLED, true);
        if (!enabled) return;
        Intent serviceIntent = new Intent(this, MonitoringForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void toggleMonitoring() {
        boolean currentlyEnabled = getPreferences(MODE_PRIVATE).getBoolean(PREF_MONITORING_ENABLED, true);
        boolean newState = !currentlyEnabled;
        getPreferences(MODE_PRIVATE).edit().putBoolean(PREF_MONITORING_ENABLED, newState).apply();

        if (newState) {
            startMonitoringService();
        } else {
            stopService(new Intent(this, MonitoringForegroundService.class));
        }
        updateToggleMonitoringTitle();
    }

    private void updateToggleMonitoringTitle() {
        if (toggleMonitoringMenuItem == null) return;
        boolean enabled = getPreferences(MODE_PRIVATE).getBoolean(PREF_MONITORING_ENABLED, true);
        toggleMonitoringMenuItem.setTitle(enabled
                ? R.string.menu_stop_monitoring
                : R.string.menu_start_monitoring);
    }

    // ---------------- درباره برنامه ----------------

    private void showAboutDialog() {
        String versionName;
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "-";
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_about)
                .setMessage(getString(R.string.about_description)
                        + "\n\n" + getString(R.string.about_version_label) + ": " + versionName)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
