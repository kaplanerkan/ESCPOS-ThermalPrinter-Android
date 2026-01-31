package com.karsu.thermalprinter.helpers;

/*
 * TcpPrintHelper.java
 *
 * Helper class for TCP/IP (WiFi/Ethernet) printing operations.
 * Handles network connection and print execution over TCP sockets.
 *
 * Features:
 * - TCP/IP connection to network printers
 * - Configurable IP address and port (default: 9100)
 * - Async print execution with callback support
 * - Input validation for IP address and port
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.app.Activity;
import android.app.AlertDialog;

import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.karsu.thermalprinter.async.AsyncEscPosPrint;
import com.karsu.thermalprinter.async.AsyncEscPosPrinter;
import com.karsu.thermalprinter.async.AsyncTcpEscPosPrint;

import timber.log.Timber;

public class TcpPrintHelper {

    private final Activity activity;

    public interface OnPrintResult {
        void onSuccess();
        void onError(String message);
    }

    public TcpPrintHelper(Activity activity) {
        this.activity = activity;
    }

    public void print(AsyncEscPosPrinter printer, String ipAddress, int port, OnPrintResult result) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            showError("Please enter the printer's IP address.");
            if (result != null) result.onError("IP address required");
            return;
        }

        new AsyncTcpEscPosPrint(activity, new AsyncEscPosPrint.OnPrintFinished() {
            @Override
            public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                Timber.e("Print error: %d", codeException);
                if (result != null) result.onError("Print failed: " + codeException);
            }

            @Override
            public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                Timber.i("Print success");
                if (result != null) result.onSuccess();
            }
        }).execute(printer.setConnection(new TcpConnection(ipAddress.trim(), port)));
    }

    public void showError(String message) {
        new AlertDialog.Builder(activity)
                .setTitle("TCP Connection")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    public static int parsePort(String portStr, int defaultPort) {
        if (portStr == null || portStr.trim().isEmpty()) {
            return defaultPort;
        }
        try {
            return Integer.parseInt(portStr.trim());
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }
}
