package com.mt5dashboard.service;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.mt5dashboard.MT5DashboardApplication;
import com.mt5dashboard.core.MetaTraderSignalParser;
import com.mt5dashboard.core.RawParsedSignal;
import com.mt5dashboard.core.Signal;
import com.mt5dashboard.core.SignalParser;
import com.mt5dashboard.core.SignalValidator;

public class MT5NotificationListenerService extends NotificationListenerService {

    private static final String TAG = "MT5ListenerService";

    private static final String MT5_PACKAGE_NAME = "net.metaquotes.metatrader5";

    private final SignalParser parser = new MetaTraderSignalParser();
    private final SignalValidator validator = new SignalValidator();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getPackageName() == null) return;
        if (!MT5_PACKAGE_NAME.equals(sbn.getPackageName())) {
            return;
        }

        String text = extractNotificationText(sbn);
        if (text == null) return;

        RawParsedSignal raw = parser.tryParse(text);
        if (raw == null) {
            return;
        }

        long receivedAt = System.currentTimeMillis();
        SignalValidator.Result result = validator.validate(raw, receivedAt);

        if (!result.isValid()) {
            Log.w(TAG, "سیگنال رد شد: " + result.rejectionReason + " | متن خام: " + text);
            return;
        }

        Signal signal = result.signal;
        SignalStateManagerAccessor.get().upsertSignal(signal);
        Log.d(TAG, "سیگنال ثبت شد: " + signal);
    }

    private String extractNotificationText(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification == null) return null;
        Bundle extras = notification.extras;
        if (extras == null) return null;

        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (text != null) return text.toString();

        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (bigText != null) return bigText.toString();

        return null;
    }

    private static final class SignalStateManagerAccessor {
        static com.mt5dashboard.core.SignalStateManager get() {
            return MT5DashboardApplication.getInstance().getSignalStateManager();
        }
    }
}
