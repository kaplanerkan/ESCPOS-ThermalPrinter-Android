package com.karsu.thermalprinter.helpers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class for printer settings stored in SharedPreferences.
 */
public class PrinterSettings {
    private static final String PREFS_NAME = "PrinterSettings";
    private static final String KEY_FEED_PAPER_MM = "feed_paper_mm";
    private static final String KEY_DPI = "dpi";
    private static final String KEY_WIDTH_MM = "width_mm";
    private static final String KEY_CHARS_PER_LINE = "chars_per_line";

    // Default values
    private static final float DEFAULT_FEED_PAPER_MM = 20f;
    private static final int DEFAULT_DPI = 203;
    private static final float DEFAULT_WIDTH_MM = 48f;
    private static final int DEFAULT_CHARS_PER_LINE = 32;

    private final SharedPreferences prefs;

    public PrinterSettings(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Feed Paper (mm)
    public float getFeedPaperMm() {
        return prefs.getFloat(KEY_FEED_PAPER_MM, DEFAULT_FEED_PAPER_MM);
    }

    public void setFeedPaperMm(float mm) {
        prefs.edit().putFloat(KEY_FEED_PAPER_MM, mm).apply();
    }

    // DPI
    public int getDpi() {
        return prefs.getInt(KEY_DPI, DEFAULT_DPI);
    }

    public void setDpi(int dpi) {
        prefs.edit().putInt(KEY_DPI, dpi).apply();
    }

    // Width (mm)
    public float getWidthMm() {
        return prefs.getFloat(KEY_WIDTH_MM, DEFAULT_WIDTH_MM);
    }

    public void setWidthMm(float mm) {
        prefs.edit().putFloat(KEY_WIDTH_MM, mm).apply();
    }

    // Characters per line
    public int getCharsPerLine() {
        return prefs.getInt(KEY_CHARS_PER_LINE, DEFAULT_CHARS_PER_LINE);
    }

    public void setCharsPerLine(int chars) {
        prefs.edit().putInt(KEY_CHARS_PER_LINE, chars).apply();
    }
}
