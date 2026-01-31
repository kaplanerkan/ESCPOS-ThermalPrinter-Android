package com.karsu.thermalprinter;

/*
 * SettingsActivity.java
 *
 * Advanced settings activity for printer configuration and testing.
 * Provides connection options, test dialogs, and printer parameter settings.
 *
 * Features:
 * - Multiple connection types: Bluetooth SPP, Bluetooth LE, USB, TCP/IP
 * - BLE device scanning with real-time discovery
 * - Configurable printer parameters: DPI, width (mm), characters per line
 * - Test dialogs for various ESC/POS features:
 *   - Paper & Cash Box control
 *   - Text formatting (alignment, font, styles)
 *   - Barcode & QR code printing
 *   - Printer status query
 *   - Raw ESC/POS commands
 *   - Full comprehensive test print
 * - Fullscreen immersive mode
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BleDeviceScanner;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothLeConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.karsu.thermalprinter.databinding.ActivitySettingsBinding;
import com.karsu.thermalprinter.dialogs.BarcodeQrDialog;
import com.karsu.thermalprinter.dialogs.FullTestPrintHelper;
import com.karsu.thermalprinter.dialogs.PaperCashBoxDialog;
import com.karsu.thermalprinter.dialogs.PrinterStatusDialog;
import com.karsu.thermalprinter.dialogs.RawCommandsDialog;
import com.karsu.thermalprinter.dialogs.TextFormattingDialog;
import com.karsu.thermalprinter.helpers.PrinterSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private DeviceConnection currentConnection;
    private EscPosPrinter printer;
    private BluetoothConnection selectedBluetoothDevice;
    private BluetoothLeConnection selectedBleDevice;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // BLE scanning
    private BleDeviceScanner bleScanner;
    private List<BluetoothLeConnection> discoveredBleDevices = new ArrayList<>();
    private ArrayAdapter<String> bleDevicesAdapter;

    // Dialog helpers (retain settings between dialog opens)
    private PaperCashBoxDialog paperCashBoxDialog;
    private TextFormattingDialog textFormattingDialog;

    // Printer settings helper
    private PrinterSettings printerSettings;

    private static final String ACTION_USB_PERMISSION = "com.karsu.thermalprinter.USB_PERMISSION";

    // Bluetooth permissions
    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;
    public static final int PERMISSION_LOCATION = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable fullscreen immersive mode (must be after setContentView)
        enableFullscreen();

        // Initialize printer settings
        printerSettings = new PrinterSettings(this);
        loadPrinterSettings();

        setupConnectionSection();
        setupCategoryButtons();
    }

    private void loadPrinterSettings() {
        binding.editDpi.setText(String.valueOf(printerSettings.getDpi()));
        binding.editWidthMm.setText(String.valueOf(printerSettings.getWidthMm()));
        binding.editCharsPerLine.setText(String.valueOf(printerSettings.getCharsPerLine()));
        binding.editFeedPaper.setText(String.valueOf(printerSettings.getFeedPaperMm()));
    }

    private void savePrinterSettings() {
        try {
            printerSettings.setDpi(Integer.parseInt(binding.editDpi.getText().toString()));
            printerSettings.setWidthMm(Float.parseFloat(binding.editWidthMm.getText().toString()));
            printerSettings.setCharsPerLine(Integer.parseInt(binding.editCharsPerLine.getText().toString()));
            printerSettings.setFeedPaperMm(Float.parseFloat(binding.editFeedPaper.getText().toString()));
        } catch (NumberFormatException e) {
            // Ignore invalid values
        }
    }

    public float getFeedPaperMm() {
        try {
            return Float.parseFloat(binding.editFeedPaper.getText().toString());
        } catch (NumberFormatException e) {
            return 20f;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableFullscreen();
    }

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

    // ==================== CONNECTION SECTION ====================

    private void setupConnectionSection() {
        // Connection type radio group
        binding.radioGroupConnection.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBluetooth) {
                binding.layoutBluetooth.setVisibility(View.VISIBLE);
                binding.layoutBle.setVisibility(View.GONE);
                binding.layoutTcp.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioBluetoothLe) {
                binding.layoutBluetooth.setVisibility(View.GONE);
                binding.layoutBle.setVisibility(View.VISIBLE);
                binding.layoutTcp.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioUsb) {
                binding.layoutBluetooth.setVisibility(View.GONE);
                binding.layoutBle.setVisibility(View.GONE);
                binding.layoutTcp.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioTcp) {
                binding.layoutBluetooth.setVisibility(View.GONE);
                binding.layoutBle.setVisibility(View.GONE);
                binding.layoutTcp.setVisibility(View.VISIBLE);
            }
        });

        // Bluetooth device selection
        binding.btnSelectBluetooth.setOnClickListener(v -> selectBluetoothDevice());

        // BLE device scanning
        setupBleSection();

        // Connect button
        binding.btnConnect.setOnClickListener(v -> connectPrinter());
    }

    @SuppressLint("MissingPermission")
    private void setupBleSection() {
        bleScanner = new BleDeviceScanner(this);

        // BLE devices spinner adapter
        bleDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        bleDevicesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBleDevices.setAdapter(bleDevicesAdapter);

        binding.spinnerBleDevices.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < discoveredBleDevices.size()) {
                    selectedBleDevice = discoveredBleDevices.get(position);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedBleDevice = null;
            }
        });

        binding.btnScanBle.setOnClickListener(v -> startBleScan());
        binding.btnStopBleScan.setOnClickListener(v -> stopBleScan());
    }

    @SuppressLint("MissingPermission")
    private void startBleScan() {
        checkBluetoothPermissions(() -> {
            if (!bleScanner.isBluetoothLeSupported()) {
                showToast("BLE not supported or Bluetooth is disabled");
                return;
            }

            // Clear previous results
            discoveredBleDevices.clear();
            bleDevicesAdapter.clear();
            binding.spinnerBleDevices.setVisibility(View.GONE);
            binding.btnStopBleScan.setVisibility(View.VISIBLE);
            binding.btnScanBle.setEnabled(false);
            binding.txtBleScanStatus.setText("Scanning for BLE printers...");

            bleScanner.startScan(new BleDeviceScanner.OnBleScanListener() {
                @Override
                public void onDeviceFound(BluetoothLeConnection device) {
                    mainHandler.post(() -> {
                        discoveredBleDevices.add(device);
                        String deviceName = device.getDevice().getName();
                        if (deviceName == null || deviceName.isEmpty()) {
                            deviceName = device.getDevice().getAddress();
                        }
                        bleDevicesAdapter.add(deviceName);
                        binding.spinnerBleDevices.setVisibility(View.VISIBLE);
                        binding.txtBleScanStatus.setText("Found " + discoveredBleDevices.size() + " device(s)");
                    });
                }

                @Override
                public void onScanComplete(List<BluetoothLeConnection> devices) {
                    mainHandler.post(() -> {
                        binding.btnStopBleScan.setVisibility(View.GONE);
                        binding.btnScanBle.setEnabled(true);
                        if (devices.isEmpty()) {
                            binding.txtBleScanStatus.setText("No BLE devices found. Try again.");
                        } else {
                            binding.txtBleScanStatus.setText("Scan complete. Found " + devices.size() + " device(s)");
                            if (selectedBleDevice == null && !devices.isEmpty()) {
                                selectedBleDevice = devices.get(0);
                            }
                        }
                    });
                }

                @Override
                public void onScanFailed(int errorCode) {
                    mainHandler.post(() -> {
                        binding.btnStopBleScan.setVisibility(View.GONE);
                        binding.btnScanBle.setEnabled(true);
                        binding.txtBleScanStatus.setText("Scan failed (error: " + errorCode + ")");
                        showToast("BLE scan failed");
                    });
                }
            }, 10000); // 10 second scan
        });
    }

    private void stopBleScan() {
        if (bleScanner != null) {
            bleScanner.stopScan();
        }
        binding.btnStopBleScan.setVisibility(View.GONE);
        binding.btnScanBle.setEnabled(true);
    }

    @SuppressLint("MissingPermission")
    private void selectBluetoothDevice() {
        checkBluetoothPermissions(() -> {
            BluetoothConnection[] devices = new BluetoothPrintersConnections(this).getList();
            if (devices == null || devices.length == 0) {
                showToast("No Bluetooth printers found");
                return;
            }

            String[] deviceNames = new String[devices.length];
            for (int i = 0; i < devices.length; i++) {
                BluetoothDevice device = devices[i].getDevice();
                deviceNames[i] = device.getName() != null ? device.getName() : device.getAddress();
            }

            new AlertDialog.Builder(this)
                    .setTitle("Select Bluetooth Printer")
                    .setItems(deviceNames, (dialog, which) -> {
                        selectedBluetoothDevice = devices[which];
                        binding.btnSelectBluetooth.setText(deviceNames[which]);
                    })
                    .show();
        });
    }

    private void connectPrinter() {
        executor.execute(() -> {
            try {
                // Disconnect current connection if any
                if (currentConnection != null) {
                    currentConnection.disconnect();
                    currentConnection = null;
                    printer = null;
                }

                int dpi = Integer.parseInt(binding.editDpi.getText().toString());
                float widthMm = Float.parseFloat(binding.editWidthMm.getText().toString());
                int charsPerLine = Integer.parseInt(binding.editCharsPerLine.getText().toString());

                // Save settings
                mainHandler.post(this::savePrinterSettings);

                if (binding.radioBluetooth.isChecked()) {
                    if (selectedBluetoothDevice == null) {
                        mainHandler.post(() -> showToast("Please select a Bluetooth device first"));
                        return;
                    }
                    currentConnection = selectedBluetoothDevice;
                } else if (binding.radioBluetoothLe.isChecked()) {
                    if (selectedBleDevice == null) {
                        mainHandler.post(() -> showToast("Please scan and select a BLE device first"));
                        return;
                    }
                    currentConnection = selectedBleDevice;
                } else if (binding.radioUsb.isChecked()) {
                    UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
                    if (usbConnection == null) {
                        mainHandler.post(() -> showToast("No USB printer found"));
                        return;
                    }
                    requestUsbPermission(usbConnection);
                    return;
                } else if (binding.radioTcp.isChecked()) {
                    String ip = binding.editTcpIp.getText().toString().trim();
                    int port = Integer.parseInt(binding.editTcpPort.getText().toString());
                    if (ip.isEmpty()) {
                        mainHandler.post(() -> showToast("Please enter IP address"));
                        return;
                    }
                    currentConnection = new TcpConnection(ip, port);
                }

                printer = new EscPosPrinter(currentConnection, dpi, widthMm, charsPerLine);

                mainHandler.post(() -> {
                    binding.txtConnectionStatus.setText("Status: Connected");
                    binding.txtConnectionStatus.setTextColor(0xFF4CAF50);
                    binding.layoutTestCategories.setVisibility(View.VISIBLE);
                    showToast("Printer connected successfully");
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    binding.txtConnectionStatus.setText("Status: Error - " + e.getMessage());
                    binding.txtConnectionStatus.setTextColor(0xFFFF5722);
                    binding.layoutTestCategories.setVisibility(View.GONE);
                    showToast("Connection error: " + e.getMessage());
                });
            }
        });
    }

    // ==================== USB PERMISSION ====================

    private void requestUsbPermission(UsbConnection usbConnection) {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager == null) return;

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(ACTION_USB_PERMISSION),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbReceiver, filter);
        }

        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    UsbDevice device;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
                    } else {
                        @SuppressWarnings("deprecation")
                        UsbDevice legacyDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        device = legacyDevice;
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                            currentConnection = new UsbConnection(usbManager, device);
                            executor.execute(() -> {
                                try {
                                    int dpi = Integer.parseInt(binding.editDpi.getText().toString());
                                    float widthMm = Float.parseFloat(binding.editWidthMm.getText().toString());
                                    int charsPerLine = Integer.parseInt(binding.editCharsPerLine.getText().toString());

                                    printer = new EscPosPrinter(currentConnection, dpi, widthMm, charsPerLine);

                                    mainHandler.post(() -> {
                                        binding.txtConnectionStatus.setText("Status: Connected (USB)");
                                        binding.txtConnectionStatus.setTextColor(0xFF4CAF50);
                                        binding.layoutTestCategories.setVisibility(View.VISIBLE);
                                        showToast("USB Printer connected");
                                    });
                                } catch (Exception e) {
                                    mainHandler.post(() -> showToast("Error: " + e.getMessage()));
                                }
                            });
                        }
                    } else {
                        showToast("USB permission denied");
                    }
                }
            }
        }
    };

    // ==================== TEST CATEGORY BUTTONS ====================

    private void setupCategoryButtons() {
        binding.btnCategoryPaper.setOnClickListener(v -> showPaperCashBoxDialog());
        binding.btnCategoryText.setOnClickListener(v -> showTextFormattingDialog());
        binding.btnCategoryBarcode.setOnClickListener(v -> showBarcodeQrDialog());
        binding.btnCategoryStatus.setOnClickListener(v -> showPrinterStatusDialog());
        binding.btnCategoryRaw.setOnClickListener(v -> showRawCommandsDialog());
        binding.btnFullTest.setOnClickListener(v -> executeFullTestPrint());
    }

    private void showPaperCashBoxDialog() {
        if (!checkPrinterConnected()) return;

        // Reuse dialog to retain settings
        if (paperCashBoxDialog == null) {
            paperCashBoxDialog = new PaperCashBoxDialog(this, printer, executor);
        }
        paperCashBoxDialog.show();
    }

    private void showTextFormattingDialog() {
        if (!checkPrinterConnected()) return;

        // Reuse dialog to retain settings
        if (textFormattingDialog == null) {
            textFormattingDialog = new TextFormattingDialog(this, printer, executor);
        }
        textFormattingDialog.show();
    }

    private void showBarcodeQrDialog() {
        if (!checkPrinterConnected()) return;
        new BarcodeQrDialog(this, printer, executor).show();
    }

    private void showPrinterStatusDialog() {
        if (!checkPrinterConnected()) return;
        new PrinterStatusDialog(this, printer, executor).show();
    }

    private void showRawCommandsDialog() {
        if (!checkPrinterConnected()) return;
        new RawCommandsDialog(this, printer, executor).show();
    }

    private void executeFullTestPrint() {
        if (!checkPrinterConnected()) return;
        new FullTestPrintHelper(this, printer, executor).execute();
    }

    private boolean checkPrinterConnected() {
        if (printer == null) {
            showToast("Printer not connected");
            return false;
        }
        return true;
    }

    // ==================== PERMISSIONS ====================

    public interface OnBluetoothPermissionsGranted {
        void onPermissionsGranted();
    }

    private OnBluetoothPermissionsGranted onBluetoothPermissionsGranted;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_BLUETOOTH:
                case PERMISSION_BLUETOOTH_ADMIN:
                case PERMISSION_BLUETOOTH_CONNECT:
                case PERMISSION_BLUETOOTH_SCAN:
                case PERMISSION_LOCATION:
                    checkBluetoothPermissions(this.onBluetoothPermissionsGranted);
                    break;
            }
        }
    }

    public void checkBluetoothPermissions(OnBluetoothPermissionsGranted callback) {
        this.onBluetoothPermissionsGranted = callback;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, PERMISSION_BLUETOOTH);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, PERMISSION_BLUETOOTH_ADMIN);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_BLUETOOTH_CONNECT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_BLUETOOTH_SCAN);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
        } else {
            callback.onPermissionsGranted();
        }
    }

    // ==================== UTILITY ====================

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(usbReceiver);
        } catch (Exception ignored) {}

        if (bleScanner != null && bleScanner.isScanning()) {
            bleScanner.stopScan();
        }

        executor.execute(() -> {
            if (currentConnection != null) {
                try {
                    currentConnection.disconnect();
                } catch (Exception ignored) {}
            }
        });
        executor.shutdown();
    }
}
