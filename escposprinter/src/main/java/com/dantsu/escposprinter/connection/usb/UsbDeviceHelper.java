package com.dantsu.escposprinter.connection.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import androidx.annotation.Nullable;

public class UsbDeviceHelper {
    /**
     * Find the correct USB interface for printing.
     * First looks for USB_CLASS_PRINTER interfaces, then falls back to
     * any interface with a bulk OUT endpoint.
     *
     * @param usbDevice USB device
     * @return correct USB interface for printing, null if not found
     */
    @Nullable
    static public UsbInterface findPrinterInterface(UsbDevice usbDevice) {
        if (usbDevice == null) {
            return null;
        }

        int interfacesCount = usbDevice.getInterfaceCount();

        // First pass: look for standard printer interface class
        for (int i = 0; i < interfacesCount; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
                // Verify it has a bulk OUT endpoint
                if (findEndpointOut(usbInterface) != null) {
                    return usbInterface;
                }
            }
        }

        // Second pass: look for vendor-specific interface with bulk OUT endpoint
        for (int i = 0; i < interfacesCount; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC) {
                if (findEndpointOut(usbInterface) != null) {
                    return usbInterface;
                }
            }
        }

        // Third pass: look for any interface with a bulk OUT endpoint
        for (int i = 0; i < interfacesCount; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (findEndpointOut(usbInterface) != null) {
                return usbInterface;
            }
        }

        return null;
    }

    /**
     * Find the USB endpoint for device output (sending data to printer).
     *
     * @param usbInterface USB interface
     * @return Output endpoint or null if not found
     */
    @Nullable
    static public UsbEndpoint findEndpointOut(UsbInterface usbInterface) {
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

    /**
     * Find the USB endpoint for device input (reading data from printer).
     *
     * @param usbInterface USB interface
     * @return Input endpoint or null if not found
     */
    @Nullable
    static public UsbEndpoint findEndpointIn(UsbInterface usbInterface) {
        // For backwards compatibility, this calls findEndpointOut
        // The name was misleading - "In" refers to "into the printer"
        return findEndpointOut(usbInterface);
    }

    /**
     * Find the USB endpoint for reading from printer (for status queries).
     *
     * @param usbInterface USB interface
     * @return Input endpoint for reading or null if not found
     */
    @Nullable
    static public UsbEndpoint findEndpointRead(UsbInterface usbInterface) {
        if (usbInterface != null) {
            int endpointsCount = usbInterface.getEndpointCount();
            for (int i = 0; i < endpointsCount; i++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(i);
                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                    return endpoint;
                }
            }
        }
        return null;
    }
}
