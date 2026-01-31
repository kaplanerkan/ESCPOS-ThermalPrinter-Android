package com.dantsu.escposprinter.connection.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.UUID;

import timber.log.Timber;

public class BluetoothConnection extends DeviceConnection {

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private BluetoothDevice device;
    private BluetoothSocket socket = null;
    private WeakReference<Context> contextRef;

    /**
     * Create an instance of BluetoothConnection.
     *
     * @param device an instance of BluetoothDevice
     * @deprecated Use {@link #BluetoothConnection(BluetoothDevice, Context)} instead
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public BluetoothConnection(BluetoothDevice device) {
        this(device, null);
    }

    /**
     * Create an instance of BluetoothConnection.
     *
     * @param device  an instance of BluetoothDevice
     * @param context the application context (used for BluetoothManager on Android 12+)
     */
    public BluetoothConnection(BluetoothDevice device, Context context) {
        super();
        this.device = device;
        this.contextRef = context != null ? new WeakReference<>(context.getApplicationContext()) : null;
        // Bluetooth-optimized settings for better reliability
        this.chunkSize = 200;      // Smaller chunks for Bluetooth
        this.chunkDelayMs = 20;    // Longer delay between chunks
        this.bytesPerMs = 8;       // Slower byte rate assumption
    }

    /**
     * Get the instance BluetoothDevice connected.
     *
     * @return an instance of BluetoothDevice
     */
    public BluetoothDevice getDevice() {
        return this.device;
    }

    /**
     * Check if OutputStream is open.
     *
     * @return true if is connected
     */
    @Override
    public boolean isConnected() {
        return this.socket != null && this.socket.isConnected() && super.isConnected();
    }

    /**
     * Get BluetoothAdapter using the modern API when possible.
     */
    @SuppressLint("MissingPermission")
    private BluetoothAdapter getBluetoothAdapter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && contextRef != null) {
            Context context = contextRef.get();
            if (context != null) {
                BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager != null) {
                    return bluetoothManager.getAdapter();
                }
            }
        }
        // Fallback for older Android versions or when context is not available
        @SuppressWarnings("deprecation")
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter;
    }

    /**
     * Start socket connection with the bluetooth device.
     */
    @SuppressLint("MissingPermission")
    public BluetoothConnection connect() throws EscPosConnectionException {
        if (this.isConnected()) {
            Timber.tag("BluetoothConnection").d("Already connected to Bluetooth device");
            return this;
        }

        if (this.device == null) {
            Timber.tag("BluetoothConnection").e("Bluetooth device is null");
            throw new EscPosConnectionException("Bluetooth device is not connected.");
        }

        String deviceName = this.device.getName() != null ? this.device.getName() : this.device.getAddress();
        Timber.tag("BluetoothConnection").i("Connecting to Bluetooth SPP device: %s", deviceName);

        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        UUID uuid = this.getDeviceUUID();
        Timber.tag("BluetoothConnection").d("Using UUID: %s", uuid.toString());

        try {
            this.socket = this.device.createRfcommSocketToServiceRecord(uuid);
            bluetoothAdapter.cancelDiscovery();
            this.socket.connect();
            this.outputStream = this.socket.getOutputStream();
            this.inputStream = this.socket.getInputStream();
            this.data = new byte[0];
            Timber.tag("BluetoothConnection").i("Bluetooth SPP connected successfully: %s", deviceName);
        } catch (IOException e) {
            Timber.tag("BluetoothConnection").e(e, "Bluetooth connection failed: %s - %s", deviceName, e.getMessage());
            this.disconnect();
            throw new EscPosConnectionException("Unable to connect to bluetooth device: " + e.getMessage());
        }
        return this;
    }

    /**
     * Get bluetooth device UUID
     */
    protected UUID getDeviceUUID() {
        ParcelUuid[] uuids = device.getUuids();
        if (uuids != null && uuids.length > 0) {
            if (Arrays.asList(uuids).contains(new ParcelUuid(BluetoothConnection.SPP_UUID))) {
                return BluetoothConnection.SPP_UUID;
            }
            return uuids[0].getUuid();
        } else {
            return BluetoothConnection.SPP_UUID;
        }
    }

    /**
     * Close the socket connection with the bluetooth device.
     */
    public BluetoothConnection disconnect() {
        Timber.tag("BluetoothConnection").d("Disconnecting Bluetooth device");
        this.data = new byte[0];
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException e) {
                Timber.tag("BluetoothConnection").e(e, "Error closing input stream");
            }
            this.inputStream = null;
        }
        if (this.outputStream != null) {
            try {
                this.outputStream.close();
            } catch (IOException e) {
                Timber.tag("BluetoothConnection").e(e, "Error closing output stream");
            }
            this.outputStream = null;
        }
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                Timber.tag("BluetoothConnection").e(e, "Error closing socket");
            }
            this.socket = null;
        }
        return this;
    }

}
