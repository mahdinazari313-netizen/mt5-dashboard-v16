package com.mt5dashboard.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mt5dashboard.MT5DashboardApplication;
import com.mt5dashboard.R;
import com.mt5dashboard.core.Direction;
import com.mt5dashboard.core.Signal;
import com.mt5dashboard.core.SignalStateManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * صفحه اول (بخش ۱۸ سند): نمایش خام و زنده سیگنال‌های فعال، کاملاً
 * مستقل از سناریوها.
 *
 * دو منبع به‌روزرسانی دارد:
 *   ۱) Event-driven: هر بار SignalStateManager سیگنال جدیدی دریافت کند.
 *   ۲) Time-driven: هر ۳۰ ثانیه تیک برای بررسی انقضای Display Duration.
 *
 * === تغییرات v12 ===
 * تصمیم ۲: Sort سیگنال‌ها قبل از نمایش:
 *   - TF از بزرگ به کوچک (D1 → M1)
 *   - در همان TF: BUY قبل از SELL
 *
 * === تغییرات v13 ===
 * تصمیم Q2: Sort کارت‌های Symbol بر اساس تازه‌ترین سیگنال داخل هر کارت
 * (نزولی) — نمادی که آخرین سیگنال را گرفته، بالاتر.
 */
public class ActiveSignalsFragment extends Fragment implements SignalStateManager.Listener {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private SymbolCardAdapter adapter;
    private AppSettings appSettings;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTick = new Runnable() {
        @Override
        public void run() {
            refreshList();
            handler.postDelayed(this, 30_000L);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_active_signals, container, false);
        recyclerView = root.findViewById(R.id.recyclerView);
        emptyStateText = root.findViewById(R.id.emptyStateText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SymbolCardAdapter();
        recyclerView.setAdapter(adapter);
        appSettings = new AppSettings(requireContext());
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        MT5DashboardApplication.getInstance().getSignalStateManager().addListener(this);
        refreshList();
        handler.postDelayed(refreshTick, 30_000L);
    }

    @Override
    public void onPause() {
        super.onPause();
        MT5DashboardApplication.getInstance().getSignalStateManager().removeListener(this);
        handler.removeCallbacks(refreshTick);
    }

    @Override
    public void onSignalStateChanged(Signal newOrUpdatedSignal) {
        handler.post(this::refreshList);
    }

    private void refreshList() {
        if (getContext() == null) return;

        long now = System.currentTimeMillis();
        long displayDurationMillis = appSettings.getDisplayDurationMinutes() * 60_000L;

        SignalStateManager manager = MT5DashboardApplication.getInstance().getSignalStateManager();
        List<Signal> all = manager.getAllSignals();

        Map<String, List<Signal>> bySymbol = new LinkedHashMap<>();
        for (Signal s : all) {
            boolean displayable = now < s.getReceivedAt() + displayDurationMillis;
            if (!displayable) continue;
            bySymbol.computeIfAbsent(s.getSymbol(), k -> new ArrayList<>()).add(s);
        }

        // تصمیم ۲: Sort سیگنال‌ها — TF از بزرگ به کوچک، در همان TF BUY قبل از SELL
        for (List<Signal> signals : bySymbol.values()) {
            Collections.sort(signals, (a, b) -> {
                int tfCompare = Long.compare(
                        b.getTimeframe().getBaseMinutes(),
                        a.getTimeframe().getBaseMinutes());
                if (tfCompare != 0) return tfCompare;
                if (a.getDirection() == b.getDirection()) return 0;
                return (a.getDirection() == Direction.BUY) ? -1 : 1;
            });
        }

        List<SymbolCardAdapter.SymbolGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Signal>> entry : bySymbol.entrySet()) {
            groups.add(new SymbolCardAdapter.SymbolGroup(entry.getKey(), entry.getValue()));
        }

        // تصمیم Q2: Sort کارت‌های Symbol بر اساس تازه‌ترین سیگنال داخل هر کارت (نزولی)
        Collections.sort(groups, (a, b) -> {
            long maxA = 0L;
            for (Signal s : a.signals) {
                if (s.getReceivedAt() > maxA) maxA = s.getReceivedAt();
            }
            long maxB = 0L;
            for (Signal s : b.signals) {
                if (s.getReceivedAt() > maxB) maxB = s.getReceivedAt();
            }
            return Long.compare(maxB, maxA);
        });

        adapter.submitGroups(groups);
        boolean isEmpty = groups.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
