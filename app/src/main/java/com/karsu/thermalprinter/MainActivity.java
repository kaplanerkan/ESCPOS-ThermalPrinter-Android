package com.karsu.thermalprinter;

/*
 * MainActivity.java
 *
 * Main activity with 4 connection sections: Bluetooth, USB, TCP/IP, and Settings.
 * Provides quick print functionality for each connection type.
 *
 * Features:
 * - Bluetooth SPP printer selection and printing
 * - USB printer auto-detection with VID/PID display
 * - TCP/IP (WiFi/Ethernet) printer connection
 * - Fullscreen immersive mode for kiosk displays
 * - Responsive layout for phones, tablets, and large displays
 *
 * Uses helper classes:
 * - BluetoothPrintHelper: Bluetooth permissions and device selection
 * - UsbPrintHelper: USB device detection and printing
 * - TcpPrintHelper: Network printer connection
 * - PrintContentHelper: Test print content generation
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.karsu.thermalprinter.helpers.BluetoothPrintHelper;
import com.karsu.thermalprinter.helpers.PrintContentHelper;
import com.karsu.thermalprinter.helpers.TcpPrintHelper;
import com.karsu.thermalprinter.helpers.UsbPrintHelper;

import java.io.File;

/**
 * Main Activity with 4 sections: Bluetooth, USB, TCP/IP, and Settings.
 * Uses helper classes for cleaner code organization.
 */
public class MainActivity extends AppCompatActivity {

    // SharedPreferences
    private static final String PREFS_NAME = "ThermalPrinterPrefs";
    private static final String PREF_TCP_IP = "tcp_ip";
    private static final String PREF_TCP_PORT = "tcp_port";

    // Helpers
    private BluetoothPrintHelper bluetoothHelper;
    private UsbPrintHelper usbHelper;
    private TcpPrintHelper tcpHelper;
    private PrintContentHelper printContentHelper;

    // UI Elements
    private Button btnBluetoothBrowse;
    private EditText edtUsbVendorId;
    private EditText edtUsbProductId;
    private TextView txtUsbStatus;
    private EditText edtTcpIp;
    private EditText edtTcpPort;
    private TextView txtLogPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enableFullscreen();

