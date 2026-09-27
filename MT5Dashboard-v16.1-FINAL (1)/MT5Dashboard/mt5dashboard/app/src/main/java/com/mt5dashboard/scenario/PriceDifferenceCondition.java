package com.mt5dashboard.scenario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mt5dashboard.core.Signal;

public class PriceDifferenceCondition implements Condition {

    @Override
    public boolean isApplicable(ScenarioConfig config) {
        return config.isPriceDifferenceEnabled();
    }

    @Override
    public ConditionResult evaluate(List<Signal> combination, ScenarioConfig config) {
        if (combination.size() < 2) {
            return ConditionResult.pass("فقط یک سیگنال در ترکیب است، مقایسه قیمت لازم نیست");
        }

        Set<Double> distinctPrices = new HashSet<>();
        for (Signal s : combination) {
            distinctPrices.add(s.getPrice());
        }

        if (distinctPrices.size() >= 2) {
            return ConditionResult.pass(
                    "حداقل دو مقدار قیمت متمایز (" + distinctPrices.size() + " مقدار) در ترکیب وجود دارد");
        }
        return ConditionResult.fail("تمام سیگنال‌های ترکیب دقیقاً یک قیمت مشترک دارند - هیچ عدد متفاوتی نیست");
    }
}
