package com.karsu.thermalprinter.dialogs;

/*
 * TextFormattingDialog.java
 *
 * Dialog for text formatting and styling options.
 * Provides UI for alignment, font size, font type, and text styles.
 *
 * Features:
 * - Text alignment: left, center, right
 * - Font size: normal, tall (2x height), wide (2x width), big (2x both)
 * - Font type selection: A, B, C, D, E
 * - Text styles: bold, underline
 * - Custom text input and print
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.karsu.thermalprinter.R;
import com.karsu.thermalprinter.helpers.PrinterSettings;

import java.util.concurrent.ExecutorService;

/**
 * Helper class for Text Formatting dialog.
 * Handles alignment, font size, font type, text styles, and custom text printing.
 */
public class TextFormattingDialog {

    private final Context context;
    private final EscPosPrinter printer;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final float feedPaperMm;

    // Text formatting state
    private String currentAlignment = "L";
    private String currentFontSize = "normal";
    private String currentFont = "";
    private boolean isBold = false;
    private boolean isUnderline = false;

    public TextFormattingDialog(Context context, EscPosPrinter printer, ExecutorService executor) {
        this.context = context;
        this.printer = printer;
        this.executor = executor;
        this.feedPaperMm = new PrinterSettings(context).getFeedPaperMm();
    }

    // Setters for loading saved settings
    public TextFormattingDialog setAlignment(String alignment) { this.currentAlignment = alignment; return this; }
    public TextFormattingDialog setFontSize(String fontSize) { this.currentFontSize = fontSize; return this; }
    public TextFormattingDialog setFont(String font) { this.currentFont = font; return this; }
    public TextFormattingDialog setBold(boolean bold) { this.isBold = bold; return this; }
    public TextFormattingDialog setUnderline(boolean underline) { this.isUnderline = underline; return this; }

    // Getters for saving settings
    public String getAlignment() { return currentAlignment; }
    public String getFontSize() { return currentFontSize; }
    public String getFont() { return currentFont; }
    public boolean isBold() { return isBold; }
    public boolean isUnderline() { return isUnderline; }

