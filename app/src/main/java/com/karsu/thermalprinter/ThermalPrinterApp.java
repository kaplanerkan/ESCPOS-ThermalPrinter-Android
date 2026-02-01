package com.karsu.thermalprinter;

/*
 * KarSu ThermalPrinterApp.java
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

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

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

    @Override
    public void onCreate() {
        super.onCreate();

        // Use app-specific external storage (no permission needed)
        // Path: /storage/emulated/0/Android/data/com.karsu.thermalprinter/files/
        logDirectory = getExternalFilesDir(null);

        // Plant Timber trees first (before Firebase)

        Timber.plant(new FileLoggingTree());   // File in app-specific storage

        Timber.i("=== ThermalPrinter App Started ===");
        Timber.i("Log file: %s", getLogFilePath());

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Configure Crashlytics
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCrashlyticsCollectionEnabled(true);

        Timber.i("Firebase initialized, Crashlytics enabled");
    }

    /**
     * Custom Timber tree that writes logs to a file in app-specific storage.
     * No special permissions required.
     */
    public static class FileLoggingTree extends Timber.Tree {

        private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

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
                File logFile = prepareLogFile();
                if (logFile == null) return;

                writeLogEntry(logFile, priority, tag, message, t);
            } catch (IOException e) {
                Log.e("FileLoggingTree", "Error writing to log file", e);
            }
        }

        @Nullable
        private File prepareLogFile() {
            if (!logDirectory.exists() && !logDirectory.mkdirs()) {
                return null;
            }

            File logFile = new File(logDirectory, LOG_FILE_NAME);

            // Limit file size to 5MB, rotate if needed
            if (logFile.exists() && logFile.length() > 5 * 1024 * 1024) {
                rotateLogFile(logFile);
            }

            return new File(logDirectory, LOG_FILE_NAME);
        }

        private void rotateLogFile(File logFile) {
            File oldLog = new File(logDirectory, "escpos_printer_log_old.txt");
            if (oldLog.exists()) {
                //noinspection ResultOfMethodCallIgnored
                oldLog.delete();
            }
            //noinspection ResultOfMethodCallIgnored
            logFile.renameTo(oldLog);
        }

        private void writeLogEntry(File logFile, int priority, String tag, String message, @Nullable Throwable t) throws IOException {
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault());
                String timestamp = dateFormat.format(new Date());
                String priorityStr = getPriorityString(priority);

                pw.println(timestamp + " " + priorityStr + "/" + tag + ": " + message);

                if (t != null) {
                    t.printStackTrace(pw);
                }
            }
        }

        private String getPriorityString(int priority) {
            return switch (priority) {
                case Log.VERBOSE -> "V";
                case Log.DEBUG -> "D";
                case Log.INFO -> "I";
                case Log.WARN -> "W";
                case Log.ERROR -> "E";
                case Log.ASSERT -> "A";
                default -> "?";
            };
        }
    }
}
