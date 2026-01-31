package com.dantsu.escposprinter.connection.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import timber.log.Timber;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Scanner for BLE (Bluetooth Low Energy) printers.
 */
public class BleDeviceScanner {

    private static final String TAG = "BleDeviceScanner";

    // Common BLE printer service UUIDs for filtering
    private static final UUID[] PRINTER_SERVICE_UUIDS = {
            UUID.fromString("0000FF00-0000-1000-8000-00805F9B34FB"),
            UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455"),
            UUID.fromString("E7810A71-73AE-499D-8C15-FAA9AEF0C3F2"),
            UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
            UUID.fromString("000018F0-0000-1000-8000-00805F9B34FB"),
    };

    private final WeakReference<Context> contextRef;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Set<String> discoveredAddresses = new HashSet<>();
    private final List<BluetoothLeConnection> discoveredDevices = new ArrayList<>();

    private boolean isScanning = false;
    private OnBleScanListener scanListener;

    /**
     * Callback interface for BLE scan results.
     */
    public interface OnBleScanListener {
        void onDeviceFound(BluetoothLeConnection device);
        void onScanComplete(List<BluetoothLeConnection> devices);
        void onScanFailed(int errorCode);
    }

    /**
     * Create a new BLE scanner.
     *
     * @param context the application context
     */
    @SuppressLint("MissingPermission")
    public BleDeviceScanner(Context context) {
        this.contextRef = new WeakReference<>(context.getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        } else {
            @SuppressWarnings("deprecation")
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            this.bluetoothAdapter = adapter;
        }
    }

    /**
     * Check if BLE is supported and enabled.
     */
    @SuppressLint("MissingPermission")
    public boolean isBluetoothLeSupported() {
        Context context = contextRef.get();
        if (context == null) return false;

        if (!context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            return false;
        }

        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Start scanning for BLE printers.
     *
     * @param listener callback for scan results
     * @param scanDurationMs how long to scan in milliseconds
     */
    @SuppressLint("MissingPermission")
    public void startScan(OnBleScanListener listener, long scanDurationMs) {
        if (!isBluetoothLeSupported()) {
            if (listener != null) {
                listener.onScanFailed(-1);
            }
            return;
        }

        if (isScanning) {
            stopScan();
        }

        this.scanListener = listener;
        discoveredAddresses.clear();
        discoveredDevices.clear();

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            if (listener != null) {
                listener.onScanFailed(-2);
            }
            return;
        }

        // Build scan settings
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        }

        ScanSettings settings = settingsBuilder.build();

        // Build scan filters (optional - can scan for specific service UUIDs)
        List<ScanFilter> filters = new ArrayList<>();
        // Uncomment to filter by service UUIDs:
        // for (UUID uuid : PRINTER_SERVICE_UUIDS) {
        //     filters.add(new ScanFilter.Builder()
        //             .setServiceUuid(new ParcelUuid(uuid))
        //             .build());
        // }

        isScanning = true;
        Timber.tag(TAG).i( "Starting BLE scan for " + scanDurationMs + "ms");

        try {
            scanner.startScan(filters.isEmpty() ? null : filters, settings, scanCallback);
        } catch (Exception e) {
            Timber.tag(TAG).e( "Failed to start scan", e);
            isScanning = false;
            if (listener != null) {
                listener.onScanFailed(-3);
            }
            return;
        }

        // Stop scan after duration
        handler.postDelayed(this::stopScan, scanDurationMs);
    }

    /**
     * Stop the current scan.
     */
    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (!isScanning) return;

        isScanning = false;
        handler.removeCallbacksAndMessages(null);

        if (scanner != null) {
            try {
                scanner.stopScan(scanCallback);
            } catch (Exception e) {
                Timber.tag(TAG).e( "Failed to stop scan", e);
            }
        }

        Timber.tag(TAG).i( "BLE scan stopped. Found " + discoveredDevices.size() + " devices");

        if (scanListener != null) {
            scanListener.onScanComplete(new ArrayList<>(discoveredDevices));
        }
    }

    /**
     * Check if currently scanning.
     */
    public boolean isScanning() {
        return isScanning;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device == null) return;

            String address = device.getAddress();
            if (discoveredAddresses.contains(address)) {
                return;
            }

            // Filter: only add devices with a name (likely to be printers)
            String name = device.getName();
            if (name == null || name.isEmpty()) {
                // Some BLE devices don't advertise their name, include them anyway
                name = "Unknown BLE Device";
            }

            Timber.tag(TAG).d( "Found BLE device: " + name + " [" + address + "], RSSI: " + result.getRssi());

            discoveredAddresses.add(address);

            Context context = contextRef.get();
            if (context != null) {
                BluetoothLeConnection connection = new BluetoothLeConnection(device, context);
                discoveredDevices.add(connection);

                if (scanListener != null) {
                    handler.post(() -> scanListener.onDeviceFound(connection));
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Timber.tag(TAG).e( "BLE scan failed with error code: " + errorCode);
            isScanning = false;
            if (scanListener != null) {
                scanListener.onScanFailed(errorCode);
            }
        }
    };

    /**
     * Get list of discovered devices (after scan complete).
     */
    public List<BluetoothLeConnection> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }
}
