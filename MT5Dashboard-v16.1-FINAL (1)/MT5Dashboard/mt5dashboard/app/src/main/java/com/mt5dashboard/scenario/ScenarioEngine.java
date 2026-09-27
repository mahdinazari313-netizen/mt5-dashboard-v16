package com.mt5dashboard.scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mt5dashboard.core.Direction;
import com.mt5dashboard.core.Signal;
import com.mt5dashboard.core.SignalStateManager;
import com.mt5dashboard.core.WeekdayTimeUtil;

/**
 * یک نمونه مستقل موتور تصمیم‌گیری برای یک Scenario مشخص (اصل ۲۴ سند:
 * هیچ Engine ای از Engine دیگر خبر ندارد و هیچ State مشترکی بین آن‌ها
 * نیست).
 *
 * === تاریخچه نسخه‌ها (v11 → v15) ===
 * v11: نسخه مبنا، Dual دوحالته (true/false) - باگ‌دار.
 * v12: تصمیمات D/E/F اعمال شد (فقط Main TF در Dual، ترکیب دوعضوی
 *      Reference+Incoming، فقط یک Trigger فعال) + تصمیم B (receivedAt
 *      به‌جای now) + تصمیم ۳ (Sort نزولی createdAt).
 * v13: تصمیم Q1 اولیه (ناقص - هنوز Multiplier را از طریق
 *      getValidCombination به‌طور غیرمستقیم اعمال می‌کرد).
 * v14: رفع ریشه‌ای Q1 - در حالت Dual، getValidCombination دیگر
 *      فیلتر Multiplier ندارد.
 * v15 (این نسخه): رفع باگ Trigger Expire زودهنگام -
 *      (الف) onNewSignal دیگر با هر FAIL، Trigger فعال را حذف
 *            نمی‌کند (طبق تصمیم F: "Trigger قبلی دست‌نخورده می‌ماند").
 *      (ب) isTriggerStillValid در حالت Dual دیگر چک ID سیگنال
 *            Incoming را ندارد (چون با هر تکرار MT5 عوض می‌شود و
 *            باعث Expire اشتباه هر ۶۰ ثانیه می‌شد).
 *
 * دو مسیر کاملاً متفاوت ورودی دارد که عمداً هرگز نباید با هم قاطی شوند:
 *
 *   ۱) onNewSignal(signal) - رویداد "سیگنال خام جدید رسید".
 *      همیشه یک ارزیابی کامل انجام می‌دهد و اگر شروط پاس شود، همیشه یک
 *      Trigger جدید می‌سازد (حتی اگر ترکیب قبلی هم پاس بود) - این رفتار
 *      عمداً Silent قبلی را می‌شکند. اگر پاس نشود، Trigger فعال قبلی
 *      (اگر باشد) دست‌نخورده می‌ماند (تصمیم F).
 *
 *   ۲) periodicSafetyCheck(now) - فقط برای:
 *        الف) پاک‌سازی Triggerهایی که پنجره/اعتبارشان تمام شده
 *        ب) تکرار آلارم طبق Repeat Interval برای Trigger هنوز فعال و
 *           Silent-نشده
 *      این مسیر هرگز Trigger جدید نمی‌سازد و هرگز Silent را نمی‌شکند.
 *      این تنها مسیر مجاز برای حذف/Expire یک Trigger بر اثر گذر زمان
 *      (یا نبود رویداد جدید) است.
 *
 * === تاریخچه اصلاح Thread-safety ===
 * نسخه ۱ (باگ‌دار): HashMap ساده.
 * نسخه ۲ (ناقص): ConcurrentHashMap با compute() ولی fireXxx بیرون.
 * نسخه ۳ (v11): per-key lock برای سریال‌سازی کامل چرخه
 *   "بخوان -> تصمیم بگیر -> بنویس -> fireXxx" برای یک کلید مشخص.
 */
public class ScenarioEngine {

    private final ScenarioConfig config;
    private final SignalStateManager signalStateManager;
    private final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
    private final List<AlarmListener> alarmListeners = new CopyOnWriteArrayList<>();

    /** آخرین Trigger فعال برای هر Symbol+Direction. */
    private final Map<SymbolDirectionKey, Trigger> activeTriggers = new ConcurrentHashMap<>();

