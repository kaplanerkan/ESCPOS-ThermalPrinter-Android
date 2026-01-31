package com.karsu.thermalprinter.async;

/*
 * AsyncBluetoothEscPosPrint.java
 *
 * Modern async printing class for Bluetooth SPP connections.
 * Extends AsyncEscPosPrint with Bluetooth-specific connection handling.
 *
 * Features:
 * - Auto-selects first paired Bluetooth printer if no device specified
 * - Handles Bluetooth connection establishment
 * - Uses CompletableFuture for async operations
 * - Falls back to base class printing logic after connection
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.content.Context;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import timber.log.Timber;

/**
 * Bluetooth SPP async printing implementation.
 */
public class AsyncBluetoothEscPosPrint extends AsyncEscPosPrint {

    public AsyncBluetoothEscPosPrint(Context context) {
        super(context);
    }

    public AsyncBluetoothEscPosPrint(Context context, OnPrintFinished onPrintFinished) {
        super(context, onPrintFinished);
    }

    @Override
    protected PrinterStatus performPrint(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return new PrinterStatus(null, FINISH_NO_PRINTER);
        }

        AsyncEscPosPrinter printerData = printersData[0];
        DeviceConnection deviceConnection = printerData.getPrinterConnection();

        updateProgress(PROGRESS_CONNECTING);

        // If no connection specified, auto-select first paired Bluetooth printer
        if (deviceConnection == null) {
            Context context = contextRef.get();
            if (context == null) {
                return new PrinterStatus(null, FINISH_NO_PRINTER);
            }

            Timber.d("No Bluetooth device specified, selecting first paired printer");

            AsyncEscPosPrinter newPrinterData = new AsyncEscPosPrinter(
                BluetoothPrintersConnections.selectFirstPaired(context),
                printerData.getPrinterDpi(),
                printerData.getPrinterWidthMM(),
                printerData.getPrinterNbrCharactersPerLine()
            );
            newPrinterData.setTextsToPrint(printerData.getTextsToPrint());
            printersData[0] = newPrinterData;
        } else {
            // Connect to specified device
            try {
                Timber.d("Connecting to specified Bluetooth device");
                deviceConnection.connect();
            } catch (EscPosConnectionException e) {
                Timber.e(e, "Bluetooth connection failed");
                return new PrinterStatus(printerData, FINISH_PRINTER_DISCONNECTED);
            }
        }

        // Delegate to base class for actual printing
        return super.performPrint(printersData);
    }
}
