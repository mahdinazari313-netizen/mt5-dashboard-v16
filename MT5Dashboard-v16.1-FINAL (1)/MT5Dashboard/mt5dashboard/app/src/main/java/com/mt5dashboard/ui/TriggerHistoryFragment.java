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
import com.mt5dashboard.scenario.TriggerHistoryEntry;

import java.util.List;

/**
 * تب سوم (B-26): مرور فقط-خواندنی تاریخچه Triggerها، مستقل از
 * سناریوهای فعال/حذف‌شده. برخلاف ScenarioBoardsFragment، این صفحه
 * AlarmListener ثبت نمی‌کند (چون تاریخچه با هر Repeat یا Expire تغییری
 * نمی‌کند) - فقط یک Tick دوره‌ای هر ۳۰ ثانیه لازم است تا رکوردهایی که
 * از پنجره نگهداری خارج شده‌اند، از نمایش خارج شوند حتی وقتی هیچ سیگنال
 * جدیدی نمی‌رسد.
 */
public class TriggerHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private TriggerHistoryAdapter adapter;

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
        View root = inflater.inflate(R.layout.fragment_trigger_history, container, false);
        recyclerView = root.findViewById(R.id.recyclerView);
        emptyStateText = root.findViewById(R.id.emptyStateText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TriggerHistoryAdapter();
        recyclerView.setAdapter(adapter);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
        handler.postDelayed(refreshTick, 30_000L);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshTick);
    }

    private void refreshList() {
        if (getContext() == null) return;
        List<TriggerHistoryEntry> entries =
                MT5DashboardApplication.getInstance().getScenarioEngineManager().getTriggerHistory();
        adapter.submitEntries(entries);

        boolean isEmpty = entries.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
