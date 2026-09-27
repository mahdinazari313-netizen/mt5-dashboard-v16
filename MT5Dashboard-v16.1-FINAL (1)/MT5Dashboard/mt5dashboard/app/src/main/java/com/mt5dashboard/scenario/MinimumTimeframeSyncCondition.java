package com.mt5dashboard.scenario;

import java.util.List;

import com.mt5dashboard.core.Signal;

public class MinimumTimeframeSyncCondition implements Condition {

    @Override
    public boolean isApplicable(ScenarioConfig config) {
        return config.isMinimumTimeframeSyncEnabled();
    }

    @Override
    public ConditionResult evaluate(List<Signal> combination, ScenarioConfig config) {
        int required = config.getMinimumTimeframeCount();
        if (combination.size() >= required) {
            return ConditionResult.pass(
                    "تعداد تایم‌فریم‌های هم‌زمان (" + combination.size() + ") به حداقل لازم (" + required + ") رسیده است");
        }
        return ConditionResult.fail(
                "تعداد تایم‌فریم‌های هم‌زمان (" + combination.size() + ") کمتر از حداقل لازم (" + required + ") است");
    }
}
