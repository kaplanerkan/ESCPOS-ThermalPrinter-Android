package com.karsu.thermalprinter.async;

/*
 * AsyncEscPosPrint.java
 *
 * Modern async printing class using CompletableFuture and lambdas.
 * Handles background printing with progress dialog and error handling.
 *
 * Features:
 * - CompletableFuture-based async execution (replaces deprecated AsyncTask)
 * - Progress dialog with status updates
 * - Functional callback interface for print completion
 * - Error handling for connection, parsing, encoding, and barcode errors
 * - Support for multiple texts printing with delay
 *
 * Status codes:
 * - FINISH_SUCCESS: Print completed successfully
 * - FINISH_NO_PRINTER: No printer connection found
 * - FINISH_PRINTER_DISCONNECTED: Connection lost during printing
 * - FINISH_PARSER_ERROR: Invalid ESC/POS format
 * - FINISH_ENCODING_ERROR: Character encoding issue
 * - FINISH_BARCODE_ERROR: Invalid barcode data
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.karsu.thermalprinter.R;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * Modern async ESC/POS printing using CompletableFuture.
 */
public class AsyncEscPosPrint {

    // Status codes
    public static final int FINISH_SUCCESS = 1;
    public static final int FINISH_NO_PRINTER = 2;
    public static final int FINISH_PRINTER_DISCONNECTED = 3;
    public static final int FINISH_PARSER_ERROR = 4;
    public static final int FINISH_ENCODING_ERROR = 5;
    public static final int FINISH_BARCODE_ERROR = 6;

    // Progress states
    protected static final int PROGRESS_CONNECTING = 1;
    protected static final int PROGRESS_CONNECTED = 2;
    protected static final int PROGRESS_PRINTING = 3;
    protected static final int PROGRESS_PRINTED = 4;

    // UI elements
    protected AlertDialog progressDialog;
    protected ProgressBar progressBar;
    protected TextView progressMessage;

    // Context and callbacks
    protected final WeakReference<Context> contextRef;
    protected final OnPrintFinished onPrintFinished;

