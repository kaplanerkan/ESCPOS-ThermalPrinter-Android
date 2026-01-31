package com.dantsu.escposprinter.connection.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.IOException;

import timber.log.Timber;

public class UsbConnection extends DeviceConnection {

    private UsbManager usbManager;
    private UsbDevice usbDevice;

    /**
     * Create un instance of UsbConnection.
     *
     * @param usbManager an instance of UsbManager
     * @param usbDevice  an instance of UsbDevice
     */
    public UsbConnection(UsbManager usbManager, UsbDevice usbDevice) {
        super();
        this.usbManager = usbManager;
        this.usbDevice = usbDevice;
    }

    /**
     * Get the instance UsbDevice connected.
     *
     * @return an instance of UsbDevice
     */
    public UsbDevice getDevice() {
        return this.usbDevice;
    }

    /**
     * Start socket connection with the usbDevice.
     */
    public UsbConnection connect() throws EscPosConnectionException {
        if (this.isConnected()) {
            Timber.tag("UsbConnection").d("Already connected to USB device");
            return this;
        }

        Timber.tag("UsbConnection").i("Connecting to USB device: %s", this.usbDevice.getDeviceName());

        try {
            this.outputStream = new UsbOutputStream(this.usbManager, this.usbDevice);
            this.data = new byte[0];
            Timber.tag("UsbConnection").i("USB connected successfully: %s", this.usbDevice.getDeviceName());
        } catch (IOException e) {
            Timber.tag("UsbConnection").e(e, "USB connection failed: %s", e.getMessage());
            this.outputStream = null;
            throw new EscPosConnectionException("Unable to connect to USB device: " + e.getMessage());
        }
        return this;
    }

    /**
     * Close the socket connection with the usbDevice.
     */
    public UsbConnection disconnect() {
        Timber.tag("UsbConnection").d("Disconnecting USB device");
        this.data = new byte[0];
        if (this.isConnected()) {
            try {
                this.outputStream.close();
            } catch (IOException e) {
                Timber.tag("UsbConnection").e(e, "Error closing USB connection");
            }
            this.outputStream = null;
        }
        return this;
    }

    /**
     * Send data to the device.
     */
    @Override
    public void send() throws EscPosConnectionException {
        this.send(0);
    }

    /**
     * Send data to the device.
     */
    @Override
    public void send(int addWaitingTime) throws EscPosConnectionException {
        // In batch mode, only accumulate waiting time - don't actually send
        if (this.batchMode) {
            this.batchWaitingTime += addWaitingTime;
            Timber.tag("UsbConnection").v("Batch mode: buffering %d bytes", this.data.length);
            return;
        }

        if (this.data.length == 0) {
            return;
        }

        Timber.tag("UsbConnection").d("Sending %d bytes via USB", this.data.length);
        try {
            this.outputStream.write(this.data);
            int sentBytes = this.data.length;
            this.data = new byte[0];

            // Add waiting time if needed
            if (addWaitingTime > 0) {
                Thread.sleep(addWaitingTime);
            }

            Timber.tag("UsbConnection").d("USB send complete: %d bytes", sentBytes);
        } catch (IOException e) {
            Timber.tag("UsbConnection").e(e, "USB send failed: %s", e.getMessage());
            throw new EscPosConnectionException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Timber.tag("UsbConnection").e(e, "USB send interrupted");
            throw new EscPosConnectionException(e.getMessage());
        }
    }
}
