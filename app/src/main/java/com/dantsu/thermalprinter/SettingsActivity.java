package com.dantsu.thermalprinter;

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
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.tcp.TcpConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.thermalprinter.databinding.ActivitySettingsBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private DeviceConnection currentConnection;
    private EscPosPrinter printer;
    private BluetoothConnection selectedBluetoothDevice;
    private UsbConnection selectedUsbDevice;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Current text formatting state
    private String currentAlignment = "L";
    private String currentFontSize = "normal";
    private String currentFont = "";
    private boolean isBold = false;
    private boolean isUnderline = false;

    private static final String ACTION_USB_PERMISSION = "com.dantsu.thermalprinter.USB_PERMISSION";

    // Bluetooth permissions
    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupSpinners();
        setupSeekBars();
        setupConnectionSection();
        setupPrintTestsSection();
        setupTextFormatSection();
        setupBarcodeQrSection();
        setupStatusSection();
        setupRawCommandsSection();
    }

    private void setupSpinners() {
        // Cash Box Pin Spinner
        String[] cashBoxPins = {"Pin 2", "Pin 5"};
        ArrayAdapter<String> cashBoxAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, cashBoxPins);
        cashBoxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCashBoxPin.setAdapter(cashBoxAdapter);

        // Font Size Spinner
        String[] fontSizes = {"Normal", "Tall (2x height)", "Wide (2x width)", "Big (2x both)"};
        ArrayAdapter<String> fontSizeAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, fontSizes);
        fontSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFontSize.setAdapter(fontSizeAdapter);

        // Barcode Type Spinner
        String[] barcodeTypes = {"EAN13", "EAN8", "UPCA", "UPCE", "128", "39", "93", "ITF", "CODABAR"};
        ArrayAdapter<String> barcodeAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, barcodeTypes);
        barcodeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBarcodeType.setAdapter(barcodeAdapter);
    }

    private void setupSeekBars() {
        // Paper Cut Feed SeekBar
        binding.seekCutFeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.txtCutFeedValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Line Spacing SeekBar
        binding.seekLineSpacing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.txtLineSpacingValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Image Delay SeekBar
        binding.seekImageDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.txtImageDelayValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupConnectionSection() {
        // Connection type radio group
        binding.radioGroupConnection.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBluetooth) {
                binding.layoutBluetooth.setVisibility(android.view.View.VISIBLE);
                binding.layoutTcp.setVisibility(android.view.View.GONE);
            } else if (checkedId == R.id.radioUsb) {
                binding.layoutBluetooth.setVisibility(android.view.View.GONE);
                binding.layoutTcp.setVisibility(android.view.View.GONE);
            } else if (checkedId == R.id.radioTcp) {
                binding.layoutBluetooth.setVisibility(android.view.View.GONE);
                binding.layoutTcp.setVisibility(android.view.View.VISIBLE);
            }
        });

        // Bluetooth device selection
        binding.btnSelectBluetooth.setOnClickListener(v -> selectBluetoothDevice());

        // Connect button
        binding.btnConnect.setOnClickListener(v -> connectPrinter());
    }

    @SuppressLint("MissingPermission")
    private void selectBluetoothDevice() {
        checkBluetoothPermissions(() -> {
            BluetoothConnection[] devices = new BluetoothPrintersConnections().getList();
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

                if (binding.radioBluetooth.isChecked()) {
                    if (selectedBluetoothDevice == null) {
                        mainHandler.post(() -> showToast("Please select a Bluetooth device first"));
                        return;
                    }
                    currentConnection = selectedBluetoothDevice;
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

                // Apply settings
                applyPrinterSettings();

                mainHandler.post(() -> {
                    binding.txtConnectionStatus.setText("Status: Connected");
                    binding.txtConnectionStatus.setTextColor(0xFF4CAF50);
                    showToast("Printer connected successfully");
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    binding.txtConnectionStatus.setText("Status: Error - " + e.getMessage());
                    binding.txtConnectionStatus.setTextColor(0xFFFF5722);
                    showToast("Connection error: " + e.getMessage());
                });
            }
        });
    }

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
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
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
                                    applyPrinterSettings();

                                    mainHandler.post(() -> {
                                        binding.txtConnectionStatus.setText("Status: Connected (USB)");
                                        binding.txtConnectionStatus.setTextColor(0xFF4CAF50);
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

    private void applyPrinterSettings() throws EscPosConnectionException {
        if (printer == null) return;

        EscPosPrinterCommands commands = printer.getPrinterCommands();

        // Apply line spacing
        int lineSpacing = binding.seekLineSpacing.getProgress();
        commands.setLineSpacing(lineSpacing);

        // Apply image delay
        int imageDelay = binding.seekImageDelay.getProgress();
        commands.setImageProcessingDelay(imageDelay);

        // Apply ESC * mode
        commands.useEscAsteriskCommand(binding.chkUseEscAsterisk.isChecked());

        // Apply cash box setting
        commands.setCashBoxEnabled(binding.chkCashBoxEnabled.isChecked());
    }

    private void setupPrintTestsSection() {
        // Paper Cut buttons
        binding.btnPartialCut.setOnClickListener(v -> executePrinterCommand(() -> {
            if (printer != null) {
                int feed = binding.seekCutFeed.getProgress();
                printer.getPrinterCommands().cutPaper(feed);
            }
        }));

        binding.btnFullCut.setOnClickListener(v -> executePrinterCommand(() -> {
            if (printer != null) {
                int feed = binding.seekCutFeed.getProgress();
                printer.getPrinterCommands().feedPaper(feed);
                printer.getPrinterCommands().cutPaper();
            }
        }));

        // Cash Box button
        binding.btnOpenCashBox.setOnClickListener(v -> executePrinterCommand(() -> {
            if (printer != null) {
                // 0 = pin 2, 1 = pin 5
                int pin = binding.spinnerCashBoxPin.getSelectedItemPosition();
                printer.openCashBox(pin);
            }
        }));

        // Line Spacing Reset button
        binding.btnResetLineSpacing.setOnClickListener(v -> executePrinterCommand(() -> {
            if (printer != null) {
                printer.resetLineSpacing();
                mainHandler.post(() -> {
                    binding.seekLineSpacing.setProgress(30);
                    showToast("Line spacing reset to default");
                });
            }
        }));

        // Full Test Print button
        binding.btnFullTest.setOnClickListener(v -> executeFullTestPrint());
    }

    private void setupTextFormatSection() {
        // Alignment buttons
        binding.btnAlignLeft.setOnClickListener(v -> {
            currentAlignment = "L";
            showToast("Alignment: Left");
        });

        binding.btnAlignCenter.setOnClickListener(v -> {
            currentAlignment = "C";
            showToast("Alignment: Center");
        });

        binding.btnAlignRight.setOnClickListener(v -> {
            currentAlignment = "R";
            showToast("Alignment: Right");
        });

        // Font Size spinner listener
        binding.spinnerFontSize.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                switch (position) {
                    case 0: currentFontSize = "normal"; break;
                    case 1: currentFontSize = "tall"; break;
                    case 2: currentFontSize = "wide"; break;
                    case 3: currentFontSize = "big"; break;
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Font type buttons
        binding.btnFontA.setOnClickListener(v -> { currentFont = ""; showToast("Font: A (Default)"); });
        binding.btnFontB.setOnClickListener(v -> { currentFont = "font='b'"; showToast("Font: B"); });
        binding.btnFontC.setOnClickListener(v -> { currentFont = "font='c'"; showToast("Font: C"); });
        binding.btnFontD.setOnClickListener(v -> { currentFont = "font='d'"; showToast("Font: D"); });
        binding.btnFontE.setOnClickListener(v -> { currentFont = "font='e'"; showToast("Font: E"); });

        // Text style buttons
        binding.btnBold.setOnClickListener(v -> {
            isBold = !isBold;
            showToast("Bold: " + (isBold ? "ON" : "OFF"));
        });

        binding.btnUnderline.setOnClickListener(v -> {
            isUnderline = !isUnderline;
            showToast("Underline: " + (isUnderline ? "ON" : "OFF"));
        });

        binding.btnStrikethrough.setOnClickListener(v -> showToast("Strikethrough not directly supported"));

        // Print Custom Text button
        binding.btnPrintCustomText.setOnClickListener(v -> executePrinterCommand(() -> {
            if (printer == null) return;

            String text = binding.editCustomText.getText().toString();
            String formattedText = buildFormattedText(text);

            try {
                printer.printFormattedTextAndCut(formattedText);
            } catch (Exception e) {
                mainHandler.post(() -> showToast("Print error: " + e.getMessage()));
            }
        }));
    }

    private String buildFormattedText(String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(currentAlignment).append("]");

        // Start font tag if needed
        boolean hasFontTag = !currentFontSize.equals("normal") || !currentFont.isEmpty();
        if (hasFontTag) {
            sb.append("<font");
            if (!currentFontSize.equals("normal")) {
                sb.append(" size='").append(currentFontSize).append("'");
            }
            if (!currentFont.isEmpty()) {
                sb.append(" ").append(currentFont);
            }
            sb.append(">");
        }

        // Add text with styles
        if (isBold) sb.append("<b>");
        if (isUnderline) sb.append("<u>");

        sb.append(text);

        if (isUnderline) sb.append("</u>");
        if (isBold) sb.append("</b>");

        if (hasFontTag) {
            sb.append("</font>");
        }

        sb.append("\n");
        return sb.toString();
    }

    private void setupBarcodeQrSection() {
        // Print Barcode button
        binding.btnPrintBarcode.setOnClickListener(v -> executePrinterCommand(() -> {
            if (printer == null) return;

            String data = binding.editBarcodeData.getText().toString();
            String type = ((String) binding.spinnerBarcodeType.getSelectedItem()).toLowerCase();

            try {
                String barcodeText = "[C]<barcode type='" + type + "' height='10'>" + data + "</barcode>\n";
                printer.printFormattedTextAndCut(barcodeText);
            } catch (Exception e) {
                mainHandler.post(() -> showToast("Barcode error: " + e.getMessage()));
            }
        }));

        // Print QR Code button
        binding.btnPrintQr.setOnClickListener(v -> executePrinterCommand(() -> {
            if (printer == null) return;

            String data = binding.editQrData.getText().toString();
            String size = binding.editQrSize.getText().toString();

            try {
                String qrText = "[C]<qrcode size='" + size + "'>" + data + "</qrcode>\n";
                printer.printFormattedTextAndCut(qrText);
            } catch (Exception e) {
                mainHandler.post(() -> showToast("QR code error: " + e.getMessage()));
            }
        }));
    }

    private void setupStatusSection() {
        binding.btnQueryStatus.setOnClickListener(v -> executePrinterCommand(() -> {
            if (printer == null) {
                mainHandler.post(() -> binding.txtPrinterStatus.setText("Status: Not connected"));
                return;
            }

            try {
                com.dantsu.escposprinter.PrinterStatus status = printer.queryStatus();
                if (status != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Online: ").append(status.isOnline()).append("\n");
                    sb.append("Paper End: ").append(status.isPaperEnd()).append("\n");
                    sb.append("Paper Near End: ").append(status.isPaperNearEnd()).append("\n");
                    sb.append("Cover Open: ").append(status.isCoverOpen()).append("\n");
                    sb.append("Drawer Open: ").append(status.isDrawerOpen()).append("\n");
                    sb.append("Error: ").append(status.isErrorOccurred()).append("\n");
                    sb.append("Ready: ").append(status.isReady());

                    mainHandler.post(() -> binding.txtPrinterStatus.setText(sb.toString()));
                } else {
                    mainHandler.post(() -> binding.txtPrinterStatus.setText("Status: No response from printer"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> binding.txtPrinterStatus.setText("Status: Error - " + e.getMessage()));
            }
        }));
    }

    private void setupRawCommandsSection() {
        binding.btnSendRaw.setOnClickListener(v -> executePrinterCommand(() -> {
            if (printer == null) return;

            String hexString = binding.editRawHex.getText().toString().trim();
            byte[] bytes = hexStringToBytes(hexString);

            if (bytes != null) {
                try {
                    printer.getPrinterCommands().printRaw(bytes);
                    mainHandler.post(() -> showToast("Raw command sent"));
                } catch (Exception e) {
                    mainHandler.post(() -> showToast("Error: " + e.getMessage()));
                }
            } else {
                mainHandler.post(() -> showToast("Invalid hex string"));
            }
        }));
    }

    private byte[] hexStringToBytes(String hexString) {
        try {
            String[] hexParts = hexString.split("\\s+");
            byte[] bytes = new byte[hexParts.length];
            for (int i = 0; i < hexParts.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hexParts[i], 16);
            }
            return bytes;
        } catch (Exception e) {
            return null;
        }
    }

    private void executeFullTestPrint() {
        executePrinterCommand(() -> {
            if (printer == null) {
                mainHandler.post(() -> showToast("Printer not connected"));
                return;
            }

            try {
                String testPrint =
                    "[C]<font size='big'><b>FULL TEST PRINT</b></font>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]<b>Left Aligned Text</b>\n" +
                    "[C]<b>Center Aligned Text</b>\n" +
                    "[R]<b>Right Aligned Text</b>\n" +
                    "[L]\n" +
                    "[C]--- Font Sizes ---\n" +
                    "[L]Normal text\n" +
                    "[L]<font size='tall'>Tall text</font>\n" +
                    "[L]<font size='wide'>Wide text</font>\n" +
                    "[L]<font size='big'>Big text</font>\n" +
                    "[L]\n" +
                    "[C]--- Text Styles ---\n" +
                    "[L]<b>Bold text</b>\n" +
                    "[L]<u>Underlined text</u>\n" +
                    "[L]<b><u>Bold + Underline</u></b>\n" +
                    "[L]\n" +
                    "[C]--- Special Characters ---\n" +
                    "[L]Less than: < (fixed!)\n" +
                    "[L]Greater than: >\n" +
                    "[L]Price: 19.99 EUR\n" +
                    "[L]\n" +
                    "[C]--- Barcode ---\n" +
                    "[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
                    "[L]\n" +
                    "[C]--- QR Code ---\n" +
                    "[C]<qrcode size='20'>https://github.com/kaplanerkan/ESCPOS-ThermalPrinter-Android</qrcode>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[C]Test completed!\n" +
                    "[L]\n";

                printer.printFormattedTextAndCut(testPrint);
                mainHandler.post(() -> showToast("Full test print completed"));
            } catch (Exception e) {
                mainHandler.post(() -> showToast("Print error: " + e.getMessage()));
            }
        });
    }

    private void executePrinterCommand(Runnable command) {
        if (printer == null) {
            showToast("Printer not connected");
            return;
        }
        executor.execute(command);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Bluetooth permissions handling
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
        } else {
            callback.onPermissionsGranted();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(usbReceiver);
        } catch (Exception ignored) {}

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
