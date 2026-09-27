package com.mt5dashboard.core;

public final class RawParsedSignal {
    public final String symbol;
    public final String timeframe;
    public final String direction;
    public final String priceText;

    public RawParsedSignal(String symbol, String timeframe, String direction, String priceText) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.direction = direction;
        this.priceText = priceText;
    }

    @Override
    public String toString() {
        return "RawParsedSignal{symbol='" + symbol + "', timeframe='" + timeframe +
                "', direction='" + direction + "', priceText='" + priceText + "'}";
    }
}
