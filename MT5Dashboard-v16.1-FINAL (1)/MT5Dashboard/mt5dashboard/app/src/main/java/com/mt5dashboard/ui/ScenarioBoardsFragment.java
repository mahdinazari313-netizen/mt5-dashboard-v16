package com.mt5dashboard.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mt5dashboard.MT5DashboardApplication;
import com.mt5dashboard.R;
import com.mt5dashboard.core.Signal;
import com.mt5dashboard.core.SignalStateManager;
import com.mt5dashboard.scenario.ScenarioEngine;
import com.mt5dashboard.scenario.Trigger;

public class ScenarioBoardsFragment extends Fragment implements
        SignalStateManager.Listener, ScenarioBoardAdapter.Callback {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ScenarioBoardAdapter adapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTick = new Runnable() {
        @Override
        public void run() {
            refreshList();
            handler.postDelayed(this, 30_000L);
        }
    };

    private ActivityResultLauncher<Intent> createScenarioLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createScenarioLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        refreshList();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_scenarios, container, false);
        recyclerView = root.findViewById(R.id.recyclerView);
        emptyStateText = root.findViewById(R.id.emptyStateText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ScenarioBoardAdapter(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = root.findViewById(R.id.fabAddScenario);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreateScenarioActivity.class);
            createScenarioLauncher.launch(intent);
        });

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
        java.util.List<ScenarioEngine> engines =
                MT5DashboardApplication.getInstance().getScenarioEngineManager().getAllEngines();
        adapter.submitEngines(engines);

        boolean isEmpty = engines.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCloseScenario(ScenarioEngine engine) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.scenario_close_confirm_title)
                .setMessage(R.string.scenario_close_confirm_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    MT5DashboardApplication.getInstance().getScenarioEngineManager()
                            .closeScenario(engine.getConfig().getId());
                    refreshList();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onSilenceTrigger(ScenarioEngine engine, Trigger trigger) {
        engine.silence(trigger.getSymbol(), trigger.getDirection());
        refreshList();
    }

    @Override
    public void onDismissTrigger(ScenarioEngine engine, Trigger trigger) {
        // B-28: فقط Trigger فعلی حذف می‌شود؛ سناریو و Signalهای موجود
        // در SignalStateManager دست‌نخورده می‌مانند.
        engine.dismiss(
                engine.getConfig().getId(),
                trigger.getSymbol(),
                trigger.getDirection(),
                trigger.getId());
        refreshList();
    }
}
