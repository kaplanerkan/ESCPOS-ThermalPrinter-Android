package com.karsu.thermalprinter.dialogs;

/*
 * RawCommandsDialog.java
 *
 * Dialog for sending raw ESC/POS commands to the printer.
 * Allows direct hex command input for advanced testing.
 *
 * Features:
 * - Raw hex command input (space-separated, e.g., "1B 40")
 * - Direct command transmission to printer
 * - Useful for debugging and custom command testing
 *
 * Common ESC/POS commands:
 * - 1B 40: Initialize printer
 * - 1B 64 XX: Feed XX lines
 * - 1D 56 00: Full cut
 * - 1D 56 01: Partial cut
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.karsu.thermalprinter.R;

import java.util.concurrent.ExecutorService;

/**
 * Helper class for Raw ESC/POS Commands dialog.
 * Handles sending raw hex commands to the printer.
 */
public class RawCommandsDialog {

    private final Context context;
    private final EscPosPrinter printer;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RawCommandsDialog(Context context, EscPosPrinter printer, ExecutorService executor) {
        this.context = context;
        this.printer = printer;
        this.executor = executor;
    }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_raw_commands, null);

        // Find views
        EditText editRawHex = dialogView.findViewById(R.id.editRawHex);
        Button btnSendRaw = dialogView.findViewById(R.id.btnSendRaw);

        // Send Raw Command button
        btnSendRaw.setOnClickListener(v -> executePrinterCommand(() -> {
            String hexString = editRawHex.getText().toString().trim();
            byte[] bytes = hexStringToBytes(hexString);

            if (bytes != null) {
                try {
                    printer.getPrinterCommands().printRaw(bytes);
                    showToast("Raw command sent");
                } catch (Exception e) {
                    showToast("Error: " + e.getMessage());
                }
            } else {
                showToast("Invalid hex string");
            }
        }));

        new AlertDialog.Builder(context)
                .setTitle("Raw ESC/POS Commands")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    /**
     * Converts a hex string (space-separated) to byte array.
     * Example: "1B 40" -> {0x1B, 0x40}
     */
    private byte[] hexStringToBytes(String hexString) {
        try {
            String[] hexParts = hexString.split("\\s+");
            byte[] bytes = new byte[hexParts.length];
            for (int i = 0; i < hexParts.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hexParts[i], 16);
            }
            return bytes;
        } catch (Exception e) {
            return null;
        }
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
}
