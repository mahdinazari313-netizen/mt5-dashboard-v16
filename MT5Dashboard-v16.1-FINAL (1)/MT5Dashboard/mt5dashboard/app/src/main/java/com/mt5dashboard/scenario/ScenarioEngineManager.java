package com.mt5dashboard.scenario;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mt5dashboard.core.Signal;
import com.mt5dashboard.core.SignalKey;
import com.mt5dashboard.core.SignalStateManager;
import com.mt5dashboard.ui.SettingsProvider;

public class ScenarioEngineManager implements SignalStateManager.Listener {

    private final SignalStateManager signalStateManager;
    private final SettingsProvider settingsProvider;
    private final Map<String, ScenarioEngine> engines = new ConcurrentHashMap<>();

    private final List<AlarmListener> globalAlarmListeners = new CopyOnWriteArrayList<>();

    /**
     * B-26 (v16.1): تاریخچه سراسری Triggerهای ساخته‌شده، مستقل از سناریوهای
     * فعال یا حذف‌شده - برای مقایسه در Forward Test. خود تاریخچه فقط RAM است؛
     * Retention یک Setting پایدار است و از SettingsProvider خوانده می‌شود.
     */
    private final Deque<TriggerHistoryEntry> triggerHistory = new ConcurrentLinkedDeque<>();

    public ScenarioEngineManager(SignalStateManager signalStateManager,
                                 SettingsProvider settingsProvider) {
        this.signalStateManager = signalStateManager;
        this.settingsProvider = settingsProvider;
        this.signalStateManager.addListener(this);
        // ثبت‌کننده تاریخچه: فقط onNewTrigger برایش مهم است. با
        // addGlobalAlarmListener ثبت می‌شود تا خودکار به تمام Engineهای
        // فعلی و آینده وصل شود - دقیقاً همان مسیری که AlarmNotifier هم
        // طی می‌کند.
        addGlobalAlarmListener(new AlarmListener() {
            @Override
            public void onNewTrigger(ScenarioConfig scenario, Trigger trigger) {
                addToHistory(scenario, trigger);
            }

            @Override
            public void onRepeatAlarm(ScenarioConfig scenario, Trigger trigger) {
                // Repeat یک Trigger موجود است، نه یک Trigger جدید - رکورد
                // تکراری در تاریخچه ثبت نمی‌شود (طبق درخواست صریح B-26).
            }

            @Override
            public void onTriggerExpired(ScenarioConfig scenario, Trigger trigger) {
                // Expiration never removes a History entry. History is purged
                // only by retention in cleanOldHistory().
            }

            @Override
            public void onTriggerDismissed(ScenarioConfig scenario, Trigger trigger) {
                // Dismiss never removes a History entry. The event itself is
                // distinct so other listeners can distinguish the action.
            }
        });
    }

    public void addGlobalAlarmListener(AlarmListener listener) {
        globalAlarmListeners.add(listener);
        for (ScenarioEngine engine : engines.values()) {
            engine.addAlarmListener(listener);
        }
    }

    public ScenarioEngine createScenario(ScenarioConfig config) {
        ScenarioEngine engine = new ScenarioEngine(config, signalStateManager);
        for (AlarmListener listener : globalAlarmListeners) {
            engine.addAlarmListener(listener);
        }
        engines.put(config.getId(), engine);

        long now = System.currentTimeMillis();
        Set<SymbolDirectionKey> alreadyFed = new HashSet<>();
        for (Signal existing : signalStateManager.getAllSignals()) {
            SymbolDirectionKey key = new SymbolDirectionKey(existing.getSymbol(), existing.getDirection());
            if (!alreadyFed.add(key)) {
                continue;
            }

            Signal signalToFeed = existing;
            if (config.isDualTimeframeEnabled() && config.getMainTimeframe() != null) {
                Signal mainTfSignal = signalStateManager.getSignal(
                        new SignalKey(existing.getSymbol(), config.getMainTimeframe(), existing.getDirection()));
                if (mainTfSignal != null) {
                    signalToFeed = mainTfSignal;
                }
            }

            engine.onNewSignal(signalToFeed, now);
        }

        return engine;
    }

    public void closeScenario(String scenarioId) {
        ScenarioEngine engine = engines.remove(scenarioId);
        if (engine == null) return;

        for (Trigger trigger : engine.getActiveTriggers()) {
            for (AlarmListener listener : globalAlarmListeners) {
                listener.onTriggerExpired(engine.getConfig(), trigger);
            }
        }
    }

    public ScenarioEngine getEngine(String scenarioId) {
        return engines.get(scenarioId);
    }

    public List<ScenarioEngine> getAllEngines() {
        return new ArrayList<>(engines.values());
    }

    @Override
    public void onSignalStateChanged(Signal newOrUpdatedSignal) {
        long now = System.currentTimeMillis();
        for (ScenarioEngine engine : engines.values()) {
            engine.onNewSignal(newOrUpdatedSignal, now);
        }
    }

    public void runPeriodicSafetyCheck() {
        long now = System.currentTimeMillis();
        cleanOldHistory(now);
        for (ScenarioEngine engine : engines.values()) {
            engine.periodicSafetyCheck(now);
        }
    }

    // =====================================================================
    // B-26: Trigger History (سراسری، مستقل از سناریوهای فعال/حذف‌شده)
    // =====================================================================

    /** Snapshot از تاریخچه، نزولی بر اساس createdAt (جدیدترین اول - چون addFirst استفاده شده). */
    public List<TriggerHistoryEntry> getTriggerHistory() {
        return new ArrayList<>(triggerHistory);
    }

    public int getTriggerHistoryRetentionHours() {
        return settingsProvider.getTriggerHistoryRetentionHours();
    }

    private void addToHistory(ScenarioConfig scenario, Trigger trigger) {
        triggerHistory.addFirst(new TriggerHistoryEntry(
                scenario.getId(),
                scenario.getName(),
                trigger.getSymbol(),
                trigger.getDirection(),
                trigger.getCombination(),
                trigger.getCreatedAt()));
    }

    /** فقط بر اساس زمان پاک می‌کند؛ activeTriggers را اصلاً لمس نمی‌کند. */
    private void cleanOldHistory(long now) {
        long retentionMillis = getTriggerHistoryRetentionHours() * 3600_000L;
        while (true) {
            TriggerHistoryEntry last = triggerHistory.peekLast();
            if (last == null) break;
            if (now - last.getCreatedAt() >= retentionMillis) {
                triggerHistory.removeLast();
            } else {
                break;
            }
        }
    }
}
