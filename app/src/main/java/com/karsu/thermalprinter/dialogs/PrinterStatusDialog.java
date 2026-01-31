package com.karsu.thermalprinter.dialogs;

/*
 * PrinterStatusDialog.java
 *
 * Dialog for querying and displaying printer status.
 * Shows real-time printer state information.
 *
 * Features:
 * - Online/offline status
 * - Paper status: end, near end
 * - Cover open detection
 * - Cash drawer status
 * - Error detection
 * - Ready state
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
import android.widget.TextView;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.PrinterStatus;
import com.karsu.thermalprinter.R;

import java.util.concurrent.ExecutorService;

/**
 * Helper class for Printer Status dialog.
 * Handles querying and displaying printer status.
 */
public class PrinterStatusDialog {

    private final Context context;
    private final EscPosPrinter printer;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PrinterStatusDialog(Context context, EscPosPrinter printer, ExecutorService executor) {
        this.context = context;
        this.printer = printer;
        this.executor = executor;
    }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_printer_status, null);

        // Find views
        Button btnQueryStatus = dialogView.findViewById(R.id.btnQueryStatus);
        TextView txtPrinterStatus = dialogView.findViewById(R.id.txtPrinterStatus);

        // Query Status button
        btnQueryStatus.setOnClickListener(v -> {
            if (printer == null) {
                txtPrinterStatus.setText("Status: Not connected");
                return;
            }

            executor.execute(() -> {
                try {
                    PrinterStatus status = printer.queryStatus();
                    if (status != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Online: ").append(status.isOnline()).append("\n");
                        sb.append("Paper End: ").append(status.isPaperEnd()).append("\n");
                        sb.append("Paper Near End: ").append(status.isPaperNearEnd()).append("\n");
                        sb.append("Cover Open: ").append(status.isCoverOpen()).append("\n");
                        sb.append("Drawer Open: ").append(status.isDrawerOpen()).append("\n");
                        sb.append("Error: ").append(status.isErrorOccurred()).append("\n");
                        sb.append("Ready: ").append(status.isReady());

                        mainHandler.post(() -> txtPrinterStatus.setText(sb.toString()));
                    } else {
                        mainHandler.post(() -> txtPrinterStatus.setText("Status: No response from printer"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> txtPrinterStatus.setText("Status: Error - " + e.getMessage()));
                }
            });
        });

        new AlertDialog.Builder(context)
                .setTitle("Printer Status")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}
