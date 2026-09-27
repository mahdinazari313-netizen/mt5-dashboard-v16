package com.mt5dashboard.core;

public interface SignalParser {
    RawParsedSignal tryParse(String notificationText);
}
