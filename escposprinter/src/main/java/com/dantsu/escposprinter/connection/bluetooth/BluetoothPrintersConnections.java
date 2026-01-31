package com.dantsu.escposprinter.connection.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.Nullable;

import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

public class BluetoothPrintersConnections extends BluetoothConnections {

    /**
     * Create a new instance of BluetoothPrintersConnections
     *
     * @deprecated Use {@link #BluetoothPrintersConnections(Context)} instead
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public BluetoothPrintersConnections() {
        super();
    }

    /**
     * Create a new instance of BluetoothPrintersConnections
     *
     * @param context the application context (used for BluetoothManager on Android 12+)
     */
    public BluetoothPrintersConnections(Context context) {
        super(context);
    }

    /**
     * Easy way to get the first bluetooth printer paired / connected.
     *
     * @return a BluetoothConnection instance
     * @deprecated Use {@link #selectFirstPaired(Context)} instead
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    @Nullable
    public static BluetoothConnection selectFirstPaired() {
        BluetoothPrintersConnections printers = new BluetoothPrintersConnections();
        BluetoothConnection[] bluetoothPrinters = printers.getList();

        if (bluetoothPrinters != null && bluetoothPrinters.length > 0) {
            for (BluetoothConnection printer : bluetoothPrinters) {
                try {
                    return printer.connect();
                } catch (EscPosConnectionException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Easy way to get the first bluetooth printer paired / connected.
     *
     * @param context the application context
     * @return a BluetoothConnection instance
     */
    @Nullable
    public static BluetoothConnection selectFirstPaired(Context context) {
        BluetoothPrintersConnections printers = new BluetoothPrintersConnections(context);
        BluetoothConnection[] bluetoothPrinters = printers.getList();

        if (bluetoothPrinters != null && bluetoothPrinters.length > 0) {
            for (BluetoothConnection printer : bluetoothPrinters) {
                try {
                    return printer.connect();
                } catch (EscPosConnectionException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Get a list of bluetooth printers.
     *
     * @return an array of BluetoothConnection
     */
    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    @Nullable
    public BluetoothConnection[] getList() {
        BluetoothConnection[] bluetoothDevicesList = super.getList();

        if (bluetoothDevicesList == null) {
            return null;
        }

        int i = 0;
        BluetoothConnection[] printersTmp = new BluetoothConnection[bluetoothDevicesList.length];
        Context context = contextRef != null ? contextRef.get() : null;

        for (BluetoothConnection bluetoothConnection : bluetoothDevicesList) {
            BluetoothDevice device = bluetoothConnection.getDevice();

            int majDeviceCl = device.getBluetoothClass().getMajorDeviceClass(),
                    deviceCl = device.getBluetoothClass().getDeviceClass();

            if (majDeviceCl == BluetoothClass.Device.Major.IMAGING && (deviceCl == 1664 || deviceCl == BluetoothClass.Device.Major.IMAGING)) {
                printersTmp[i++] = new BluetoothConnection(device, context);
            }
        }
        BluetoothConnection[] bluetoothPrinters = new BluetoothConnection[i];
        System.arraycopy(printersTmp, 0, bluetoothPrinters, 0, i);
        return bluetoothPrinters;
    }

}