        initHelpers();
        initViews();
        setupListeners();
    }

    private void initHelpers() {
        bluetoothHelper = new BluetoothPrintHelper(this);
        usbHelper = new UsbPrintHelper(this);
        tcpHelper = new TcpPrintHelper(this);
        printContentHelper = new PrintContentHelper(this);

        // USB status callback
        usbHelper.setOnUsbStatusUpdate((status, vendorId, productId) -> {
            txtUsbStatus.setText(status);
            if (vendorId != null && edtUsbVendorId.getText().toString().isEmpty()) {
                edtUsbVendorId.setText(String.valueOf(vendorId));
                edtUsbProductId.setText(String.valueOf(productId));
            }
        });

        // Bluetooth device selection callback
        bluetoothHelper.setOnDeviceSelected(deviceName -> {
            btnBluetoothBrowse.setText(deviceName);
        });
    }

    private void initViews() {
        // Bluetooth
        btnBluetoothBrowse = findViewById(R.id.button_bluetooth_browse);

        // USB
        edtUsbVendorId = findViewById(R.id.edittext_usb_vendor_id);
        edtUsbProductId = findViewById(R.id.edittext_usb_product_id);
        txtUsbStatus = findViewById(R.id.txt_usb_status);

        // TCP
        edtTcpIp = findViewById(R.id.edittext_tcp_ip);
        edtTcpPort = findViewById(R.id.edittext_tcp_port);

        // Load saved TCP settings
        loadTcpSettings();

        // Log file
        txtLogPath = findViewById(R.id.txt_log_path);
        txtLogPath.setText(ThermalPrinterApp.getLogFilePath());
    }

    private void setupListeners() {
        // Bluetooth
        btnBluetoothBrowse.setOnClickListener(v -> bluetoothHelper.browseDevices());
        findViewById(R.id.button_bluetooth).setOnClickListener(v -> printBluetooth());

        // USB
        findViewById(R.id.button_usb).setOnClickListener(v -> printUsb());

        // TCP
        findViewById(R.id.button_tcp).setOnClickListener(v -> printTcp());

        // Settings
        findViewById(R.id.button_settings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        // Log file actions
        findViewById(R.id.button_share_log).setOnClickListener(v -> shareLogFile());
        findViewById(R.id.button_clear_log).setOnClickListener(v -> clearLogFile());
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableFullscreen();
        usbHelper.updateUsbStatus();

        // Handle USB device attached
        if (getIntent() != null && UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(getIntent().getAction())) {
            usbHelper.updateUsbStatus();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            usbHelper.updateUsbStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        bluetoothHelper.onRequestPermissionsResult(requestCode, grantResults);
    }

    // ==================== PRINT METHODS ====================

    private void printBluetooth() {
        bluetoothHelper.print(
                printContentHelper.createTestPrinter(bluetoothHelper.getSelectedDevice()),
                new BluetoothPrintHelper.OnPrintResult() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Print successful!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        showError("Bluetooth Print", message);
                    }
                }
        );
    }

    private void printUsb() {
        Integer vendorId = parseIntOrNull(edtUsbVendorId.getText().toString());
        Integer productId = parseIntOrNull(edtUsbProductId.getText().toString());

        usbHelper.print(
                printContentHelper.createTestPrinter(null),
                vendorId,
                productId,
                new UsbPrintHelper.OnPrintResult() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Print successful!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        showError("USB Print", message);
                    }
                }
        );
    }

    private void printTcp() {
        String ip = edtTcpIp.getText().toString().trim();
        int port = TcpPrintHelper.parsePort(edtTcpPort.getText().toString(), 9100);

        if (ip.isEmpty()) {
            showError("TCP Print", "Please enter the printer's IP address.");
            return;
        }

        tcpHelper.print(
                printContentHelper.createTestPrinter(null),
                ip,
                port,
                new TcpPrintHelper.OnPrintResult() {
                    @Override
                    public void onSuccess() {
                        // Save TCP settings on successful print
                        saveTcpSettings(ip, port);
                        Toast.makeText(MainActivity.this, "Print successful!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        showError("TCP Print", message);
                    }
                }
        );
    }

    // ==================== UTILITY METHODS ====================

    @SuppressWarnings("deprecation")
    private void enableFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private Integer parseIntOrNull(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ==================== TCP SETTINGS METHODS ====================

    private void saveTcpSettings(String ip, int port) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(PREF_TCP_IP, ip)
                .putInt(PREF_TCP_PORT, port)
                .apply();
    }

    private void loadTcpSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedIp = prefs.getString(PREF_TCP_IP, "");
        int savedPort = prefs.getInt(PREF_TCP_PORT, 9100);

        if (!savedIp.isEmpty()) {
            edtTcpIp.setText(savedIp);
        }
        edtTcpPort.setText(String.valueOf(savedPort));
    }

    // ==================== LOG FILE METHODS ====================

    private void shareLogFile() {
        File logDir = ThermalPrinterApp.getLogDirectory();
        if (logDir == null) {
            Toast.makeText(this, "Log directory not available", Toast.LENGTH_SHORT).show();
            return;
        }

        File logFile = new File(logDir, "escpos_printer_log.txt");
        if (!logFile.exists()) {
            Toast.makeText(this, "Log file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            android.net.Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    logFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "ESC/POS Printer Log");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Log file from ESC/POS Thermal Printer app");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share log file via"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing log: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void clearLogFile() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Log File")
                .setMessage("Are you sure you want to delete the log file?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    File logDir = ThermalPrinterApp.getLogDirectory();
                    if (logDir != null) {
                        File logFile = new File(logDir, "escpos_printer_log.txt");
                        File oldLogFile = new File(logDir, "escpos_printer_log_old.txt");

                        boolean deleted = false;
                        if (logFile.exists()) {
                            deleted = logFile.delete();
                        }
                        if (oldLogFile.exists()) {
                            oldLogFile.delete();
                        }

                        if (deleted) {
                            Toast.makeText(this, "Log file deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Log file not found or already empty", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
