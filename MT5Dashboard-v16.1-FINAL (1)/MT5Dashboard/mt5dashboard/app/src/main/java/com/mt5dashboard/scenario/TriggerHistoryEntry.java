package com.mt5dashboard.scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mt5dashboard.core.Direction;
import com.mt5dashboard.core.Signal;

/**
 * Immutable snapshot of a Trigger created for Forward Test history.
 * The combination is retained as structured Signal data instead of only
 * preformatted text, so future filtering/search/analysis can use the
 * individual signal fields.
 */
public final class TriggerHistoryEntry {
    private final String scenarioId;
    private final String scenarioName;
    private final String symbol;
    private final Direction direction;
    private final List<Signal> combinationSnapshot;
    private final long createdAt;

    public TriggerHistoryEntry(String scenarioId,
                               String scenarioName,
                               String symbol,
                               Direction direction,
                               List<Signal> combinationSnapshot,
                               long createdAt) {
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.symbol = symbol;
        this.direction = direction;
        this.combinationSnapshot = Collections.unmodifiableList(
                new ArrayList<>(combinationSnapshot));
        this.createdAt = createdAt;
    }

    public String getScenarioId() { return scenarioId; }
    public String getScenarioName() { return scenarioName; }
    public String getSymbol() { return symbol; }
    public Direction getDirection() { return direction; }
    public List<Signal> getCombinationSnapshot() { return combinationSnapshot; }
    public long getCreatedAt() { return createdAt; }
}
