package com.karsu.thermalprinter.helpers;

/*
 * PrintContentHelper.java
 *
 * Helper class for generating print content.
 * Creates formatted ESC/POS text with images, barcodes, and QR codes.
 *
 * Features:
 * - Test receipt generation with logo, text, barcode, and QR code
 * - ESC/POS formatted text creation
 * - Image to hexadecimal string conversion
 * - Reusable print content templates
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import androidx.core.content.res.ResourcesCompat;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.karsu.thermalprinter.R;
import com.karsu.thermalprinter.async.AsyncEscPosPrinter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class for generating print content.
 */
public class PrintContentHelper {

    private final Context context;
    private final PrinterSettings printerSettings;

    public PrintContentHelper(Context context) {
        this.context = context;
        this.printerSettings = new PrinterSettings(context);
    }

    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter createTestPrinter(DeviceConnection connection) {
        SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(
                connection,
                printerSettings.getDpi(),
                printerSettings.getWidthMm(),
                printerSettings.getCharsPerLine()
        );
        printer.setFeedPaperMm(printerSettings.getFeedPaperMm());

        Drawable logoDrawable = ResourcesCompat.getDrawableForDensity(
                context.getResources(), R.drawable.logo, DisplayMetrics.DENSITY_MEDIUM, context.getTheme()
        );

        return printer.addTextToPrint(
                "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, logoDrawable) + "</img>\n" +
                        "[L]\n" +
                        "[C]<u><font size='big'>ORDER N°045</font></u>\n" +
                        "[L]\n" +
                        "[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                        "[C]\n" +
                        "[C]================================\n" +
                        "[L]\n" +
                        "[L]<b>BEAUTIFUL SHIRT</b>[R]9.99 EUR\n" +
                        "[L]  + Size : S\n" +
                        "[L]\n" +
                        "[L]<b>AWESOME HAT</b>[R]24.99 EUR\n" +
                        "[L]  + Size : 57/58\n" +
                        "[L]\n" +
                        "[C]--------------------------------\n" +
                        "[R]TOTAL PRICE :[R]34.98 EUR\n" +
                        "[R]TAX :[R]4.23 EUR\n" +
                        "[L]\n" +
                        "[C]================================\n" +
                        "[L]\n" +
                        "[L]<u><font color='bg-black' size='tall'>Customer :</font></u>\n" +
                        "[L]Raymond DUPONT\n" +
                        "[L]5 rue des girafes\n" +
                        "[L]31547 PERPETES\n" +
                        "[L]Tel : +33801201456\n" +
                        "\n" +
                        "[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
                        "[L]\n" +
                        "[C]<qrcode size='20'>https://dantsu.com/</qrcode>\n"
        );
    }
}
