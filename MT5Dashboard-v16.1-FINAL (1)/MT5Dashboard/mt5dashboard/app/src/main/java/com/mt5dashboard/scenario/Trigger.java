package com.mt5dashboard.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mt5dashboard.core.Direction;
import com.mt5dashboard.core.Signal;

public class Trigger {

    private final String id;
    private final String scenarioId;
    private final String symbol;
    private final Direction direction;
    private final List<Signal> combination;
    private final long createdAt;

    private volatile boolean silenced = false;
    private volatile long lastAlarmAt;

    public Trigger(String scenarioId, String symbol, Direction direction,
                    List<Signal> combination, long createdAt) {
        this.id = UUID.randomUUID().toString();
        this.scenarioId = scenarioId;
        this.symbol = symbol;
        this.direction = direction;
        this.combination = new ArrayList<>(combination);
        this.createdAt = createdAt;
        this.lastAlarmAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Direction getDirection() {
        return direction;
    }

    public List<Signal> getCombination() {
        return combination;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isSilenced() {
        return silenced;
    }

    public void silence() {
        this.silenced = true;
    }

    public long getLastAlarmAt() {
        return lastAlarmAt;
    }

    public void markAlarmFired(long now) {
        this.lastAlarmAt = now;
    }

    public java.util.Set<String> getSignalIdSet() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Signal s : combination) {
            ids.add(s.getId());
        }
        return ids;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Trigger{scenario=").append(scenarioId)
                .append(", symbol=").append(symbol)
                .append(", direction=").append(direction)
                .append(", combination=[");
        for (Signal s : combination) {
            sb.append(s.getTimeframe()).append("@").append(s.getPrice()).append(" ");
        }
        sb.append("], silenced=").append(silenced).append("}");
        return sb.toString();
    }
}
