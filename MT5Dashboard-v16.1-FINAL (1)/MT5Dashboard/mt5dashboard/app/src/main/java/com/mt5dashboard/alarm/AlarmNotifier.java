package com.mt5dashboard.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.mt5dashboard.MainActivity;
import com.mt5dashboard.R;
import com.mt5dashboard.core.Signal;
import com.mt5dashboard.scenario.AlarmListener;
import com.mt5dashboard.scenario.ScenarioConfig;
import com.mt5dashboard.scenario.Trigger;

public class AlarmNotifier implements AlarmListener {

    public static final String CHANNEL_ID = "mt5_scenario_alarm_channel";
    public static final String ACTION_SILENCE = "com.mt5dashboard.ACTION_SILENCE_TRIGGER";
    public static final String EXTRA_SCENARIO_ID = "extra_scenario_id";
    public static final String EXTRA_SYMBOL = "extra_symbol";
    public static final String EXTRA_DIRECTION = "extra_direction";

    private final Context appContext;

    public AlarmNotifier(Context context) {
        this.appContext = context.getApplicationContext();
        createChannelIfNeeded();
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = appContext.getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        appContext.getString(R.string.alarm_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription(appContext.getString(R.string.alarm_channel_desc));
                manager.createNotificationChannel(channel);
            }
        }
    }

    private int notificationIdFor(String scenarioId, String symbol, String direction) {
        return (scenarioId + "|" + symbol + "|" + direction).hashCode();
    }

    private String buildCombinationText(Trigger trigger) {
        StringBuilder sb = new StringBuilder();
        for (Signal s : trigger.getCombination()) {
            if (sb.length() > 0) sb.append("  +  ");
            sb.append(s.getTimeframe()).append(" @ ").append(s.getPrice());
        }
        return trigger.getSymbol() + " " + trigger.getDirection() + "  |  " + sb;
    }

    private void postNotification(ScenarioConfig scenario, Trigger trigger) {
        int notificationId = notificationIdFor(scenario.getId(), trigger.getSymbol(), trigger.getDirection().name());

        Intent openAppIntent = new Intent(appContext, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(appContext, notificationId, openAppIntent, flags);

        Intent silenceIntent = new Intent(appContext, SilenceActionReceiver.class);
        silenceIntent.setAction(ACTION_SILENCE);
        silenceIntent.putExtra(EXTRA_SCENARIO_ID, scenario.getId());
        silenceIntent.putExtra(EXTRA_SYMBOL, trigger.getSymbol());
        silenceIntent.putExtra(EXTRA_DIRECTION, trigger.getDirection().name());
        PendingIntent silencePendingIntent = PendingIntent.getBroadcast(
                appContext, notificationId, silenceIntent, flags);

        String title = appContext.getString(R.string.alarm_notification_title, scenario.getName());
        String text = buildCombinationText(trigger);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_monitor_status)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .addAction(0, appContext.getString(R.string.alarm_action_silence), silencePendingIntent);

        NotificationManager manager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }

    @Override
    public void onNewTrigger(ScenarioConfig scenario, Trigger trigger) {
        postNotification(scenario, trigger);
    }

    @Override
    public void onRepeatAlarm(ScenarioConfig scenario, Trigger trigger) {
        postNotification(scenario, trigger);
    }

    @Override
    public void onTriggerExpired(ScenarioConfig scenario, Trigger trigger) {
        cancelNotification(scenario, trigger);
    }

    @Override
    public void onTriggerDismissed(ScenarioConfig scenario, Trigger trigger) {
        cancelNotification(scenario, trigger);
    }

    private void cancelNotification(ScenarioConfig scenario, Trigger trigger) {
        int notificationId = notificationIdFor(
                scenario.getId(), trigger.getSymbol(), trigger.getDirection().name());
        NotificationManager manager = (NotificationManager)
                appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationId);
        }
    }
}
