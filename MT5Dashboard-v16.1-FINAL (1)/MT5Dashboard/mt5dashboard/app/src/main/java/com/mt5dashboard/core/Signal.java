package com.mt5dashboard.core;

import java.util.Objects;
import java.util.UUID;

public final class Signal {

    private final String id;
    private final String symbol;
    private final Timeframe timeframe;
    private final Direction direction;
    private final double price;
    private final long receivedAt;

    public Signal(String symbol, Timeframe timeframe, Direction direction, double price, long receivedAt) {
        this.id = UUID.randomUUID().toString();
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.direction = direction;
        this.price = price;
        this.receivedAt = receivedAt;
    }

    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public Timeframe getTimeframe() {
        return timeframe;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getPrice() {
        return price;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public boolean isValidForScenario(long nowMillis, int validityMultiplier) {
        long validUntil = receivedAt + timeframe.getBaseMinutes() * 60_000L * validityMultiplier;
        return nowMillis < validUntil;
    }

    public long getValidUntilForScenario(int validityMultiplier) {
        return receivedAt + timeframe.getBaseMinutes() * 60_000L * validityMultiplier;
    }

    public SignalKey getKey() {
        return new SignalKey(symbol, timeframe, direction);
    }

    public boolean hasSamePrice(Signal other) {
        if (other == null) return false;
        return Double.compare(this.price, other.price) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Signal)) return false;
        Signal signal = (Signal) o;
        return id.equals(signal.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Signal{" +
                "symbol='" + symbol + '\'' +
                ", timeframe=" + timeframe +
                ", direction=" + direction +
                ", price=" + price +
                ", receivedAt=" + receivedAt +
                '}';
    }
}
