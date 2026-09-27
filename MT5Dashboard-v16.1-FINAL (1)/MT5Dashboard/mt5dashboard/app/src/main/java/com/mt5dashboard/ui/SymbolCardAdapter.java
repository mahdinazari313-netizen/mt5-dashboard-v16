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
import com.mt5dashboard.core.Direction;
import com.mt5dashboard.core.Signal;

import java.util.ArrayList;
import java.util.List;

public class SymbolCardAdapter extends RecyclerView.Adapter<SymbolCardAdapter.ViewHolder> {

    public static final class SymbolGroup {
        public final String symbol;
        public final List<Signal> signals;

        public SymbolGroup(String symbol, List<Signal> signals) {
            this.symbol = symbol;
            this.signals = signals;
        }
    }

    private final List<SymbolGroup> groups = new ArrayList<>();

    public void submitGroups(List<SymbolGroup> newGroups) {
        groups.clear();
        groups.addAll(newGroups);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_symbol_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SymbolGroup group = groups.get(position);
        holder.symbolTitle.setText(group.symbol);
        holder.signalsContainer.removeAllViews();

        for (Signal s : group.signals) {
            TextView row = new TextView(holder.itemView.getContext());
            String line = s.getTimeframe().name() + "  " + s.getDirection() + "  @ " + s.getPrice();
            row.setText(line);
            int colorRes = (s.getDirection() == Direction.BUY) ? R.color.colorBuy : R.color.colorSell;
            row.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorRes));
            row.setPadding(0, 4, 0, 4);
            holder.signalsContainer.addView(row);
        }
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView symbolTitle;
        final LinearLayout signalsContainer;

        ViewHolder(View itemView) {
            super(itemView);
            symbolTitle = itemView.findViewById(R.id.symbolTitle);
            signalsContainer = itemView.findViewById(R.id.signalsContainer);
        }
    }
}
