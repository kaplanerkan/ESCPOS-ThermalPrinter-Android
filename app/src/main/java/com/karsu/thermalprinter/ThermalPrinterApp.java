package com.karsu.thermalprinter;

/*
 * ThermalPrinterApp.java
 *
 * Application class for ESC/POS Thermal Printer.
 * Initializes Timber logging with file and logcat output.
 *
 * Features:
 * - Dual logging: Logcat (DebugTree) + File (FileLoggingTree)
 * - App-specific storage for log files (no permissions required)
 * - Log file rotation at 5MB limit
 * - Filtered logging for printer-related tags only
 *
 * Log file location:
 * /storage/emulated/0/Android/data/com.karsu.thermalprinter/files/escpos_printer_log.txt
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class ThermalPrinterApp extends Application {

    private static final String LOG_FILE_NAME = "escpos_printer_log.txt";
    private static File logDirectory;

    @Override
    public void onCreate() {
        super.onCreate();

        // Use app-specific external storage (no permission needed)
        // Path: /storage/emulated/0/Android/data/com.karsu.thermalprinter/files/
        logDirectory = getExternalFilesDir(null);

        // Plant Timber trees
        Timber.plant(new Timber.DebugTree()); // Logcat
        Timber.plant(new FileLoggingTree());   // File in app-specific storage

        Timber.i("=== ThermalPrinter App Started ===");
        Timber.i("Log file: " + getLogFilePath());
    }

    /**
     * Get the log file path.
     * Location: /storage/emulated/0/Android/data/com.karsu.thermalprinter/files/escpos_printer_log.txt
     */
    public static String getLogFilePath() {
        if (logDirectory != null) {
            return new File(logDirectory, LOG_FILE_NAME).getAbsolutePath();
        }
        return "Log directory not initialized";
    }

    /**
     * Get the log directory for external access.
     */
    public static File getLogDirectory() {
        return logDirectory;
    }

    /**
     * Custom Timber tree that writes logs to a file in app-specific storage.
     * No special permissions required.
     */
    public static class FileLoggingTree extends Timber.Tree {

        private static final SimpleDateFormat DATE_FORMAT =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

        @Override
        protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
            // Only log DEBUG and above to file
            if (priority < Log.DEBUG) {
                return;
            }

            // Filter to only log printer-related tags
            if (tag == null || (!tag.contains("Connection") && !tag.contains("Printer") &&
                    !tag.contains("EscPos") && !tag.contains("Ble") && !tag.contains("Usb") &&
                    !tag.contains("Tcp") && !tag.contains("Bluetooth") && !tag.contains("Settings") &&
                    !tag.contains("ThermalPrinterApp"))) {
                return;
            }

            if (logDirectory == null) {
                return;
            }

            try {
                if (!logDirectory.exists()) {
                    logDirectory.mkdirs();
                }

                File logFile = new File(logDirectory, LOG_FILE_NAME);

                // Limit file size to 5MB, rotate if needed
                if (logFile.exists() && logFile.length() > 5 * 1024 * 1024) {
                    File oldLog = new File(logDirectory, "escpos_printer_log_old.txt");
                    if (oldLog.exists()) {
                        oldLog.delete();
                    }
                    logFile.renameTo(oldLog);
                    logFile = new File(logDirectory, LOG_FILE_NAME);
                }

                FileWriter fw = new FileWriter(logFile, true);
                PrintWriter pw = new PrintWriter(fw);

                String timestamp = DATE_FORMAT.format(new Date());
                String priorityStr = getPriorityString(priority);

                pw.println(timestamp + " " + priorityStr + "/" + tag + ": " + message);

                if (t != null) {
                    t.printStackTrace(pw);
                }

                pw.flush();
                pw.close();
            } catch (IOException e) {
                Log.e("FileLoggingTree", "Error writing to log file", e);
            }
        }

        private String getPriorityString(int priority) {
            switch (priority) {
                case Log.VERBOSE: return "V";
                case Log.DEBUG: return "D";
                case Log.INFO: return "I";
                case Log.WARN: return "W";
                case Log.ERROR: return "E";
                case Log.ASSERT: return "A";
                default: return "?";
            }
        }
    }
}
