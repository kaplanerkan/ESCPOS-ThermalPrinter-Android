package com.dantsu.escposprinter;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParser;
import com.dantsu.escposprinter.textparser.PrinterTextParserColumn;
import com.dantsu.escposprinter.textparser.IPrinterTextParserElement;
import com.dantsu.escposprinter.textparser.PrinterTextParserLine;
import com.dantsu.escposprinter.textparser.PrinterTextParserString;

public class EscPosPrinter extends EscPosPrinterSize {

    private EscPosPrinterCommands printer = null;

    /**
     * Create new instance of EscPosPrinter.
     *
     * @param printerConnection           Instance of class which implement DeviceConnection
     * @param printerDpi                  DPI of the connected printer
     * @param printerWidthMM              Printing width in millimeters
     * @param printerNbrCharactersPerLine The maximum number of characters that can be printed on a line.
     */
    public EscPosPrinter(DeviceConnection printerConnection, int printerDpi, float printerWidthMM, int printerNbrCharactersPerLine) throws EscPosConnectionException {
        this(printerConnection != null ? new EscPosPrinterCommands(printerConnection) : null, printerDpi, printerWidthMM, printerNbrCharactersPerLine);
    }

    /**
     * Create new instance of EscPosPrinter.
     *
     * @param printerConnection           Instance of class which implement DeviceConnection
     * @param printerDpi                  DPI of the connected printer
     * @param printerWidthMM              Printing width in millimeters
     * @param printerNbrCharactersPerLine The maximum number of characters that can be printed on a line.
     * @param charsetEncoding             Set the charset encoding.
     */
    public EscPosPrinter(DeviceConnection printerConnection, int printerDpi, float printerWidthMM, int printerNbrCharactersPerLine, EscPosCharsetEncoding charsetEncoding) throws EscPosConnectionException {
        this(printerConnection != null ? new EscPosPrinterCommands(printerConnection, charsetEncoding) : null, printerDpi, printerWidthMM, printerNbrCharactersPerLine);
    }

    /**
     * Create new instance of EscPosPrinter.
     *
     * @param printer                     Instance of EscPosPrinterCommands
     * @param printerDpi                  DPI of the connected printer
     * @param printerWidthMM              Printing width in millimeters
     * @param printerNbrCharactersPerLine The maximum number of characters that can be printed on a line.
     */
    public EscPosPrinter(EscPosPrinterCommands printer, int printerDpi, float printerWidthMM, int printerNbrCharactersPerLine) throws EscPosConnectionException {
        super(printerDpi, printerWidthMM, printerNbrCharactersPerLine);
        if (printer != null) {
            this.printer = printer.connect();
        }
    }

    /**
     * Close the connection with the printer.
     *
     * @return Fluent interface
     */
    public EscPosPrinter disconnectPrinter() {
        if (this.printer != null) {
            this.printer.disconnect();
            this.printer = null;
        }
        return this;
    }

    /**
     * Active "ESC *" command for image printing.
     *
     * @param enable true to use "ESC *", false to use "GS v 0"
     * @return Fluent interface
     */
    public EscPosPrinter useEscAsteriskCommand(boolean enable) {
        this.printer.useEscAsteriskCommand(enable);
        return this;
    }

    /**
     * Print a formatted text. Read the README.md for more information about text formatting options.
     *
     * @param text Formatted text to be printed.
     * @return Fluent interface
     */
    public EscPosPrinter printFormattedText(String text) throws EscPosConnectionException, EscPosParserException, EscPosEncodingException, EscPosBarcodeException {
        return this.printFormattedText(text, 20f);
    }

    /**
     * Print a formatted text. Read the README.md for more information about text formatting options.
     *
     * @param text        Formatted text to be printed.
     * @param mmFeedPaper millimeter distance feed paper at the end.
     * @return Fluent interface
     */
    public EscPosPrinter printFormattedText(String text, float mmFeedPaper) throws EscPosConnectionException, EscPosParserException, EscPosEncodingException, EscPosBarcodeException {
        return this.printFormattedText(text, this.mmToPx(mmFeedPaper));
    }

