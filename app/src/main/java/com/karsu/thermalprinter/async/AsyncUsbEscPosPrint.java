package com.karsu.thermalprinter.async;

/*
 * AsyncUsbEscPosPrint.java
 *
 * Modern async printing class for USB connections.
 * Extends AsyncEscPosPrint for USB printer support with batch mode optimization.
 *
 * Features:
 * - USB host mode connection to printers
 * - Supports USB OTG connections
 * - Batch mode for faster USB transfers (buffers all data before sending)
 * - Uses ExecutorService for async operations
 * - Inherits base class progress dialog and error handling
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.content.Context;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;

import timber.log.Timber;

/**
 * USB async printing implementation with batch mode optimization.
 * Batch mode buffers all ESC/POS commands and sends them in one transfer,
 * significantly improving USB print speed.
 */
public class AsyncUsbEscPosPrint extends AsyncEscPosPrint {

    public AsyncUsbEscPosPrint(Context context) {
        super(context);
    }

    public AsyncUsbEscPosPrint(Context context, OnPrintFinished onPrintFinished) {
        super(context, onPrintFinished);
    }

    /**
     * Perform the actual printing with batch mode enabled for USB.
     * This overrides the base class to enable batch mode, which buffers
     * all data and sends it in one transfer for better performance.
     */
    @Override
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

            // Enable batch mode for USB - all data will be buffered
            Timber.i("USB: Enabling batch mode for optimized transfer");
            deviceConnection.setBatchMode(true);

            EscPosPrinter printer = new EscPosPrinter(
                deviceConnection,
                printerData.getPrinterDpi(),
                printerData.getPrinterWidthMM(),
                printerData.getPrinterNbrCharactersPerLine(),
                new EscPosCharsetEncoding("windows-1252", 16)
            );

            updateProgress(PROGRESS_PRINTING);

            String[] textsToPrint = printerData.getTextsToPrint();
            float feedMm = printerData.getFeedPaperMm();
            for (String text : textsToPrint) {
                printer.printFormattedTextAndCut(text, feedMm);
            }

            // Flush all buffered data at once
            Timber.i("USB: Flushing batch buffer");
            deviceConnection.flushBatch();

            updateProgress(PROGRESS_PRINTED);
            Timber.i("USB: Print completed successfully with batch mode");

            return new PrinterStatus(printerData, FINISH_SUCCESS);

        } catch (EscPosConnectionException e) {
            Timber.e(e, "USB printer connection error");
            return new PrinterStatus(printerData, FINISH_PRINTER_DISCONNECTED);
        } catch (EscPosParserException e) {
            Timber.e(e, "USB parser error");
            return new PrinterStatus(printerData, FINISH_PARSER_ERROR);
        } catch (EscPosEncodingException e) {
            Timber.e(e, "USB encoding error");
            return new PrinterStatus(printerData, FINISH_ENCODING_ERROR);
        } catch (EscPosBarcodeException e) {
            Timber.e(e, "USB barcode error");
            return new PrinterStatus(printerData, FINISH_BARCODE_ERROR);
        }
    }
}
