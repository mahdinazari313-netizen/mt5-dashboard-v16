package com.mt5dashboard.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.mt5dashboard.MT5DashboardApplication;
import com.mt5dashboard.R;
import com.mt5dashboard.core.Timeframe;
import com.mt5dashboard.scenario.ScenarioConfig;

/**
 * فرم ساخت سناریوی جدید.
 *
 * === تغییرات v13 ===
 * تصمیم Q3: اگر Dual روشن شود، Minimum Timeframe Sync مسدود می‌شود
 * (غیرفعال + خاموش). اگر Dual خاموش شود، Minimum Sync دوباره فعال
 * می‌شود. یک چک دفاعی هم در saveScenario وجود دارد.
 */
public class CreateScenarioActivity extends AppCompatActivity {

    private EditText inputName;
    private SwitchMaterial switchMainTimeFrame;
    private Spinner spinnerMainTimeframe;
    private SwitchMaterial switchPriceDifference;
    private SwitchMaterial switchMinimumTimeframeSync;
    private View layoutMinimumTimeframeCount;
    private EditText inputMinimumTimeframeCount;
    private SwitchMaterial switchDualTimeframe;
    private EditText inputValidityMultiplier;
    private EditText inputRepeatInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_scenario);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        inputName = findViewById(R.id.inputName);
        switchMainTimeFrame = findViewById(R.id.switchMainTimeFrame);
        spinnerMainTimeframe = findViewById(R.id.spinnerMainTimeframe);
        switchPriceDifference = findViewById(R.id.switchPriceDifference);
        switchMinimumTimeframeSync = findViewById(R.id.switchMinimumTimeframeSync);
        layoutMinimumTimeframeCount = findViewById(R.id.layoutMinimumTimeframeCount);
        inputMinimumTimeframeCount = findViewById(R.id.inputMinimumTimeframeCount);
        switchDualTimeframe = findViewById(R.id.switchDualTimeframe);
        inputValidityMultiplier = findViewById(R.id.inputValidityMultiplier);
        inputRepeatInterval = findViewById(R.id.inputRepeatInterval);

        setupTimeframeSpinner();

        switchMainTimeFrame.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                spinnerMainTimeframe.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        switchMinimumTimeframeSync.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                layoutMinimumTimeframeCount.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // تصمیم Q3: اگر Dual روشن شود، Minimum Timeframe Sync مسدود می‌شود
        switchDualTimeframe.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            switchMinimumTimeframeSync.setEnabled(!isChecked);
            if (isChecked) {
                switchMinimumTimeframeSync.setChecked(false);
                layoutMinimumTimeframeCount.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.buttonSave).setOnClickListener(v -> saveScenario());
    }

    private void setupTimeframeSpinner() {
        ArrayAdapter<Timeframe> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, Timeframe.values());
        spinnerMainTimeframe.setAdapter(adapter);
    }

    private void saveScenario() {
        String name = inputName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.scenario_name_required_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (switchDualTimeframe.isChecked() && !switchMainTimeFrame.isChecked()) {
            Toast.makeText(this, R.string.scenario_dual_requires_main_tf_error, Toast.LENGTH_LONG).show();
            return;
        }

        int validityMultiplier = parseIntOrDefault(inputValidityMultiplier, 10, 1);
        int repeatInterval = parseIntOrDefault(inputRepeatInterval, 10, 1);

        ScenarioConfig config = new ScenarioConfig(name);
        config.setValidityMultiplier(validityMultiplier);
        config.setRepeatIntervalMinutes(repeatInterval);
        config.setMainTimeFrameEnabled(switchMainTimeFrame.isChecked());
        if (switchMainTimeFrame.isChecked()) {
            Timeframe selected = (Timeframe) spinnerMainTimeframe.getSelectedItem();
            config.setMainTimeframe(selected);
        }
        config.setPriceDifferenceEnabled(switchPriceDifference.isChecked());

        config.setMinimumTimeframeSyncEnabled(switchMinimumTimeframeSync.isChecked());
        if (switchMinimumTimeframeSync.isChecked()) {
            int minimumTimeframeCount = parseIntOrDefault(inputMinimumTimeframeCount, 2, 2);
            config.setMinimumTimeframeCount(minimumTimeframeCount);
        }

        config.setDualTimeframeEnabled(switchDualTimeframe.isChecked());

        // تصمیم Q3 (چک دفاعی): اگر Dual فعال است، Minimum Sync نباید فعال باشد
        if (config.isDualTimeframeEnabled()) {
            config.setMinimumTimeframeSyncEnabled(false);
        }

        MT5DashboardApplication.getInstance().getScenarioEngineManager().createScenario(config);

        setResult(RESULT_OK);
        finish();
    }

    private int parseIntOrDefault(EditText field, int defaultValue, int minimum) {
        String text = field.getText().toString().trim();
        if (text.isEmpty()) return defaultValue;
        try {
            int value = Integer.parseInt(text);
            return value >= minimum ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
