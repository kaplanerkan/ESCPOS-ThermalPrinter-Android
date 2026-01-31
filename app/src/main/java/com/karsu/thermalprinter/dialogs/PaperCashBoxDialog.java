package com.karsu.thermalprinter.dialogs;

/*
 * PaperCashBoxDialog.java
 *
 * Dialog for paper and cash box control settings.
 * Provides UI for paper cutting, cash drawer, and image printing options.
 *
 * Features:
 * - Paper cut control: partial cut and full cut with feed dots adjustment
 * - Cash box/drawer control: pin selection (2 or 5) and open command
 * - Line spacing adjustment for text printing
 * - Image delay configuration for stable printing
 * - ESC * command toggle for image printing compatibility
 * - Settings persistence between dialog opens
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.karsu.thermalprinter.R;

import java.util.concurrent.ExecutorService;

/**
 * Helper class for Paper & Cash Box dialog.
 * Handles paper cutting, cash box control, line spacing, and image delay settings.
 */
public class PaperCashBoxDialog {

    private final Context context;
    private final EscPosPrinter printer;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Settings with defaults
    private int cutFeedDots = 65;
    private int lineSpacingDots = 30;
    private int imageDelayMs = 5;
    private boolean cashBoxEnabled = true;
    private int cashBoxPin = 0;
    private boolean useEscAsterisk = false;

    public PaperCashBoxDialog(Context context, EscPosPrinter printer, ExecutorService executor) {
        this.context = context;
        this.printer = printer;
        this.executor = executor;
    }

    // Setters for loading saved settings
    public PaperCashBoxDialog setCutFeedDots(int dots) { this.cutFeedDots = dots; return this; }
    public PaperCashBoxDialog setLineSpacingDots(int dots) { this.lineSpacingDots = dots; return this; }
    public PaperCashBoxDialog setImageDelayMs(int ms) { this.imageDelayMs = ms; return this; }
    public PaperCashBoxDialog setCashBoxEnabled(boolean enabled) { this.cashBoxEnabled = enabled; return this; }
    public PaperCashBoxDialog setCashBoxPin(int pin) { this.cashBoxPin = pin; return this; }
    public PaperCashBoxDialog setUseEscAsterisk(boolean use) { this.useEscAsterisk = use; return this; }

    // Getters for saving settings
    public int getCutFeedDots() { return cutFeedDots; }
    public int getLineSpacingDots() { return lineSpacingDots; }
    public int getImageDelayMs() { return imageDelayMs; }
    public boolean isCashBoxEnabled() { return cashBoxEnabled; }
    public int getCashBoxPin() { return cashBoxPin; }
    public boolean isUseEscAsterisk() { return useEscAsterisk; }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_paper_cashbox, null);

        // Find views
        SeekBar seekCutFeed = dialogView.findViewById(R.id.seekCutFeed);
        TextView txtCutFeedValue = dialogView.findViewById(R.id.txtCutFeedValue);
        Button btnPartialCut = dialogView.findViewById(R.id.btnPartialCut);
        Button btnFullCut = dialogView.findViewById(R.id.btnFullCut);
        CheckBox chkCashBoxEnabled = dialogView.findViewById(R.id.chkCashBoxEnabled);
        Spinner spinnerCashBoxPin = dialogView.findViewById(R.id.spinnerCashBoxPin);
        Button btnOpenCashBox = dialogView.findViewById(R.id.btnOpenCashBox);
        SeekBar seekLineSpacing = dialogView.findViewById(R.id.seekLineSpacing);
        TextView txtLineSpacingValue = dialogView.findViewById(R.id.txtLineSpacingValue);
        Button btnResetLineSpacing = dialogView.findViewById(R.id.btnResetLineSpacing);
        SeekBar seekImageDelay = dialogView.findViewById(R.id.seekImageDelay);
        TextView txtImageDelayValue = dialogView.findViewById(R.id.txtImageDelayValue);
        CheckBox chkUseEscAsterisk = dialogView.findViewById(R.id.chkUseEscAsterisk);

        // Setup Cash Box Pin Spinner
        String[] cashBoxPins = {"Pin 2", "Pin 5"};
        ArrayAdapter<String> cashBoxAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, cashBoxPins);
        cashBoxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCashBoxPin.setAdapter(cashBoxAdapter);

        // Load current values
        seekCutFeed.setProgress(cutFeedDots);
        txtCutFeedValue.setText(String.valueOf(cutFeedDots));
        chkCashBoxEnabled.setChecked(cashBoxEnabled);
        spinnerCashBoxPin.setSelection(cashBoxPin);
        seekLineSpacing.setProgress(lineSpacingDots);
        txtLineSpacingValue.setText(String.valueOf(lineSpacingDots));
        seekImageDelay.setProgress(imageDelayMs);
        txtImageDelayValue.setText(String.valueOf(imageDelayMs));
        chkUseEscAsterisk.setChecked(useEscAsterisk);

        // SeekBar listeners
        seekCutFeed.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cutFeedDots = progress;
                txtCutFeedValue.setText(String.valueOf(progress));
            }
        });

        seekLineSpacing.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lineSpacingDots = progress;
                txtLineSpacingValue.setText(String.valueOf(progress));
            }
        });

        seekImageDelay.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                imageDelayMs = progress;
                txtImageDelayValue.setText(String.valueOf(progress));
            }
        });

        // Button actions
        btnPartialCut.setOnClickListener(v -> executePrinterCommand(() -> {
            try {
                printer.getPrinterCommands().cutPaper(cutFeedDots);
            } catch (Exception e) {
                showToast("Cut error: " + e.getMessage());
            }
        }));

        btnFullCut.setOnClickListener(v -> executePrinterCommand(() -> {
            try {
                printer.getPrinterCommands().feedPaper(cutFeedDots);
                printer.getPrinterCommands().cutPaper();
            } catch (Exception e) {
                showToast("Cut error: " + e.getMessage());
            }
        }));

        btnOpenCashBox.setOnClickListener(v -> executePrinterCommand(() -> {
            try {
                cashBoxPin = spinnerCashBoxPin.getSelectedItemPosition();
                printer.openCashBox(cashBoxPin);
            } catch (Exception e) {
                showToast("Cash box error: " + e.getMessage());
            }
        }));

        btnResetLineSpacing.setOnClickListener(v -> {
            lineSpacingDots = 30;
            seekLineSpacing.setProgress(30);
            executePrinterCommand(() -> {
                printer.resetLineSpacing();
                showToast("Line spacing reset");
            });
        });

        chkCashBoxEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cashBoxEnabled = isChecked;
            try {
                printer.getPrinterCommands().setCashBoxEnabled(isChecked);
            } catch (Exception ignored) {}
        });

        chkUseEscAsterisk.setOnCheckedChangeListener((buttonView, isChecked) -> {
            useEscAsterisk = isChecked;
            try {
                printer.getPrinterCommands().useEscAsteriskCommand(isChecked);
            } catch (Exception ignored) {}
        });

        new AlertDialog.Builder(context)
                .setTitle("Paper & Cash Box")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void executePrinterCommand(Runnable command) {
        if (printer == null) {
            showToast("Printer not connected");
            return;
        }
        executor.execute(command);
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    // Simple SeekBar listener to reduce boilerplate
    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
