package com.mt5dashboard.scenario;

public interface AlarmListener {
    void onNewTrigger(ScenarioConfig scenario, Trigger trigger);
    void onRepeatAlarm(ScenarioConfig scenario, Trigger trigger);
    void onTriggerExpired(ScenarioConfig scenario, Trigger trigger);
    void onTriggerDismissed(ScenarioConfig scenario, Trigger trigger);
}
