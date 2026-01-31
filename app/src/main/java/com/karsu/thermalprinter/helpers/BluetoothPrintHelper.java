package com.karsu.thermalprinter.helpers;

/*
 * BluetoothPrintHelper.java
 *
 * Helper class for Bluetooth (SPP) printing operations.
 * Handles Bluetooth permissions, device discovery, and print execution.
 *
 * Features:
 * - Runtime permission handling for Bluetooth (Android 6.0+)
 * - Bluetooth device browsing and selection dialog
 * - Async print execution with callback support
 * - Support for Android 12+ new Bluetooth permissions
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.karsu.thermalprinter.async.AsyncBluetoothEscPosPrint;
import com.karsu.thermalprinter.async.AsyncEscPosPrint;
import com.karsu.thermalprinter.async.AsyncEscPosPrinter;

import timber.log.Timber;

public class BluetoothPrintHelper {

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    private final Activity activity;
    private BluetoothConnection selectedDevice;
    private OnBluetoothPermissionsGranted onPermissionsGranted;
    private OnDeviceSelected onDeviceSelected;

    public interface OnBluetoothPermissionsGranted {
        void onPermissionsGranted();
    }

    public interface OnDeviceSelected {
        void onDeviceSelected(String deviceName);
    }

    public interface OnPrintResult {
        void onSuccess();
        void onError(String message);
    }

    public BluetoothPrintHelper(Activity activity) {
        this.activity = activity;
    }

    public void setOnDeviceSelected(OnDeviceSelected listener) {
        this.onDeviceSelected = listener;
    }

    public BluetoothConnection getSelectedDevice() {
        return selectedDevice;
    }

    public void checkPermissions(OnBluetoothPermissionsGranted callback) {
        this.onPermissionsGranted = callback;

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH}, PERMISSION_BLUETOOTH);
        } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, PERMISSION_BLUETOOTH_ADMIN);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_BLUETOOTH_CONNECT);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN}, PERMISSION_BLUETOOTH_SCAN);
        } else {
            if (onPermissionsGranted != null) {
                onPermissionsGranted.onPermissionsGranted();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_BLUETOOTH:
                case PERMISSION_BLUETOOTH_ADMIN:
                case PERMISSION_BLUETOOTH_CONNECT:
                case PERMISSION_BLUETOOTH_SCAN:
                    checkPermissions(onPermissionsGranted);
                    break;
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void browseDevices() {
        checkPermissions(() -> {
            final BluetoothConnection[] devices = (new BluetoothPrintersConnections(activity)).getList();

            if (devices != null && devices.length > 0) {
                final String[] items = new String[devices.length + 1];
                items[0] = "Default printer";
                int i = 0;
                for (BluetoothConnection device : devices) {
                    String name = device.getDevice().getName();
                    items[++i] = name != null ? name : device.getDevice().getAddress();
                }

                new AlertDialog.Builder(activity)
                        .setTitle("Select Bluetooth Printer")
                        .setItems(items, (dialog, which) -> {
                            int index = which - 1;
                            selectedDevice = index == -1 ? null : devices[index];
                            if (onDeviceSelected != null) {
                                onDeviceSelected.onDeviceSelected(items[which]);
                            }
                        })
                        .setCancelable(false)
                        .show();
            } else {
                new AlertDialog.Builder(activity)
                        .setTitle("No Devices")
                        .setMessage("No paired Bluetooth printers found. Please pair your printer in Android settings first.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    public void print(AsyncEscPosPrinter printer, OnPrintResult result) {
        checkPermissions(() -> {
            new AsyncBluetoothEscPosPrint(activity, new AsyncEscPosPrint.OnPrintFinished() {
                @Override
                public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                    Timber.e("Print error: %d", codeException);
                    if (result != null) result.onError("Print failed: " + codeException);
                }

                @Override
                public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                    Timber.i("Print success");
                    if (result != null) result.onSuccess();
                }
            }).execute(printer);
        });
    }
}
