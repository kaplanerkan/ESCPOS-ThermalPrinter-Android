package com.karsu.thermalprinter.helpers;

/*
 * UsbPrintHelper.java
 *
 * Helper class for USB printing operations.
 * Handles USB device detection, permission requests, and print execution.
 *
 * Features:
 * - Automatic USB printer detection with VID/PID display
 * - USB permission request handling (Android broadcast receiver)
 * - Support for specific VID/PID targeting or auto-detection
 * - Real-time USB connection status updates
 * - Compatible with Android 13+ (Tiramisu) USB APIs
 *
 * @author Erkan Kaplan
 * @date 2026-01-31
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;

import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.karsu.thermalprinter.async.AsyncEscPosPrint;
import com.karsu.thermalprinter.async.AsyncEscPosPrinter;
import com.karsu.thermalprinter.async.AsyncUsbEscPosPrint;

import java.util.HashMap;

import timber.log.Timber;

public class UsbPrintHelper {

    private static final String ACTION_USB_PERMISSION = "com.karsu.thermalprinter.USB_PERMISSION";

    private final Activity activity;
    private OnUsbStatusUpdate onUsbStatusUpdate;
    private OnPrintResult pendingPrintResult;
    private AsyncEscPosPrinter pendingPrinter;

    public interface OnUsbStatusUpdate {
        void onStatusUpdate(String status, Integer vendorId, Integer productId);
    }

    public interface OnPrintResult {
        void onSuccess();
        void onError(String message);
    }

    private boolean receiverRegistered = false;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                // Unregister receiver after receiving permission result
                unregisterUsbReceiver();

                synchronized (this) {
                    UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
                    UsbDevice usbDevice;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
                    } else {
                        @SuppressWarnings("deprecation")
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        usbDevice = device;
                    }

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null && pendingPrinter != null) {
                            executePrint(new UsbConnection(usbManager, usbDevice));
                        }
                    } else {
                        if (pendingPrintResult != null) {
                            pendingPrintResult.onError("USB permission denied");
                        }
                    }
                }
            }
        }
    };

    public UsbPrintHelper(Activity activity) {
        this.activity = activity;
    }

    public void setOnUsbStatusUpdate(OnUsbStatusUpdate listener) {
        this.onUsbStatusUpdate = listener;
    }

    @SuppressLint("SetTextI18n")
    public void updateUsbStatus() {
        UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        if (usbManager != null) {
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

            Timber.i("========== USB DEVICE SCAN ==========");
            Timber.i("Total USB devices connected: %d", deviceList.size());

            if (deviceList.isEmpty()) {
                Timber.w("No USB devices detected!");
                if (onUsbStatusUpdate != null) {
                    onUsbStatusUpdate.onStatusUpdate("No USB devices detected", null, null);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Found ").append(deviceList.size()).append(" device(s):\n");
                Integer firstPrinterVid = null;
                Integer firstPrinterPid = null;

                for (UsbDevice device : deviceList.values()) {
                    String name = device.getProductName();
                    int deviceClass = device.getDeviceClass();
                    String className = getUsbClassName(deviceClass);

                    Timber.i("---");
                    Timber.i("Device: %s", device.getDeviceName());
                    Timber.i("  Name: %s", name != null ? name : "Unknown");
                    Timber.i("  VID: %d (0x%04X) PID: %d (0x%04X)",
                            device.getVendorId(), device.getVendorId(),
                            device.getProductId(), device.getProductId());
                    Timber.i("  Class: %s", className);
                    Timber.i("  Interfaces: %d", device.getInterfaceCount());

                    // Log interface details
                    for (int i = 0; i < device.getInterfaceCount(); i++) {
                        android.hardware.usb.UsbInterface iface = device.getInterface(i);
                        Timber.i("    Interface %d: class=%s endpoints=%d",
                                i, getUsbClassName(iface.getInterfaceClass()), iface.getEndpointCount());
                    }

                    // Check if this could be a printer
                    boolean isPrinter = isPotentialPrinter(device);
                    Timber.i("  Is Printer: %s", isPrinter ? "YES" : "NO");

                    if (isPrinter && firstPrinterVid == null) {
                        firstPrinterVid = device.getVendorId();
                        firstPrinterPid = device.getProductId();
                    }

                    sb.append(String.format("VID:%04X PID:%04X [%s]",
                            device.getVendorId(), device.getProductId(), className));
                    if (name != null && !name.isEmpty()) {
                        sb.append(" ").append(name);
                    }
                    if (isPrinter) {
                        sb.append(" ✓");
                    }
                    sb.append("\n");
                }

                Timber.i("========== END USB SCAN ==========");

                if (onUsbStatusUpdate != null) {
                    onUsbStatusUpdate.onStatusUpdate(sb.toString().trim(), firstPrinterVid, firstPrinterPid);
                }
            }
        } else {
            Timber.e("USB Manager not available!");
            if (onUsbStatusUpdate != null) {
                onUsbStatusUpdate.onStatusUpdate("USB not available", null, null);
            }
        }
    }

    /**
     * Check if a USB device could potentially be a printer.
     */
    private boolean isPotentialPrinter(UsbDevice device) {
        int deviceClass = device.getDeviceClass();

        // Direct printer class
        if (deviceClass == android.hardware.usb.UsbConstants.USB_CLASS_PRINTER) {
            return true;
        }

        // Check interfaces for printer class or vendor-specific with bulk out
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            android.hardware.usb.UsbInterface iface = device.getInterface(i);
            int ifaceClass = iface.getInterfaceClass();

            if (ifaceClass == android.hardware.usb.UsbConstants.USB_CLASS_PRINTER) {
                return true;
            }
            if (ifaceClass == android.hardware.usb.UsbConstants.USB_CLASS_VENDOR_SPEC) {
                // Vendor-specific might be a printer
                return true;
            }
        }
        return false;
    }

    /**
     * Get human-readable USB class name.
     */
    private String getUsbClassName(int usbClass) {
        switch (usbClass) {
            case android.hardware.usb.UsbConstants.USB_CLASS_PER_INTERFACE: return "PER_INTERFACE";
            case android.hardware.usb.UsbConstants.USB_CLASS_AUDIO: return "AUDIO";
            case android.hardware.usb.UsbConstants.USB_CLASS_COMM: return "COMM";
            case android.hardware.usb.UsbConstants.USB_CLASS_HID: return "HID";
            case android.hardware.usb.UsbConstants.USB_CLASS_PHYSICA: return "PHYSICAL";
            case android.hardware.usb.UsbConstants.USB_CLASS_STILL_IMAGE: return "IMAGE";
            case android.hardware.usb.UsbConstants.USB_CLASS_PRINTER: return "PRINTER";
            case android.hardware.usb.UsbConstants.USB_CLASS_MASS_STORAGE: return "STORAGE";
            case android.hardware.usb.UsbConstants.USB_CLASS_HUB: return "HUB";
            case android.hardware.usb.UsbConstants.USB_CLASS_CDC_DATA: return "CDC_DATA";
            case android.hardware.usb.UsbConstants.USB_CLASS_CSCID: return "SMARTCARD";
            case android.hardware.usb.UsbConstants.USB_CLASS_CONTENT_SEC: return "CONTENT_SEC";
            case android.hardware.usb.UsbConstants.USB_CLASS_VIDEO: return "VIDEO";
            case android.hardware.usb.UsbConstants.USB_CLASS_WIRELESS_CONTROLLER: return "WIRELESS";
            case android.hardware.usb.UsbConstants.USB_CLASS_MISC: return "MISC";
            case android.hardware.usb.UsbConstants.USB_CLASS_APP_SPEC: return "APP_SPEC";
            case android.hardware.usb.UsbConstants.USB_CLASS_VENDOR_SPEC: return "VENDOR_SPEC";
            default: return "UNKNOWN(" + usbClass + ")";
        }
    }

    public void print(AsyncEscPosPrinter printer, Integer vendorId, Integer productId, OnPrintResult result) {
        this.pendingPrinter = printer;
        this.pendingPrintResult = result;

        Timber.i("========== USB PRINT REQUEST ==========");
        Timber.i("VID: %s, PID: %s", vendorId != null ? vendorId : "auto", productId != null ? productId : "auto");

        UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            Timber.e("USB Manager not available!");
            result.onError("USB Manager not available");
            return;
        }

        // Log all connected devices first
        HashMap<String, UsbDevice> allDevices = usbManager.getDeviceList();
        Timber.i("Total USB devices available: %d", allDevices.size());
        for (UsbDevice device : allDevices.values()) {
            Timber.i("  - %s VID:%d PID:%d Class:%s",
                    device.getDeviceName(),
                    device.getVendorId(),
                    device.getProductId(),
                    getUsbClassName(device.getDeviceClass()));
        }

        UsbConnection usbConnection = null;

        if (vendorId != null && productId != null) {
            Timber.i("Searching for specific device VID:%d PID:%d...", vendorId, productId);
            usbConnection = findUsbDevice(usbManager, vendorId, productId);
            if (usbConnection == null) {
                Timber.e("Device not found with VID:%d PID:%d", vendorId, productId);
                result.onError(String.format("No USB device found with VID:%d PID:%d", vendorId, productId));
                return;
            }
            Timber.i("Found specified device!");
        } else {
            Timber.i("Auto-detecting USB printer...");
            usbConnection = UsbPrintersConnections.selectFirstConnected(activity);
        }

        if (usbConnection == null) {
            Timber.e("No USB printer found after auto-detection!");
            Timber.w("Hint: The connected USB devices may not be printers. Check device classes above.");
            result.onError("No USB printer found. Please connect a printer.");
            return;
        }

        UsbDevice selectedDevice = usbConnection.getDevice();
        Timber.i("Selected printer: %s VID:%d PID:%d",
                selectedDevice.getDeviceName(),
                selectedDevice.getVendorId(),
                selectedDevice.getProductId());

        requestPermissionAndPrint(usbManager, usbConnection);
    }

    private UsbConnection findUsbDevice(UsbManager usbManager, int vendorId, int productId) {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                return new UsbConnection(usbManager, device);
            }
        }
        return null;
    }

    private void requestPermissionAndPrint(UsbManager usbManager, UsbConnection usbConnection) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                activity,
                0,
                new Intent(ACTION_USB_PERMISSION),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            activity.registerReceiver(usbReceiver, filter);
        }
        receiverRegistered = true;

        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
    }

    private void unregisterUsbReceiver() {
        if (receiverRegistered) {
            try {
                activity.unregisterReceiver(usbReceiver);
            } catch (Exception ignored) {
            }
            receiverRegistered = false;
        }
    }

    /**
     * Clean up resources. Call this when the helper is no longer needed.
     */
    public void cleanup() {
        unregisterUsbReceiver();
    }

    private void executePrint(UsbConnection usbConnection) {
        new AsyncUsbEscPosPrint(activity, new AsyncEscPosPrint.OnPrintFinished() {
            @Override
            public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                Timber.e("Print error: %d", codeException);
                if (pendingPrintResult != null) {
                    pendingPrintResult.onError("Print failed: " + codeException);
                }
            }

            @Override
            public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                Timber.i("Print success");
                if (pendingPrintResult != null) {
                    pendingPrintResult.onSuccess();
                }
            }
        }).execute(pendingPrinter.setConnection(usbConnection));
    }

    public void showError(String message) {
        new AlertDialog.Builder(activity)
                .setTitle("USB Connection")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