    /**
     * Print a formatted text. Read the README.md for more information about text formatting options.
     *
     * @param text          Formatted text to be printed.
     * @param dotsFeedPaper distance feed paper at the end.
     * @return Fluent interface
     */
    public EscPosPrinter printFormattedText(String text, int dotsFeedPaper) throws EscPosConnectionException, EscPosParserException, EscPosEncodingException, EscPosBarcodeException {
        if (this.printer == null || this.printerNbrCharactersPerLine == 0) {
            return this;
        }

        PrinterTextParser textParser = new PrinterTextParser(this);
        PrinterTextParserLine[] linesParsed = textParser
                .setFormattedText(text)
                .parse();

        this.printer.reset();

        for (PrinterTextParserLine line : linesParsed) {
            PrinterTextParserColumn[] columns = line.getColumns();

            IPrinterTextParserElement lastElement = null;
            for (PrinterTextParserColumn column : columns) {
                IPrinterTextParserElement[] elements = column.getElements();
                for (IPrinterTextParserElement element : elements) {
                    element.print(this.printer);
                    lastElement = element;
                }
            }

            if (lastElement instanceof PrinterTextParserString) {
                this.printer.newLine();
            }
        }

        this.printer.feedPaper(dotsFeedPaper);
        return this;
    }

    /**
     * Print a formatted text and cut the paper. Read the README.md for more information about text formatting options.
     *
     * @param text Formatted text to be printed.
     * @return Fluent interface
     */
    public EscPosPrinter printFormattedTextAndCut(String text) throws EscPosConnectionException, EscPosParserException, EscPosEncodingException, EscPosBarcodeException {
        return this.printFormattedTextAndCut(text, 20f);
    }

    /**
     * Print a formatted text and cut the paper. Read the README.md for more information about text formatting options.
     *
     * @param text        Formatted text to be printed.
     * @param mmFeedPaper millimeter distance feed paper at the end.
     * @return Fluent interface
     */
    public EscPosPrinter printFormattedTextAndCut(String text, float mmFeedPaper) throws EscPosConnectionException, EscPosParserException, EscPosEncodingException, EscPosBarcodeException {
        return this.printFormattedTextAndCut(text, this.mmToPx(mmFeedPaper));
    }

    /**
     * Print a formatted text and cut the paper. Read the README.md for more information about text formatting options.
     *
     * @param text          Formatted text to be printed.
     * @param dotsFeedPaper distance feed paper at the end.
     * @return Fluent interface
     */
    public EscPosPrinter printFormattedTextAndCut(String text, int dotsFeedPaper) throws EscPosConnectionException, EscPosParserException, EscPosEncodingException, EscPosBarcodeException {
        if (this.printer == null || this.printerNbrCharactersPerLine == 0) {
            return this;
        }

        this.printFormattedText(text, dotsFeedPaper);
        this.printer.cutPaper();

        return this;
    }

    /**
     * Print a formatted text, cut the paper and open the cash box. Read the README.md for more information about text formatting options.
     *
     * @param text        Formatted text to be printed.
     * @param mmFeedPaper millimeter distance feed paper at the end.
     * @return Fluent interface
     */
    public EscPosPrinter printFormattedTextAndOpenCashBox(String text, float mmFeedPaper) throws EscPosConnectionException, EscPosParserException, EscPosEncodingException, EscPosBarcodeException {
        return this.printFormattedTextAndOpenCashBox(text, this.mmToPx(mmFeedPaper));
    }

    /**
     * Print a formatted text, cut the paper and open the cash box. Read the README.md for more information about text formatting options.
     *
     * @param text          Formatted text to be printed.
     * @param dotsFeedPaper distance feed paper at the end.
     * @return Fluent interface
     */
    public EscPosPrinter printFormattedTextAndOpenCashBox(String text, int dotsFeedPaper) throws EscPosConnectionException, EscPosParserException, EscPosEncodingException, EscPosBarcodeException {
        if (this.printer == null || this.printerNbrCharactersPerLine == 0) {
            return this;
        }

        this.printFormattedTextAndCut(text, dotsFeedPaper);
        this.printer.openCashBox();
        return this;
    }

    /**
     * @return Charset encoding
     */
    public EscPosCharsetEncoding getEncoding() {
        return this.printer.getCharsetEncoding();
    }


    /**
     * Print all characters of all charset encoding
     *
     * @return Fluent interface
     */
    public EscPosPrinter printAllCharsetsEncodingCharacters() {
        this.printer.printAllCharsetsEncodingCharacters();
        return this;
    }

    /**
     * Print all characters of selected charsets encoding
     *
     * @param charsetsId Array of charset id to print.
     * @return Fluent interface
     */
    public EscPosPrinter printCharsetsEncodingCharacters(int[] charsetsId) {
        this.printer.printCharsetsEncodingCharacters(charsetsId);
        return this;
    }

