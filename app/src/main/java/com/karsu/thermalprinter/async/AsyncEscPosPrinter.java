package com.karsu.thermalprinter.async;

/*
 * AsyncEscPosPrinter.java
 *
 * Data class for async printing operations.
 * Extends EscPosPrinterSize to hold connection and text data for printing.
 *
 * Features:
 * - Stores printer connection reference
 * - Manages array of texts to print
 * - Supports method chaining for fluent API
 * - Inherits printer size parameters (DPI, width, characters per line)
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import com.dantsu.escposprinter.EscPosPrinterSize;
import com.dantsu.escposprinter.connection.DeviceConnection;

/**
 * Printer data holder for async print operations.
 * Supports fluent API with method chaining.
 */
public class AsyncEscPosPrinter extends EscPosPrinterSize {

    private DeviceConnection printerConnection;
    private String[] textsToPrint = new String[0];
    private float feedPaperMm = 20f;

    public AsyncEscPosPrinter(DeviceConnection printerConnection, int printerDpi, float printerWidthMM, int printerNbrCharactersPerLine) {
        super(printerDpi, printerWidthMM, printerNbrCharactersPerLine);
        this.printerConnection = printerConnection;
    }

    /**
     * Get feed paper distance in mm.
     */
    public float getFeedPaperMm() {
        return this.feedPaperMm;
    }

    /**
     * Set feed paper distance in mm (fluent API).
     */
    public AsyncEscPosPrinter setFeedPaperMm(float mm) {
        this.feedPaperMm = mm;
        return this;
    }

    /**
     * Get the printer connection.
     */
    public DeviceConnection getPrinterConnection() {
        return this.printerConnection;
    }

    /**
     * Set the printer connection (fluent API).
     */
    public AsyncEscPosPrinter setConnection(DeviceConnection connection) {
        this.printerConnection = connection;
        return this;
    }

    /**
     * Set all texts to print (fluent API).
     */
    public AsyncEscPosPrinter setTextsToPrint(String[] textsToPrint) {
        this.textsToPrint = textsToPrint != null ? textsToPrint : new String[0];
        return this;
    }

    /**
     * Add a single text to print (fluent API).
     */
    public AsyncEscPosPrinter addTextToPrint(String textToPrint) {
        String[] newArray = new String[this.textsToPrint.length + 1];
        System.arraycopy(this.textsToPrint, 0, newArray, 0, this.textsToPrint.length);
        newArray[this.textsToPrint.length] = textToPrint;
        this.textsToPrint = newArray;
        return this;
    }

    /**
     * Get all texts to print.
     */
    public String[] getTextsToPrint() {
        return this.textsToPrint;
    }
}