    public void show() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_text_formatting, null);

        // Find views
        Button btnAlignLeft = dialogView.findViewById(R.id.btnAlignLeft);
        Button btnAlignCenter = dialogView.findViewById(R.id.btnAlignCenter);
        Button btnAlignRight = dialogView.findViewById(R.id.btnAlignRight);
        Spinner spinnerFontSize = dialogView.findViewById(R.id.spinnerFontSize);
        Button btnFontA = dialogView.findViewById(R.id.btnFontA);
        Button btnFontB = dialogView.findViewById(R.id.btnFontB);
        Button btnFontC = dialogView.findViewById(R.id.btnFontC);
        Button btnFontD = dialogView.findViewById(R.id.btnFontD);
        Button btnFontE = dialogView.findViewById(R.id.btnFontE);
        Button btnBold = dialogView.findViewById(R.id.btnBold);
        Button btnUnderline = dialogView.findViewById(R.id.btnUnderline);
        EditText editCustomText = dialogView.findViewById(R.id.editCustomText);
        Button btnPrintCustomText = dialogView.findViewById(R.id.btnPrintCustomText);

        // Setup Font Size Spinner
        String[] fontSizes = {"Normal", "Tall (2x height)", "Wide (2x width)", "Big (2x both)"};
        ArrayAdapter<String> fontSizeAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, fontSizes);
        fontSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFontSize.setAdapter(fontSizeAdapter);

        // Set initial spinner selection based on current font size
        int fontSizeIndex = getFontSizeIndex(currentFontSize);
        spinnerFontSize.setSelection(fontSizeIndex);

        // Update button states
        updateAlignmentButtons(btnAlignLeft, btnAlignCenter, btnAlignRight);
        updateStyleButtons(btnBold, btnUnderline);

        // Alignment buttons
        btnAlignLeft.setOnClickListener(v -> {
            currentAlignment = "L";
            updateAlignmentButtons(btnAlignLeft, btnAlignCenter, btnAlignRight);
            showToast("Alignment: Left");
        });

        btnAlignCenter.setOnClickListener(v -> {
            currentAlignment = "C";
            updateAlignmentButtons(btnAlignLeft, btnAlignCenter, btnAlignRight);
            showToast("Alignment: Center");
        });

        btnAlignRight.setOnClickListener(v -> {
            currentAlignment = "R";
            updateAlignmentButtons(btnAlignLeft, btnAlignCenter, btnAlignRight);
            showToast("Alignment: Right");
        });

        // Font Size spinner listener
        spinnerFontSize.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentFontSize = "normal"; break;
                    case 1: currentFontSize = "tall"; break;
                    case 2: currentFontSize = "wide"; break;
                    case 3: currentFontSize = "big"; break;
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Font type buttons
        btnFontA.setOnClickListener(v -> { currentFont = ""; showToast("Font: A"); });
        btnFontB.setOnClickListener(v -> { currentFont = "font='b'"; showToast("Font: B"); });
        btnFontC.setOnClickListener(v -> { currentFont = "font='c'"; showToast("Font: C"); });
        btnFontD.setOnClickListener(v -> { currentFont = "font='d'"; showToast("Font: D"); });
        btnFontE.setOnClickListener(v -> { currentFont = "font='e'"; showToast("Font: E"); });

        // Style buttons
        btnBold.setOnClickListener(v -> {
            isBold = !isBold;
            updateStyleButtons(btnBold, btnUnderline);
            showToast("Bold: " + (isBold ? "ON" : "OFF"));
        });

        btnUnderline.setOnClickListener(v -> {
            isUnderline = !isUnderline;
            updateStyleButtons(btnBold, btnUnderline);
            showToast("Underline: " + (isUnderline ? "ON" : "OFF"));
        });

        // Print button
        btnPrintCustomText.setOnClickListener(v -> executePrinterCommand(() -> {
            String text = editCustomText.getText().toString();
            String formattedText = buildFormattedText(text);

            try {
                printer.printFormattedTextAndCut(formattedText, feedPaperMm);
            } catch (Exception e) {
                showToast("Print error: " + e.getMessage());
            }
        }));

        new AlertDialog.Builder(context)
                .setTitle("Text Formatting")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private int getFontSizeIndex(String fontSize) {
        switch (fontSize) {
            case "tall": return 1;
            case "wide": return 2;
            case "big": return 3;
            default: return 0;
        }
    }

    private void updateAlignmentButtons(Button left, Button center, Button right) {
        left.setAlpha(currentAlignment.equals("L") ? 1.0f : 0.5f);
        center.setAlpha(currentAlignment.equals("C") ? 1.0f : 0.5f);
        right.setAlpha(currentAlignment.equals("R") ? 1.0f : 0.5f);
    }

    private void updateStyleButtons(Button bold, Button underline) {
        bold.setAlpha(isBold ? 1.0f : 0.5f);
        underline.setAlpha(isUnderline ? 1.0f : 0.5f);
    }

    private String buildFormattedText(String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(currentAlignment).append("]");

        boolean hasFontTag = !currentFontSize.equals("normal") || !currentFont.isEmpty();
        if (hasFontTag) {
            sb.append("<font");
            if (!currentFontSize.equals("normal")) {
                sb.append(" size='").append(currentFontSize).append("'");
            }
            if (!currentFont.isEmpty()) {
                sb.append(" ").append(currentFont);
            }
            sb.append(">");
        }

        if (isBold) sb.append("<b>");
        if (isUnderline) sb.append("<u>");

        sb.append(text);

        if (isUnderline) sb.append("</u>");
        if (isBold) sb.append("</b>");

        if (hasFontTag) {
            sb.append("</font>");
        }

        sb.append("\n");
        return sb.toString();
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