    /**
     * مرجع فعلی Dual in One Timeframe به‌ازای هر Symbol+Direction.
     * فقط وقتی config.isDualTimeframeEnabled() باشد پر می‌شود.
     */
    private final Map<SymbolDirectionKey, DualTimeframeReference> dualReferences = new ConcurrentHashMap<>();

    /** پنجره ثابت Dual: ۳۰ کندل. عدد ثابت، نه قابل‌تنظیم در UI. */
    private static final int DUAL_WINDOW_CANDLES = 30;

    /** قفل مخصوص هر Symbol+Direction برای سریال‌سازی کامل. */
    private final Map<SymbolDirectionKey, Object> keyLocks = new ConcurrentHashMap<>();

    private Object lockFor(SymbolDirectionKey key) {
        return keyLocks.computeIfAbsent(key, k -> new Object());
    }

    public ScenarioEngine(ScenarioConfig config, SignalStateManager signalStateManager) {
        this.config = config;
        this.signalStateManager = signalStateManager;
    }

    public ScenarioConfig getConfig() {
        return config;
    }

    public void addAlarmListener(AlarmListener listener) {
        alarmListeners.add(listener);
    }

    public void removeAlarmListener(AlarmListener listener) {
        alarmListeners.remove(listener);
    }

    /** فقط Triggerهای فعلاً فعال، مرتب‌شده نزولی بر اساس createdAt (تصمیم ۳). */
    public List<Trigger> getActiveTriggers() {
        List<Trigger> list = new ArrayList<>(activeTriggers.values());
        Collections.sort(list, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return list;
    }

    // =====================================================================
    // مسیر ۱: رویداد سیگنال خام جدید
    // =====================================================================

    /**
     * هر بار یک سیگنال جدید وارد SignalStateManager می‌شود صدا زده می‌شود.
     * فقط برای Symbol+Direction همان سیگنال ارزیابی انجام می‌دهد.
     *
     * تصمیم D: در سناریوی Dual، سیگنال‌های غیر از Main TF کاملاً نادیده
     * گرفته می‌شوند.
     */
    public void onNewSignal(Signal incomingSignal, long now) {
        if (!config.isEnabled()) return;

        // تصمیم D: در سناریوی Dual، فقط سیگنال Main TF بررسی می‌شود.
        if (config.isDualTimeframeEnabled()
                && config.isMainTimeFrameEnabled()
                && config.getMainTimeframe() != null
                && incomingSignal.getTimeframe() != config.getMainTimeframe()) {
            return;
        }

        SymbolDirectionKey key = new SymbolDirectionKey(
                incomingSignal.getSymbol(), incomingSignal.getDirection());

        synchronized (lockFor(key)) {
            List<Signal> validCombination = getValidCombination(key, now);

            if (validCombination.isEmpty()) {
                // در عمل با معماری فعلی این حالت رخ نمی‌دهد (سیگنال Incoming
                // خودش تازه ثبت شده و معتبر است)، اما به‌عنوان محافظت دفاعی
                // نگه داشته می‌شود.
                return;
            }

            ConditionResult result = conditionEvaluator.evaluateAll(validCombination, config);
            boolean passed = result.passed;

            // ترکیب Trigger پیش‌فرض = ترکیب معتبر فعلی
            List<Signal> triggerCombination = validCombination;

            // Dual in One Timeframe (تصمیم B و E):
            // اجرای Dual همیشه (مستقل از نتیجه سایر Conditions) تا Reference
            // به‌روزرسانی شود. نتیجه در passed نهایی AND می‌شود.
            if (config.isDualTimeframeEnabled()
                    && config.isMainTimeFrameEnabled()
                    && config.getMainTimeframe() != null) {

                DualCheckResult dualResult = checkAndUpdateDualTimeframe(key, incomingSignal);

                if (!dualResult.passed) {
                    passed = false;
                } else {
                    // تصمیم E: ترکیب دو عضوی {Reference قبلی، Incoming}
                    List<Signal> dualCombination = new ArrayList<>(2);
                    dualCombination.add(dualResult.referenceSignal);
                    dualCombination.add(incomingSignal);
                    triggerCombination = dualCombination;
                }
            }

            // === v15: تصمیم F ===
            // اگر شروط پاس نشوند، Trigger فعال قبلی (اگر باشد) دست‌نخورده
            // می‌ماند. حذف/Expire آن فقط از دو مسیر مجاز اتفاق می‌افتد:
            //   الف) periodicSafetyCheck (پنجره/Validity تمام شود)
            //   ب) جایگزینی با یک Trigger جدید *معتبر* (همین متد، پایین‌تر)
            if (!passed) {
                return;
            }

            // تصمیم F: فقط یک Trigger فعال.
            // Trigger جدید جایگزین می‌شود، Trigger قدیمی Expire اعلام می‌شود
            // (تا Notification آن Cancel شود).
            Trigger newTrigger = new Trigger(
                    config.getId(), key.getSymbol(), key.getDirection(),
                    triggerCombination, now);

            Trigger oldTrigger = activeTriggers.put(key, newTrigger);
            if (oldTrigger != null) {
                fireTriggerExpired(oldTrigger);
            }
            fireNewTrigger(newTrigger);
        }
    }

    // =====================================================================
    // مسیر ۲: بررسی دوره‌ای امن (فقط انقضا + تکرار آلارم)
    // =====================================================================

    /**
     * باید به‌صورت دوره‌ای (هر ۶۰ ثانیه از MonitoringForegroundService)
     * صدا زده شود. هرگز Trigger جدید نمی‌سازد. تنها مسیر مجاز برای
     * Expire کردن یک Trigger فعال بر اثر گذر زمان.
     */
    public void periodicSafetyCheck(long now) {
        if (!config.isEnabled()) return;

        for (SymbolDirectionKey key : activeTriggers.keySet()) {
            synchronized (lockFor(key)) {
                Trigger trigger = activeTriggers.get(key);
                if (trigger == null) {
                    continue;
                }

                List<Signal> currentValid = getValidCombination(key, now);

                if (!isTriggerStillValid(trigger, currentValid, now)) {
                    activeTriggers.remove(key);
                    fireTriggerExpired(trigger);
                    continue;
                }

                // ترکیب همان قبلی است و هنوز معتبر - فقط بررسی Repeat Interval
                if (!trigger.isSilenced()) {
                    long repeatIntervalMillis = config.getRepeatIntervalMinutes() * 60_000L;
                    if (now - trigger.getLastAlarmAt() >= repeatIntervalMillis) {
                        trigger.markAlarmFired(now);
                        fireRepeatAlarm(trigger);
                    }
                }
            }
        }
    }

    // =====================================================================
    // Silent
    // =====================================================================

    /** بخش ۱۴ سند: Silent فقط همین Trigger فعلی را ساکت می‌کند. */
    public void silence(String symbol, Direction direction) {
        SymbolDirectionKey key = new SymbolDirectionKey(symbol, direction);
        synchronized (lockFor(key)) {
            Trigger trigger = activeTriggers.get(key);
            if (trigger != null) {
                trigger.silence();
            }
        }
    }

    /**
     * B-28 (v16.1): حذف صریح یک Trigger فعال توسط کاربر، مستقل از Silent و
     * مستقل از حذف کل سناریو. Dismiss با triggerId کار می‌کند تا اگر پیش از
     * اجرای callback یک Trigger جدید جایگزین شده باشد، Trigger جدید به‌اشتباه
     * حذف نشود.
     *
     * Silent فقط Trigger فعلی را ساکت می‌کند. Dismiss Trigger را از
     * activeTriggers حذف می‌کند و رویداد مستقل onTriggerDismissed را ارسال
     * می‌کند. سناریو و Signalهای موجود در SignalStateManager دست‌نخورده
     * می‌مانند.
     */
    public void dismiss(String scenarioId, String symbol, Direction direction, String triggerId) {
        if (scenarioId == null || !config.getId().equals(scenarioId) || triggerId == null) {
            return;
        }

        SymbolDirectionKey key = new SymbolDirectionKey(symbol, direction);
        synchronized (lockFor(key)) {
            Trigger current = activeTriggers.get(key);
            if (current == null || !triggerId.equals(current.getId())) {
                // The UI callback refers to a stale Trigger. Never dismiss
                // a newer Trigger that has replaced it for this key.
                return;
            }

            activeTriggers.remove(key);
            fireTriggerDismissed(current);
        }
    }

    // =====================================================================
    // کمکی‌ها
    // =====================================================================

    /**
     * لیست سیگنال‌های معتبر برای این Symbol+Direction.
     *
     * === v14 (تصمیم Q1) ===
     * در حالت Dual، فیلتر Multiplier کلاً حذف می‌شود. Validity فقط با
     * پنجره ۳۰ کندل (در isTriggerStillValid) سنجیده می‌شود؛ اگر فیلتر
     * Multiplier اینجا باقی بماند، سیگنال Incoming که از Multiplier
     * منقضی شده (ولی هنوز در پنجره ۳۰ کندل است) به‌اشتباه از
     * currentValid حذف و Trigger زودتر از موعد Expire می‌شد.
     *
     * در غیر Dual: فیلتر Multiplier طبق رفتار همیشگی اعمال می‌شود.
     */
    private List<Signal> getValidCombination(SymbolDirectionKey key, long now) {
        List<Signal> all = signalStateManager.getSignalsForSymbolAndDirection(
                key.getSymbol(), key.getDirection());
        List<Signal> valid = new ArrayList<>();
        for (Signal s : all) {
            // تصمیم D: در حالت Dual فقط Main TF
            if (config.isDualTimeframeEnabled()
                    && config.isMainTimeFrameEnabled()
                    && config.getMainTimeframe() != null
                    && s.getTimeframe() != config.getMainTimeframe()) {
                continue;
            }

            // تصمیم Q1: در Dual، Multiplier نادیده گرفته می‌شود.
            if (config.isDualTimeframeEnabled()) {
                valid.add(s);
            } else {
                if (s.isValidForScenario(now, config.getValidityMultiplier())) {
                    valid.add(s);
                }
            }
        }
        return valid;
    }

    /**
     * بررسی و به‌روزرسانی مرجع Dual. فقط با سیگنال Main TF فراخوانی
     * می‌شود (تضمین‌شده توسط تصمیم D).
     *
     * منطق:
     *   - اولین سیگنال Main TF → Reference ثبت، بدون Trigger.
     *   - قیمت یکسان با Reference → Reference تغییر نمی‌کند، رویداد
     *     جدید محسوب نمی‌شود (نتیجه fail، اما در onNewSignal سطح
     *     بالاتر این یعنی صرفاً "بدون اقدام"، نه حذف Trigger فعال -
     *     تصمیم C + F).
     *   - قیمت متفاوت → فاصله مؤثر (بدون شنبه/یکشنبه) با مرجع مقایسه
     *     می‌شود. صرف‌نظر از نتیجه، Reference به‌روز می‌شود.
     *
     * تصمیم B: زمان Reference = receivedAt سیگنال (نه now).
     * تصمیم E: مقدار برگشتی شامل Reference قبلی است.
     */
    private DualCheckResult checkAndUpdateDualTimeframe(SymbolDirectionKey key, Signal incomingSignal) {

        DualTimeframeReference previous = dualReferences.get(key);

        // اولین سیگنال روی Main TF → Reference ثبت، بدون Trigger
        if (previous == null) {
            dualReferences.put(key, new DualTimeframeReference(incomingSignal));
            return DualCheckResult.fail(null);
        }

        // همان قیمت → Reference تغییر نمی‌کند
        if (Double.compare(previous.signal.getPrice(), incomingSignal.getPrice()) == 0) {
            return DualCheckResult.fail(null);
        }

        // قیمت متفاوت → مقایسه با پنجره ۳۰ کندل
        long windowMillis = config.getMainTimeframe().getBaseMinutes()
                * 60_000L * DUAL_WINDOW_CANDLES;
        long effectiveElapsed = WeekdayTimeUtil.weekdayMillisBetween(
                previous.signal.getReceivedAt(), incomingSignal.getReceivedAt());

        Signal referenceSignal = previous.signal;

        // Reference به‌روز می‌شود - صرف‌نظر از نتیجه
        dualReferences.put(key, new DualTimeframeReference(incomingSignal));

        // طبق متن دقیق: "کمتر از ۳۰ کندل" یعنی نامساوی اکید (<)
        if (effectiveElapsed < windowMillis) {
            return DualCheckResult.pass(referenceSignal);
        }
        return DualCheckResult.fail(null);
    }

    /**
     * بررسی می‌کند آیا یک Trigger فعال هنوز معتبر است. تنها محل فراخوانی:
     * periodicSafetyCheck (تنها مسیر مجاز Expire).
     *
     * === تصمیم Q1 + v15 ===
     * در حالت Dual، فقط پنجره ۳۰ کندل از لحظه Reference (اولین عضو
     * ترکیب) حاکم است. Multiplier کاملاً نادیده گرفته می‌شود.
     *
     * نکته حیاتی v15: در Dual، چک ID سیگنال Incoming حذف شده. چون هر
     * تکرار MT5 (حتی با همان قیمت - تصمیم C) سیگنال Incoming را در
     * state جایگزین می‌کند و ID عوض می‌شود. اگر این چک باقی می‌ماند،
     * Trigger هر ۶۰ ثانیه (اولین Safety Check بعد از تکرار) به‌اشتباه
     * Expire می‌شد - مستقیماً ناقض تصمیم F ("Trigger قبلی دست‌نخورده
     * می‌ماند تا پنجره تمام شود یا جایگزین شود").
     *
     * در غیر Dual: همه سیگنال‌های ترکیب باید طبق Multiplier معتبر
     * باشند و دقیقاً همان ID ها باید هنوز در state باشند.
     */
    private boolean isTriggerStillValid(Trigger trigger, List<Signal> currentValid, long now) {
        List<Signal> combo = trigger.getCombination();
        if (combo.isEmpty()) return false;

        if (config.isDualTimeframeEnabled()) {
            // تصمیم Q1: فقط پنجره ۳۰ کندل از Reference (اولین عضو ترکیب).
            // بدون چک ID سیگنال Incoming.
            Signal reference = combo.get(0);
            long windowMillis = config.getMainTimeframe().getBaseMinutes()
                    * 60_000L * DUAL_WINDOW_CANDLES;
            long effectiveElapsed = WeekdayTimeUtil.weekdayMillisBetween(
                    reference.getReceivedAt(), now);
            return effectiveElapsed < windowMillis;

        } else {
            // غیر Dual: همه سیگنال‌ها باید طبق Multiplier معتبر باشند.
            for (Signal s : combo) {
                if (!s.isValidForScenario(now, config.getValidityMultiplier())) {
                    return false;
                }
            }
            // چک ID: اگر سیگنالی جایگزین شده باشد، ترکیب قبلی دیگر
            // «جاری» نیست - Trigger Expire می‌شود.
            java.util.Set<String> currentIds = new java.util.HashSet<>();
            for (Signal s : currentValid) {
                currentIds.add(s.getId());
            }
            return currentIds.equals(trigger.getSignalIdSet());
        }
    }

    private void fireNewTrigger(Trigger trigger) {
        for (AlarmListener listener : alarmListeners) {
            listener.onNewTrigger(config, trigger);
        }
    }

    private void fireRepeatAlarm(Trigger trigger) {
        for (AlarmListener listener : alarmListeners) {
            listener.onRepeatAlarm(config, trigger);
        }
    }

    private void fireTriggerExpired(Trigger trigger) {
        for (AlarmListener listener : alarmListeners) {
            listener.onTriggerExpired(config, trigger);
        }
    }

    private void fireTriggerDismissed(Trigger trigger) {
        for (AlarmListener listener : alarmListeners) {
            listener.onTriggerDismissed(config, trigger);
        }
    }

    // =====================================================================
    // کلاس داخلی: DualCheckResult
    // =====================================================================

    /**
     * نتیجه بررسی Dual.
     *
     * فیلد referenceSignal برای ساخت ترکیب دو عضوی {Reference, Incoming}
     * در onNewSignal لازم است (تصمیم E).
     */
    private static final class DualCheckResult {
        final boolean passed;
        final Signal referenceSignal;

        private DualCheckResult(boolean passed, Signal referenceSignal) {
            this.passed = passed;
            this.referenceSignal = referenceSignal;
        }

        static DualCheckResult pass(Signal referenceSignal) {
            return new DualCheckResult(true, referenceSignal);
        }

        static DualCheckResult fail(Signal referenceSignal) {
            return new DualCheckResult(false, referenceSignal);
        }
    }
}
