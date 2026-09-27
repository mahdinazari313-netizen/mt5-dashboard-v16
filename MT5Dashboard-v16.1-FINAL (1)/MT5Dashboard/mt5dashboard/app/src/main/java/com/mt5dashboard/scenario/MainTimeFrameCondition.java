package com.mt5dashboard.scenario;

import java.util.List;

import com.mt5dashboard.core.Signal;

public class MainTimeFrameCondition implements Condition {

    @Override
    public boolean isApplicable(ScenarioConfig config) {
        return config.isMainTimeFrameEnabled();
    }

    @Override
    public ConditionResult evaluate(List<Signal> combination, ScenarioConfig config) {
        for (Signal s : combination) {
            if (s.getTimeframe() == config.getMainTimeframe()) {
                return ConditionResult.pass("Main Time Frame (" + config.getMainTimeframe() + ") در ترکیب موجود است");
            }
        }
        return ConditionResult.fail("Main Time Frame (" + config.getMainTimeframe() + ") در ترکیب موجود نیست");
    }
}
