package com.mt5dashboard.scenario;

import java.util.UUID;

import com.mt5dashboard.core.Timeframe;

public class ScenarioConfig {

    private final String id;
    private String name;
    private boolean enabled = true;

    private int validityMultiplier;
    private boolean mainTimeFrameEnabled;
    private Timeframe mainTimeframe;
    private boolean priceDifferenceEnabled;
    private int repeatIntervalMinutes;

    private boolean minimumTimeframeSyncEnabled;
    private int minimumTimeframeCount = 2;

    private boolean dualTimeframeEnabled;

    public ScenarioConfig(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.validityMultiplier = 10;
        this.repeatIntervalMinutes = 10;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getValidityMultiplier() {
        return validityMultiplier;
    }

    public void setValidityMultiplier(int validityMultiplier) {
        if (validityMultiplier <= 0) {
            throw new IllegalArgumentException("Signal Validity Multiplier باید مثبت باشد");
        }
        this.validityMultiplier = validityMultiplier;
    }

    public boolean isMainTimeFrameEnabled() {
        return mainTimeFrameEnabled;
    }

    public void setMainTimeFrameEnabled(boolean mainTimeFrameEnabled) {
        this.mainTimeFrameEnabled = mainTimeFrameEnabled;
    }

    public Timeframe getMainTimeframe() {
        return mainTimeframe;
    }

    public void setMainTimeframe(Timeframe mainTimeframe) {
        this.mainTimeframe = mainTimeframe;
    }

    public boolean isPriceDifferenceEnabled() {
        return priceDifferenceEnabled;
    }

    public void setPriceDifferenceEnabled(boolean priceDifferenceEnabled) {
        this.priceDifferenceEnabled = priceDifferenceEnabled;
    }

    public int getRepeatIntervalMinutes() {
        return repeatIntervalMinutes;
    }

    public void setRepeatIntervalMinutes(int repeatIntervalMinutes) {
        if (repeatIntervalMinutes <= 0) {
            throw new IllegalArgumentException("Signal Repeat Interval باید مثبت باشد");
        }
        this.repeatIntervalMinutes = repeatIntervalMinutes;
    }

    public boolean isMinimumTimeframeSyncEnabled() {
        return minimumTimeframeSyncEnabled;
    }

    public void setMinimumTimeframeSyncEnabled(boolean minimumTimeframeSyncEnabled) {
        this.minimumTimeframeSyncEnabled = minimumTimeframeSyncEnabled;
    }

    public int getMinimumTimeframeCount() {
        return minimumTimeframeCount;
    }

    public void setMinimumTimeframeCount(int minimumTimeframeCount) {
        if (minimumTimeframeCount < 2) {
            throw new IllegalArgumentException("Minimum Timeframe Sync باید حداقل ۲ باشد");
        }
        this.minimumTimeframeCount = minimumTimeframeCount;
    }

    public boolean isDualTimeframeEnabled() {
        return dualTimeframeEnabled;
    }

    public void setDualTimeframeEnabled(boolean dualTimeframeEnabled) {
        this.dualTimeframeEnabled = dualTimeframeEnabled;
    }
}
