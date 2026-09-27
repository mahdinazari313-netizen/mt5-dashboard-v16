package com.mt5dashboard.scenario;

import java.util.Arrays;
import java.util.List;

import com.mt5dashboard.core.Signal;

public class ConditionEvaluator {

    private final List<Condition> conditions = Arrays.asList(
            new MainTimeFrameCondition(),
            new PriceDifferenceCondition(),
            new MinimumTimeframeSyncCondition()
    );

    public ConditionResult evaluateAll(List<Signal> combination, ScenarioConfig config) {
        for (Condition condition : conditions) {
            if (!condition.isApplicable(config)) {
                continue;
            }
            ConditionResult result = condition.evaluate(combination, config);
            if (!result.passed) {
                return result;
            }
        }
        return ConditionResult.pass("تمام شروط فعال پاس شدند");
    }
}
