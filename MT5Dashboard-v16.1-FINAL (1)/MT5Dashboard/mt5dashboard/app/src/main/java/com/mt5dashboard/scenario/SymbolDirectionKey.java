package com.mt5dashboard.scenario;

import java.util.Objects;

import com.mt5dashboard.core.Direction;

public final class SymbolDirectionKey {
    private final String symbol;
    private final Direction direction;

    public SymbolDirectionKey(String symbol, Direction direction) {
        this.symbol = symbol;
        this.direction = direction;
    }

    public String getSymbol() {
        return symbol;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymbolDirectionKey)) return false;
        SymbolDirectionKey that = (SymbolDirectionKey) o;
        return symbol.equals(that.symbol) && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, direction);
    }

    @Override
    public String toString() {
        return symbol + "+" + direction;
    }
}
