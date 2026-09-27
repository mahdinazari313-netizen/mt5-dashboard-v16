package com.mt5dashboard.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mt5dashboard.R;
import com.mt5dashboard.core.Signal;
import com.mt5dashboard.scenario.ScenarioConfig;
import com.mt5dashboard.scenario.ScenarioEngine;
import com.mt5dashboard.scenario.Trigger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScenarioBoardAdapter extends RecyclerView.Adapter<ScenarioBoardAdapter.ViewHolder> {

    public interface Callback {
        void onCloseScenario(ScenarioEngine engine);

        void onSilenceTrigger(ScenarioEngine engine, Trigger trigger);

        void onDismissTrigger(ScenarioEngine engine, Trigger trigger);
    }

    private final List<ScenarioEngine> engines = new ArrayList<>();
    private final Callback callback;
    // B-25 (v16.1): زمان ساخت Trigger روی همان ردیف نمایش داده می‌شود.
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ScenarioBoardAdapter(Callback callback) {
        this.callback = callback;
    }

    public void submitEngines(List<ScenarioEngine> newEngines) {
        engines.clear();
        engines.addAll(newEngines);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scenario_board, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScenarioEngine engine = engines.get(position);
        ScenarioConfig config = engine.getConfig();

        holder.scenarioName.setText(config.getName());
        holder.scenarioSettingsSummary.setText(buildSettingsSummary(config));

        holder.buttonClose.setOnClickListener(v -> callback.onCloseScenario(engine));

        holder.triggersContainer.removeAllViews();
        List<Trigger> activeTriggers = engine.getActiveTriggers();

        if (activeTriggers.isEmpty()) {
            TextView emptyText = new TextView(holder.itemView.getContext());
            emptyText.setText(R.string.scenario_no_active_trigger);
            emptyText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorTextSecondary));
            emptyText.setTextSize(13f);
            holder.triggersContainer.addView(emptyText);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        for (Trigger trigger : activeTriggers) {
            View row = inflater.inflate(R.layout.item_trigger_row, holder.triggersContainer, false);
            TextView triggerInfo = row.findViewById(R.id.triggerInfo);
            TextView buttonSilence = row.findViewById(R.id.buttonSilence);
            TextView buttonDismiss = row.findViewById(R.id.buttonDismiss);

            triggerInfo.setText(buildTriggerText(trigger));

            buttonDismiss.setOnClickListener(v -> callback.onDismissTrigger(engine, trigger));

            if (trigger.isSilenced()) {
                buttonSilence.setText(R.string.scenario_silenced_label);
                buttonSilence.setEnabled(false);
                buttonSilence.setAlpha(0.5f);
            } else {
                buttonSilence.setText(R.string.scenario_silence_button);
                buttonSilence.setEnabled(true);
                buttonSilence.setAlpha(1f);
                buttonSilence.setOnClickListener(v -> {
                    callback.onSilenceTrigger(engine, trigger);
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(currentPosition);
                    }
                });
            }

            holder.triggersContainer.addView(row);
        }
    }

    private String buildSettingsSummary(ScenarioConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("Multiplier: ").append(config.getValidityMultiplier());
        sb.append("  |  Repeat: ").append(config.getRepeatIntervalMinutes()).append("m");
        if (config.isMainTimeFrameEnabled()) {
            sb.append("  |  Main TF: ").append(config.getMainTimeframe());
        }
        if (config.isPriceDifferenceEnabled()) {
            sb.append("  |  Price Diff: On");
        }
        if (config.isMinimumTimeframeSyncEnabled()) {
            sb.append("  |  Min TF Sync: ").append(config.getMinimumTimeframeCount());
        }
        if (config.isDualTimeframeEnabled()) {
            sb.append("  |  Dual: On");
        }
        return sb.toString();
    }

    /**
     * B-25 (v16.1): خط اول شامل Symbol/Direction/زمان ساخت (createdAt، نه
     * زمان فعلی)، خط دوم ترکیب TF@price. state تغییری نکرده - Trigger و
     * Sort از قبل بر اساس createdAt کار می‌کردند؛ این فقط نمایش UI است.
     */
    private String buildTriggerText(Trigger trigger) {
        StringBuilder header = new StringBuilder();
        header.append(trigger.getSymbol()).append(" ").append(trigger.getDirection())
                .append("   ").append(timeFormat.format(new Date(trigger.getCreatedAt())));

        StringBuilder combination = new StringBuilder();
        for (Signal s : trigger.getCombination()) {
            if (combination.length() > 0) combination.append(" ");
            combination.append(s.getTimeframe()).append("@").append(s.getPrice());
        }
        return header + "\n" + combination;
    }

    @Override
    public int getItemCount() {
        return engines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView scenarioName;
        final TextView scenarioSettingsSummary;
        final TextView buttonClose;
        final LinearLayout triggersContainer;

        ViewHolder(View itemView) {
            super(itemView);
            scenarioName = itemView.findViewById(R.id.scenarioName);
            scenarioSettingsSummary = itemView.findViewById(R.id.scenarioSettingsSummary);
            buttonClose = itemView.findViewById(R.id.buttonClose);
            triggersContainer = itemView.findViewById(R.id.triggersContainer);
        }
    }
}
