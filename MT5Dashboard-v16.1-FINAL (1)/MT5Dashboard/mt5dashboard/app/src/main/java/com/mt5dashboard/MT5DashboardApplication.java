package com.mt5dashboard;

import android.app.Application;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mt5dashboard.core.SignalStateManager;
import com.mt5dashboard.scenario.ScenarioEngineManager;
import com.mt5dashboard.ui.SettingsProvider;

/**
 * نقطه‌ی اشتراک وضعیت بین NotificationListenerService (که سیگنال دریافت می‌کند)
 * و UI (که سیگنال‌ها/سناریوها را نمایش می‌دهد). فقط یک نمونه از هرکدام در کل عمر اپ.
 *
 * توجه: این کلاس عمداً فقط یک Container ساده است و هیچ منطق کسب‌وکاری ندارد
 * (اصل استقلال لایه‌ها - بخش ۲۴ سند اصلی).
 *
 * اصلاحیه مهم (رفع باگ): فلگ "آیا AlarmNotifier ثبت شده یا نه" باید در سطح
 * Application (Singleton واقعی کل عمر Process) نگه داشته شود، نه در سطح
 * MonitoringForegroundService. چون اگر سیستم پروسه را بکشد و بعداً به‌خاطر
 * START_STICKY یک نمونه جدید از سرویس ساخته شود، فیلد داخل خود Service از نو
 * false می‌شود و یک AlarmNotifier تکراری به لیست globalAlarmListeners اضافه
 * می‌گردد؛ نتیجه‌اش نمایش/پخش چندباره هر آلارم است. با نگه‌داشتن این فلگ اینجا
 * (که فقط یک بار در طول عمر Process مقداردهی می‌شود)، این تکرار غیرممکن می‌شود.
 */
public class MT5DashboardApplication extends Application {

    private static MT5DashboardApplication instance;
    private final SignalStateManager signalStateManager = new SignalStateManager();
    private ScenarioEngineManager scenarioEngineManager;

    private final AtomicBoolean alarmNotifierRegistered = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // ScenarioEngineManager باید بعد از SignalStateManager ساخته شود چون در سازنده‌اش
        // به‌عنوان Listener روی آن ثبت‌نام می‌کند.
        scenarioEngineManager = new ScenarioEngineManager(
                signalStateManager, new SettingsProvider(this));
    }

    public static MT5DashboardApplication getInstance() {
        return instance;
    }

    public SignalStateManager getSignalStateManager() {
        return signalStateManager;
    }

    public ScenarioEngineManager getScenarioEngineManager() {
        return scenarioEngineManager;
    }

    /**
     * فقط اولین فراخوانی در کل عمر Process مقدار true برمی‌گرداند (ثبت مجاز است).
     * فراخوانی‌های بعدی - حتی اگر MonitoringForegroundService چندین بار توسط
     * سیستم Restart شود - false برمی‌گردانند تا AlarmNotifier دوباره ثبت نشود.
     */
    public boolean shouldRegisterAlarmNotifier() {
        return alarmNotifierRegistered.compareAndSet(false, true);
    }
}
