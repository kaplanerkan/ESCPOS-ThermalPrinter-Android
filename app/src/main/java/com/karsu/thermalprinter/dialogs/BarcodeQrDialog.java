package com.karsu.thermalprinter.dialogs;

/*
 * BarcodeQrDialog.java
 *
 * Dialog for barcode and QR code printing.
 * Provides UI for generating and printing various barcode types and QR codes.
 *
 * Features:
 * - Barcode types: EAN13, EAN8, UPCA, UPCE, Code 128, Code 39, Code 93, ITF, CODABAR
 * - Custom barcode data input
 * - QR code generation with size control
 * - Custom QR code data input (URLs, text, etc.)
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.karsu.thermalprinter.R;

import java.util.concurrent.ExecutorService;

/**
 * Helper class for Barcode & QR Code dialog.
 * Handles barcode and QR code printing.
 */
public class BarcodeQrDialog {

    private final Context context;
    private final EscPosPrinter printer;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public BarcodeQrDialog(Context context, EscPosPrinter printer, ExecutorService executor) {
        this.context = context;
        this.printer = printer;
        this.executor = executor;
    }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_barcode_qr, null);

        // Find views
        EditText editBarcodeData = dialogView.findViewById(R.id.editBarcodeData);
        Spinner spinnerBarcodeType = dialogView.findViewById(R.id.spinnerBarcodeType);
        Button btnPrintBarcode = dialogView.findViewById(R.id.btnPrintBarcode);
        EditText editQrData = dialogView.findViewById(R.id.editQrData);
        EditText editQrSize = dialogView.findViewById(R.id.editQrSize);
        Button btnPrintQr = dialogView.findViewById(R.id.btnPrintQr);

        // Setup Barcode Type Spinner
        String[] barcodeTypes = {"EAN13", "EAN8", "UPCA", "UPCE", "128", "39", "93", "ITF", "CODABAR"};
        ArrayAdapter<String> barcodeAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, barcodeTypes);
        barcodeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarcodeType.setAdapter(barcodeAdapter);

        // Print Barcode button
        btnPrintBarcode.setOnClickListener(v -> executePrinterCommand(() -> {
            String data = editBarcodeData.getText().toString();
            String type = ((String) spinnerBarcodeType.getSelectedItem()).toLowerCase();

            try {
                String barcodeText = "[C]<barcode type='" + type + "' height='10'>" + data + "</barcode>\n";
                printer.printFormattedTextAndCut(barcodeText);
            } catch (Exception e) {
                showToast("Barcode error: " + e.getMessage());
            }
        }));

        // Print QR Code button
        btnPrintQr.setOnClickListener(v -> executePrinterCommand(() -> {
            String data = editQrData.getText().toString();
            String size = editQrSize.getText().toString();

            try {
                String qrText = "[C]<qrcode size='" + size + "'>" + data + "</qrcode>\n";
                printer.printFormattedTextAndCut(qrText);
            } catch (Exception e) {
                showToast("QR code error: " + e.getMessage());
            }
        }));

        new AlertDialog.Builder(context)
                .setTitle("Barcode & QR Code")
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
}
