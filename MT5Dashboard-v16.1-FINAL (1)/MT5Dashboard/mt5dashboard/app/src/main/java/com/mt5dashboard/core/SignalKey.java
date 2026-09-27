package com.mt5dashboard.core;

import java.util.Objects;

public final class SignalKey {

    private final String symbol;
    private final Timeframe timeframe;
    private final Direction direction;

    public SignalKey(String symbol, Timeframe timeframe, Direction direction) {
        if (symbol == null || timeframe == null || direction == null) {
            throw new IllegalArgumentException("symbol, timeframe و direction نمی‌توانند null باشند");
        }
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.direction = direction;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignalKey)) return false;
        SignalKey that = (SignalKey) o;
        return symbol.equals(that.symbol)
                && timeframe == that.timeframe
                && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, timeframe, direction);
    }

    @Override
    public String toString() {
        return symbol + "+" + timeframe + "+" + direction;
    }
}
