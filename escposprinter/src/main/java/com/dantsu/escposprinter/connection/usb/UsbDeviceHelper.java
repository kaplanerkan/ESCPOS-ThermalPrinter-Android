package com.dantsu.escposprinter.connection.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import androidx.annotation.Nullable;

import timber.log.Timber;

public class UsbDeviceHelper {

    /**
     * Get USB class name for logging.
     */
    private static String getUsbClassName(int usbClass) {
        switch (usbClass) {
            case UsbConstants.USB_CLASS_PER_INTERFACE: return "PER_INTERFACE(0)";
            case UsbConstants.USB_CLASS_AUDIO: return "AUDIO(1)";
            case UsbConstants.USB_CLASS_COMM: return "COMM(2)";
            case UsbConstants.USB_CLASS_HID: return "HID(3)";
            case UsbConstants.USB_CLASS_PHYSICA: return "PHYSICAL(5)";
            case UsbConstants.USB_CLASS_STILL_IMAGE: return "IMAGE(6)";
            case UsbConstants.USB_CLASS_PRINTER: return "PRINTER(7)";
            case UsbConstants.USB_CLASS_MASS_STORAGE: return "STORAGE(8)";
            case UsbConstants.USB_CLASS_HUB: return "HUB(9)";
            case UsbConstants.USB_CLASS_CDC_DATA: return "CDC_DATA(10)";
            case UsbConstants.USB_CLASS_CSCID: return "SMARTCARD(11)";
            case UsbConstants.USB_CLASS_CONTENT_SEC: return "CONTENT_SEC(13)";
            case UsbConstants.USB_CLASS_VIDEO: return "VIDEO(14)";
            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER: return "WIRELESS(224)";
            case UsbConstants.USB_CLASS_MISC: return "MISC(239)";
            case UsbConstants.USB_CLASS_APP_SPEC: return "APP_SPEC(254)";
            case UsbConstants.USB_CLASS_VENDOR_SPEC: return "VENDOR_SPEC(255)";
            default: return "UNKNOWN(" + usbClass + ")";
        }
    }

    /**
     * Find the correct USB interface for printing.
     * Looks for USB_CLASS_PRINTER interface first, then falls back to vendor-specific.
     *
     * @param usbDevice USB device
     * @return correct USB interface for printing, null if not found
     */
    @Nullable
    static public UsbInterface findPrinterInterface(UsbDevice usbDevice) {
        if (usbDevice == null) {
            Timber.e("USB device is null");
            return null;
        }

        int interfacesCount = usbDevice.getInterfaceCount();
        String productName = usbDevice.getProductName();
        Timber.i("=== USB Device Analysis ===");
        Timber.i("Device: %s", usbDevice.getDeviceName());
        Timber.i("Name: %s", productName != null ? productName : "Unknown");
        Timber.i("VID: %d (0x%04X) PID: %d (0x%04X)",
                usbDevice.getVendorId(), usbDevice.getVendorId(),
                usbDevice.getProductId(), usbDevice.getProductId());
        Timber.i("Device Class: %s", getUsbClassName(usbDevice.getDeviceClass()));
        Timber.i("Interfaces: %d", interfacesCount);

        // Log all interfaces for debugging
        for (int i = 0; i < interfacesCount; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            Timber.i("  Interface %d: class=%s subclass=%d protocol=%d endpoints=%d",
                    i,
                    getUsbClassName(usbInterface.getInterfaceClass()),
                    usbInterface.getInterfaceSubclass(),
                    usbInterface.getInterfaceProtocol(),
                    usbInterface.getEndpointCount());
        }

        // First pass: look for standard printer interface class (7)
        for (int i = 0; i < interfacesCount; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                Timber.i("✓ Found USB_CLASS_PRINTER interface at index %d", i);
                return usbInterface;
            }
        }

        // Second pass: look for vendor-specific interface (class 255)
        for (int i = 0; i < interfacesCount; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC) {
                Timber.i("✓ Found USB_CLASS_VENDOR_SPEC interface at index %d", i);
                return usbInterface;
            }
        }

        // Third pass: try any interface with bulk OUT endpoint
        for (int i = 0; i < interfacesCount; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (findEndpointIn(usbInterface) != null) {
                Timber.i("✓ Found interface with bulk OUT endpoint at index %d", i);
                return usbInterface;
            }
        }

        Timber.w("✗ No suitable USB printer interface found for this device!");
        return null;
    }

    /**
     * Find the USB endpoint for device input (sending data to printer).
     *
     * @param usbInterface USB interface
     * @return Input endpoint or null if not found
     */
    @Nullable
    static public UsbEndpoint findEndpointIn(UsbInterface usbInterface) {
        if (usbInterface != null) {
            int endpointsCount = usbInterface.getEndpointCount();
            for (int i = 0; i < endpointsCount; i++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(i);
                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                    return endpoint;
                }
            }
        }
        return null;
    }
}
