package com.mt5dashboard.core;

public class SignalValidator {

    public static final class Result {
        public final Signal signal;
        public final String rejectionReason;

        private Result(Signal signal, String rejectionReason) {
            this.signal = signal;
            this.rejectionReason = rejectionReason;
        }

        public boolean isValid() {
            return signal != null;
        }

        static Result ok(Signal signal) {
            return new Result(signal, null);
        }

        static Result rejected(String reason) {
            return new Result(null, reason);
        }
    }

    public Result validate(RawParsedSignal raw, long receivedAtMillis) {
        if (raw == null) {
            return Result.rejected("ورودی خام null است");
        }

        if (raw.symbol == null || raw.symbol.trim().isEmpty()) {
            return Result.rejected("Symbol خالی است");
        }

        Timeframe timeframe = Timeframe.fromString(raw.timeframe);
        if (timeframe == null) {
            return Result.rejected("Timeframe نامعتبر: " + raw.timeframe);
        }

        Direction direction = Direction.fromRawText(raw.direction);
        if (direction == null) {
            return Result.rejected("Direction نامعتبر: " + raw.direction);
        }

        double price;
        try {
            price = Double.parseDouble(raw.priceText);
        } catch (NumberFormatException | NullPointerException e) {
            return Result.rejected("Price نامعتبر: " + raw.priceText);
        }

        Signal signal = new Signal(raw.symbol.trim(), timeframe, direction, price, receivedAtMillis);
        return Result.ok(signal);
    }
}
