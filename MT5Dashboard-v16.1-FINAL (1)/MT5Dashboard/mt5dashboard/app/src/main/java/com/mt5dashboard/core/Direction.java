package com.mt5dashboard.core;

public enum Direction {
    BUY,
    SELL;

    public static Direction fromRawText(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toUpperCase();
        if (normalized.contains("BUY")) return BUY;
        if (normalized.contains("SELL")) return SELL;
        return null;
    }
}
