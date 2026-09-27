package com.mt5dashboard.scenario;

import java.util.List;

import com.mt5dashboard.core.Signal;

public interface Condition {
    boolean isApplicable(ScenarioConfig config);
    ConditionResult evaluate(List<Signal> combination, ScenarioConfig config);
}
