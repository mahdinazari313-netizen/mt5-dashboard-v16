package com.mt5dashboard.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mt5dashboard.R;
import com.mt5dashboard.scenario.TriggerHistoryEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * B-26 (v16.1): لیست تخت (بدون گروه‌بندی) رکوردهای TriggerHistoryEntry،
 * جدیدترین اول. item_trigger_row.xml دوباره استفاده می‌شود، اما هر دو
 * دکمه (Silent و Dismiss) همیشه مخفی هستند - یک رکورد تاریخچه مربوط به
 * گذشته است و هیچ‌کدام از این دو عمل برایش معنی ندارد.
 */
public class TriggerHistoryAdapter extends RecyclerView.Adapter<TriggerHistoryAdapter.ViewHolder> {

    private final List<TriggerHistoryEntry> entries = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public void submitEntries(List<TriggerHistoryEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trigger_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TriggerHistoryEntry entry = entries.get(position);

        String time = timeFormat.format(new Date(entry.getCreatedAt()));
        String header = time + "   " + entry.getScenarioName() + "   "
                + entry.getSymbol() + " " + entry.getDirection();
        holder.info.setText(header + "\n" + buildCombinationSummary(entry));

        holder.buttonSilence.setVisibility(View.GONE);
        holder.buttonDismiss.setVisibility(View.GONE);
    }

    private String buildCombinationSummary(TriggerHistoryEntry entry) {
        StringBuilder sb = new StringBuilder();
        for (com.mt5dashboard.core.Signal signal : entry.getCombinationSnapshot()) {
            if (sb.length() > 0) sb.append("  +  ");
            sb.append(signal.getTimeframe()).append(" @ ").append(signal.getPrice());
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView info;
        final TextView buttonSilence;
        final TextView buttonDismiss;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            info = itemView.findViewById(R.id.triggerInfo);
            buttonSilence = itemView.findViewById(R.id.buttonSilence);
            buttonDismiss = itemView.findViewById(R.id.buttonDismiss);
        }
    }
}
