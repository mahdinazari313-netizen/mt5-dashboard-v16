package com.mt5dashboard.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.mt5dashboard.MT5DashboardApplication;
import com.mt5dashboard.MainActivity;
import com.mt5dashboard.R;
import com.mt5dashboard.alarm.AlarmNotifier;

public class MonitoringForegroundService extends Service {

    public static final String CHANNEL_ID = "mt5_monitoring_status_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long SAFETY_CHECK_INTERVAL_MILLIS = 60_000L;

    // B-23 (v16): برای نمایش وضعیت زنده سرویس در دیالوگ مدیریت مجوزها.
    private static volatile boolean running;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable safetyCheckTick = new Runnable() {
        @Override
        public void run() {
            MT5DashboardApplication.getInstance().getScenarioEngineManager().runPeriodicSafetyCheck();
            handler.postDelayed(this, SAFETY_CHECK_INTERVAL_MILLIS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannelIfNeeded();
        startForeground(NOTIFICATION_ID, buildNotification(true));
        registerAlarmNotifierIfNeeded();
        running = true;
        handler.postDelayed(safetyCheckTick, SAFETY_CHECK_INTERVAL_MILLIS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification(true));
        registerAlarmNotifierIfNeeded();
        running = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(safetyCheckTick);
        running = false;
    }

    public static boolean isRunning() {
        return running;
    }

    private void registerAlarmNotifierIfNeeded() {
        if (!MT5DashboardApplication.getInstance().shouldRegisterAlarmNotifier()) {
            return;
        }
        MT5DashboardApplication.getInstance().getScenarioEngineManager()
                .addGlobalAlarmListener(new AlarmNotifier(this));
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.monitoring_notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription(getString(R.string.monitoring_notification_channel_desc));
                channel.setShowBadge(false);
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(boolean running) {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openAppIntent, pendingFlags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_monitor_status)
                .setContentTitle(getString(R.string.monitoring_notification_title))
                .setContentText(running
                        ? getString(R.string.monitoring_notification_text_running)
                        : getString(R.string.monitoring_notification_text_stopped))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