    /**
     * Print all characters of a charset encoding
     *
     * @param charsetId Charset id to print.
     * @return Fluent interface
     */
    public EscPosPrinter printCharsetEncodingCharacters(int charsetId) {
        this.printer.printCharsetEncodingCharacters(charsetId);
        return this;
    }

    /**
     * Send raw bytes directly to the printer.
     * Useful for sending custom ESC/POS commands.
     *
     * @param bytes Raw bytes to send
     * @return Fluent interface
     */
    public EscPosPrinter printRaw(byte[] bytes) throws EscPosConnectionException {
        if (this.printer == null) {
            return this;
        }
        this.printer.printRaw(bytes);
        return this;
    }

    /**
     * Send raw hexadecimal string directly to the printer.
     * Useful for sending custom ESC/POS commands.
     * Example: "1B 40 1B 61 01" or "1B40" or "0x1B,0x40"
     *
     * @param hexString Hexadecimal string
     * @return Fluent interface
     */
    public EscPosPrinter printRawHex(String hexString) throws EscPosConnectionException {
        if (this.printer == null) {
            return this;
        }
        this.printer.printRawHex(hexString);
        return this;
    }

    /**
     * Write raw bytes to the buffer without sending.
     * Use send() to flush the buffer.
     *
     * @param bytes Raw bytes to write
     * @return Fluent interface
     */
    public EscPosPrinter write(byte[] bytes) {
        if (this.printer == null) {
            return this;
        }
        this.printer.write(bytes);
        return this;
    }

    /**
     * Send the buffer to the printer.
     *
     * @return Fluent interface
     */
    public EscPosPrinter send() throws EscPosConnectionException {
        if (this.printer == null) {
            return this;
        }
        this.printer.send();
        return this;
    }

    /**
     * Get the underlying EscPosPrinterCommands instance for advanced operations.
     *
     * @return EscPosPrinterCommands instance
     */
    public EscPosPrinterCommands getPrinterCommands() {
        return this.printer;
    }

    /**
     * Cut the paper with default feed (65 dots ~ 8mm).
     *
     * @return Fluent interface
     */
    public EscPosPrinter cutPaper() throws EscPosConnectionException {
        if (this.printer != null) {
            this.printer.cutPaper();
        }
        return this;
    }

    /**
     * Cut the paper with specified feed before cutting.
     *
     * @param feedDots Number of dots to feed before cutting (0-255)
     * @return Fluent interface
     */
    public EscPosPrinter cutPaper(int feedDots) throws EscPosConnectionException {
        if (this.printer != null) {
            this.printer.cutPaper(feedDots);
        }
        return this;
    }

    /**
     * Full cut the paper with default feed (65 dots ~ 8mm).
     *
     * @return Fluent interface
     */
    public EscPosPrinter fullCutPaper() throws EscPosConnectionException {
        if (this.printer != null) {
            this.printer.fullCutPaper();
        }
        return this;
    }

    /**
     * Full cut the paper with specified feed before cutting.
     *
     * @param feedDots Number of dots to feed before cutting (0-255)
     * @return Fluent interface
     */
    public EscPosPrinter fullCutPaper(int feedDots) throws EscPosConnectionException {
        if (this.printer != null) {
            this.printer.fullCutPaper(feedDots);
        }
        return this;
    }

    /**
     * Enable or disable the cash box opening functionality.
     * When disabled, openCashBox() calls will be ignored.
     * Use this to prevent cash box from opening during printing.
     *
     * @param enabled true to enable cash box (default), false to disable
     * @return Fluent interface
     */
    public EscPosPrinter setCashBoxEnabled(boolean enabled) {
        if (this.printer != null) {
            this.printer.setCashBoxEnabled(enabled);
        }
        return this;
    }

    /**
     * Open the cash box using pin 2 (default).
     *
     * @return Fluent interface
     */
    public EscPosPrinter openCashBox() throws EscPosConnectionException {
        if (this.printer != null) {
            this.printer.openCashBox();
        }
        return this;
    }

    /**
     * Open the cash box using specified pin.
     *
     * @param pin Pin connector (0 = pin 2, 1 = pin 5)
     * @return Fluent interface
     */
    public EscPosPrinter openCashBox(int pin) throws EscPosConnectionException {
        if (this.printer != null) {
            this.printer.openCashBox(pin);
        }
        return this;
    }
}
