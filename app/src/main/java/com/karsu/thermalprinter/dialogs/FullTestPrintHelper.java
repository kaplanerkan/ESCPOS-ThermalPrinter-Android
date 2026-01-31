package com.karsu.thermalprinter.dialogs;

/*
 * FullTestPrintHelper.java
 *
 * Helper class for executing comprehensive test prints.
 * Prints a full test page demonstrating all ESC/POS formatting features.
 *
 * Test print includes:
 * - Text alignment: left, center, right
 * - Font sizes: normal, tall, wide, big
 * - Text styles: bold, underline, combined
 * - Special characters handling
 * - Barcode (EAN13)
 * - QR code
 *
 * Useful for verifying printer capabilities and connection quality.
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;

import java.util.concurrent.ExecutorService;

/**
 * Helper class for executing full test print.
 * Prints a comprehensive test page with various formatting options.
 */
public class FullTestPrintHelper {

    private final Context context;
    private final EscPosPrinter printer;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public FullTestPrintHelper(Context context, EscPosPrinter printer, ExecutorService executor) {
        this.context = context;
        this.printer = printer;
        this.executor = executor;
    }

    public void execute() {
        if (printer == null) {
            showToast("Printer not connected");
            return;
        }

        executor.execute(() -> {
            try {
                String testPrint =
                        "[C]<font size='big'><b>FULL TEST PRINT</b></font>\n" +
                        "[C]================================\n" +
                        "[L]\n" +
                        "[L]<b>Left Aligned Text</b>\n" +
                        "[C]<b>Center Aligned Text</b>\n" +
                        "[R]<b>Right Aligned Text</b>\n" +
                        "[L]\n" +
                        "[C]--- Font Sizes ---\n" +
                        "[L]Normal text\n" +
                        "[L]<font size='tall'>Tall text</font>\n" +
                        "[L]<font size='wide'>Wide text</font>\n" +
                        "[L]<font size='big'>Big text</font>\n" +
                        "[L]\n" +
                        "[C]--- Text Styles ---\n" +
                        "[L]<b>Bold text</b>\n" +
                        "[L]<u>Underlined text</u>\n" +
                        "[L]<b><u>Bold + Underline</u></b>\n" +
                        "[L]\n" +
                        "[C]--- Special Characters ---\n" +
                        "[L]Less than: < (fixed!)\n" +
                        "[L]Greater than: >\n" +
                        "[L]Price: 19.99 EUR\n" +
                        "[L]\n" +
                        "[C]--- Barcode ---\n" +
                        "[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
                        "[L]\n" +
                        "[C]--- QR Code ---\n" +
                        "[C]<qrcode size='20'>https://github.com/kaplanerkan/ESCPOS-ThermalPrinter-Android</qrcode>\n" +
                        "[L]\n" +
                        "[C]================================\n" +
                        "[C]Test completed!\n" +
                        "[L]\n";

                printer.printFormattedTextAndCut(testPrint);
                showToast("Full test print completed");
            } catch (Exception e) {
                showToast("Print error: " + e.getMessage());
            }
        });
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}