    // Threading
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());
    protected final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Functional interface for print completion callback.
     */
    public interface OnPrintFinished {
        void onError(AsyncEscPosPrinter printer, int errorCode);
        void onSuccess(AsyncEscPosPrinter printer);
    }

    public AsyncEscPosPrint(Context context) {
        this(context, null);
    }

    public AsyncEscPosPrint(Context context, OnPrintFinished onPrintFinished) {
        this.contextRef = new WeakReference<>(context);
        this.onPrintFinished = onPrintFinished;
    }

    /**
     * Execute the print task asynchronously.
     */
    public void execute(AsyncEscPosPrinter... printersData) {
        // Show progress dialog on main thread
        showProgressDialog();

        // Run print operation in background thread
        executor.execute(() -> {
            PrinterStatus result = performPrint(printersData);
            // Post result handling to main thread
            mainHandler.post(() -> handleResult(result));
        });
    }

    /**
     * Perform the actual printing operation (runs on background thread).
     */
    protected PrinterStatus performPrint(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return new PrinterStatus(null, FINISH_NO_PRINTER);
        }

        updateProgress(PROGRESS_CONNECTING);

        AsyncEscPosPrinter printerData = printersData[0];

        try {
            DeviceConnection deviceConnection = printerData.getPrinterConnection();

            if (deviceConnection == null) {
                return new PrinterStatus(null, FINISH_NO_PRINTER);
            }

            EscPosPrinter printer = new EscPosPrinter(
                deviceConnection,
                printerData.getPrinterDpi(),
                printerData.getPrinterWidthMM(),
                printerData.getPrinterNbrCharactersPerLine(),
                new EscPosCharsetEncoding("windows-1252", 16)
            );

            updateProgress(PROGRESS_PRINTING);

            String[] textsToPrint = printerData.getTextsToPrint();
            for (String text : textsToPrint) {
                printer.printFormattedTextAndCut(text);
                Thread.sleep(500);
            }

            updateProgress(PROGRESS_PRINTED);
            Timber.i("Print completed successfully");

            return new PrinterStatus(printerData, FINISH_SUCCESS);

        } catch (EscPosConnectionException e) {
            Timber.e(e, "Printer connection error");
            return new PrinterStatus(printerData, FINISH_PRINTER_DISCONNECTED);
        } catch (EscPosParserException e) {
            Timber.e(e, "Parser error");
            return new PrinterStatus(printerData, FINISH_PARSER_ERROR);
        } catch (EscPosEncodingException e) {
            Timber.e(e, "Encoding error");
            return new PrinterStatus(printerData, FINISH_ENCODING_ERROR);
        } catch (EscPosBarcodeException e) {
            Timber.e(e, "Barcode error");
            return new PrinterStatus(printerData, FINISH_BARCODE_ERROR);
        } catch (InterruptedException e) {
            Timber.e(e, "Print interrupted");
            Thread.currentThread().interrupt();
            return new PrinterStatus(printerData, FINISH_PRINTER_DISCONNECTED);
        }
    }

    /**
     * Update progress dialog (thread-safe).
     */
    protected void updateProgress(int progress) {
        mainHandler.post(() -> {
            if (progressDialog == null || progressMessage == null || progressBar == null) {
                return;
            }

            String message;
            switch (progress) {
                case PROGRESS_CONNECTING:
                    message = "Connecting printer...";
                    break;
                case PROGRESS_CONNECTED:
                    message = "Printer is connected...";
                    break;
                case PROGRESS_PRINTING:
                    message = "Printer is printing...";
                    break;
                case PROGRESS_PRINTED:
                    message = "Printer has finished...";
                    break;
                default:
                    message = "...";
            }
            progressMessage.setText(message);
            progressBar.setProgress(progress);
        });
    }

    /**
     * Show progress dialog.
     */
    protected void showProgressDialog() {
        Context context = contextRef.get();
        if (context == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_print_progress, null);
        progressBar = view.findViewById(R.id.progressBar);
        progressMessage = view.findViewById(R.id.progressMessage);

        progressBar.setMax(4);
        progressBar.setProgress(0);
        progressMessage.setText("...");

        progressDialog = new AlertDialog.Builder(context)
            .setTitle("Printing in progress...")
            .setView(view)
            .setCancelable(false)
            .create();
        progressDialog.show();
    }

    /**
     * Handle print result on main thread.
     */
    protected void handleResult(PrinterStatus result) {
        // Dismiss progress dialog
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        Context context = contextRef.get();
        if (context == null) return;

        // Show result dialog
        showResultDialog(context, result);

        // Invoke callback
        if (onPrintFinished != null) {
            if (result.getStatus() == FINISH_SUCCESS) {
                onPrintFinished.onSuccess(result.getPrinter());
            } else {
                onPrintFinished.onError(result.getPrinter(), result.getStatus());
            }
        }
    }

    /**
     * Show result dialog based on status.
     */
    private void showResultDialog(Context context, PrinterStatus result) {
        String title;
        String message;

        switch (result.getStatus()) {
            case FINISH_SUCCESS:
                title = "Success";
                message = "The print job completed successfully!";
                break;
            case FINISH_NO_PRINTER:
                title = "No printer";
                message = "No printer connection found.";
                break;
            case FINISH_PRINTER_DISCONNECTED:
                title = "Connection error";
                message = "Unable to connect to the printer.";
                break;
            case FINISH_PARSER_ERROR:
                title = "Format error";
                message = "Invalid text format syntax.";
                break;
            case FINISH_ENCODING_ERROR:
                title = "Encoding error";
                message = "Character encoding error occurred.";
                break;
            case FINISH_BARCODE_ERROR:
                title = "Barcode error";
                message = "Invalid barcode or QR code data.";
                break;
            default:
                title = "Error";
                message = "An unknown error occurred.";
        }

        new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Printer status result class.
     */
    public static class PrinterStatus {
        private final AsyncEscPosPrinter printer;
        private final int status;

        public PrinterStatus(AsyncEscPosPrinter printer, int status) {
            this.printer = printer;
            this.status = status;
        }

        public AsyncEscPosPrinter getPrinter() {
            return printer;
        }

        public int getStatus() {
            return status;
        }
    }
}
