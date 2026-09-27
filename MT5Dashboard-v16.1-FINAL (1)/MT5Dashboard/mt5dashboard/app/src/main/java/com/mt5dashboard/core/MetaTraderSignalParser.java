package com.mt5dashboard.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetaTraderSignalParser implements SignalParser {

    private static final Pattern PATTERN = Pattern.compile(
            "^\\(([^(),\\s]+),\\s*([A-Za-z][A-Za-z0-9]*)\\)\\s+(Buy|Sell)\\s+Signal-\\(([A-Za-z][A-Za-z0-9]*)-(-?\\d+(?:\\.\\d+)?)\\)-\\[[^\\]]+\\]\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public RawParsedSignal tryParse(String notificationText) {
        if (notificationText == null) return null;
        Matcher matcher = PATTERN.matcher(notificationText.trim());
        if (!matcher.matches()) return null;

        String symbol = matcher.group(1);
        String timeframe = matcher.group(2);
        String direction = matcher.group(3);
        String embeddedTimeframe = matcher.group(4);

        if (!timeframe.equalsIgnoreCase(embeddedTimeframe)) return null;

        String priceText = matcher.group(5);
        return new RawParsedSignal(symbol, timeframe, direction, priceText);
    }
}
