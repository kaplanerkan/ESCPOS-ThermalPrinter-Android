package com.dantsu.escposprinter.connection.usb;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;

import androidx.annotation.Nullable;

import timber.log.Timber;

public class UsbPrintersConnections extends UsbConnections {

    /**
     * Create a new instance of UsbPrintersConnections
     *
     * @param context Application context
     */
    public UsbPrintersConnections(Context context) {
        super(context);
    }

    /**
     * Easy way to get the first USB printer paired / connected.
     *
     * @return a UsbConnection instance
     */
    @Nullable
    public static UsbConnection selectFirstConnected(Context context) {
        Timber.i("Searching for first connected USB printer...");

        UsbPrintersConnections printers = new UsbPrintersConnections(context);
        UsbConnection[] usbPrinters = printers.getList();

        if (usbPrinters == null || usbPrinters.length == 0) {
            Timber.w("No USB printers found!");
            return null;
        }

        UsbDevice selectedDevice = usbPrinters[0].getDevice();
        Timber.i("Selected first printer: %s VID:%d PID:%d",
                selectedDevice.getDeviceName(),
                selectedDevice.getVendorId(),
                selectedDevice.getProductId());

        return usbPrinters[0];
    }
    
    
    /**
     * Get a list of USB printers.
     *
     * @return an array of UsbConnection
     */
    @Nullable
    public UsbConnection[] getList() {
        Timber.i("=== Scanning for USB Printers ===");

        UsbConnection[] usbConnections = super.getList();

        if(usbConnections == null) {
            Timber.w("No USB connections available");
            return null;
        }

        Timber.i("Found %d USB device(s) to check", usbConnections.length);

        int i = 0;
        UsbConnection[] printersTmp = new UsbConnection[usbConnections.length];
        for (UsbConnection usbConnection : usbConnections) {
            UsbDevice device = usbConnection.getDevice();
            int usbClass = device.getDeviceClass();
            String originalClass = getClassName(usbClass);

            Timber.i("Checking device: %s VID:%d PID:%d Class:%s",
                    device.getDeviceName(),
                    device.getVendorId(),
                    device.getProductId(),
                    originalClass);

            if((usbClass == UsbConstants.USB_CLASS_PER_INTERFACE || usbClass == UsbConstants.USB_CLASS_MISC ) && UsbDeviceHelper.findPrinterInterface(device) != null) {
                Timber.i("  -> Device has printer interface, treating as PRINTER");
                usbClass = UsbConstants.USB_CLASS_PRINTER;
            }

            if (usbClass == UsbConstants.USB_CLASS_PRINTER) {
                Timber.i("  -> ✓ Added as printer");
                printersTmp[i++] = new UsbConnection(this.usbManager, device);
            } else {
                Timber.i("  -> ✗ Not a printer (class: %s)", originalClass);
            }
        }

        Timber.i("=== Found %d USB printer(s) ===", i);

        UsbConnection[] usbPrinters = new UsbConnection[i];
        System.arraycopy(printersTmp, 0, usbPrinters, 0, i);
        return usbPrinters;
    }

    private String getClassName(int usbClass) {
        switch (usbClass) {
            case UsbConstants.USB_CLASS_PER_INTERFACE: return "PER_INTERFACE";
            case UsbConstants.USB_CLASS_HID: return "HID";
            case UsbConstants.USB_CLASS_PRINTER: return "PRINTER";
            case UsbConstants.USB_CLASS_MASS_STORAGE: return "STORAGE";
            case UsbConstants.USB_CLASS_VENDOR_SPEC: return "VENDOR_SPEC";
            case UsbConstants.USB_CLASS_MISC: return "MISC";
            default: return "CLASS_" + usbClass;
        }
    }
    
}
