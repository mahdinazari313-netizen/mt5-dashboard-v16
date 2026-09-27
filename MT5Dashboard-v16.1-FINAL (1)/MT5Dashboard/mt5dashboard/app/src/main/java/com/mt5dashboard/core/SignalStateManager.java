package com.mt5dashboard.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SignalStateManager {

    public interface Listener {
        void onSignalStateChanged(Signal newOrUpdatedSignal);
    }

    private final Map<SignalKey, Signal> signals = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Signal signal) {
        for (Listener l : listeners) {
            l.onSignalStateChanged(signal);
        }
    }

    public void upsertSignal(Signal newSignal) {
        signals.put(newSignal.getKey(), newSignal);
        notifyListeners(newSignal);
    }

    public void removeSignal(SignalKey key) {
        signals.remove(key);
    }

    public Signal getSignal(SignalKey key) {
        return signals.get(key);
    }

    public List<Signal> getAllSignals() {
        return new ArrayList<>(signals.values());
    }

    public List<Signal> getSignalsForSymbol(String symbol) {
        List<Signal> result = new ArrayList<>();
        for (Signal s : signals.values()) {
            if (s.getSymbol().equals(symbol)) {
                result.add(s);
            }
        }
        return result;
    }

    public List<Signal> getSignalsForSymbolAndDirection(String symbol, Direction direction) {
        List<Signal> result = new ArrayList<>();
        for (Signal s : signals.values()) {
            if (s.getSymbol().equals(symbol) && s.getDirection() == direction) {
                result.add(s);
            }
        }
        return result;
    }

    public List<String> getDistinctSymbols() {
        List<String> result = new ArrayList<>();
        for (Signal s : signals.values()) {
            if (!result.contains(s.getSymbol())) {
                result.add(s.getSymbol());
            }
        }
        return result;
    }

    public int size() {
        return signals.size();
    }
}
