package com.mt5dashboard.scenario;

public final class ConditionResult {
    public final boolean passed;
    public final String reason;

    private ConditionResult(boolean passed, String reason) {
        this.passed = passed;
        this.reason = reason;
    }

    public static ConditionResult pass(String reason) {
        return new ConditionResult(true, reason);
    }

    public static ConditionResult fail(String reason) {
        return new ConditionResult(false, reason);
    }
}
