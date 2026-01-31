package com.dantsu.escposprinter.connection.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Set;

public class BluetoothConnections {
    protected BluetoothAdapter bluetoothAdapter;
    protected WeakReference<Context> contextRef;

    /**
     * Create a new instance of BluetoothConnections
     *
     * @deprecated Use {@link #BluetoothConnections(Context)} instead
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public BluetoothConnections() {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.contextRef = null;
    }

    /**
     * Create a new instance of BluetoothConnections
     *
     * @param context the application context (used for BluetoothManager on Android 12+)
     */
    @SuppressLint("MissingPermission")
    public BluetoothConnections(Context context) {
        this.contextRef = context != null ? new WeakReference<>(context.getApplicationContext()) : null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        } else {
            @SuppressWarnings("deprecation")
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            this.bluetoothAdapter = adapter;
        }
    }
    
    /**
     * Get a list of bluetooth devices available.
     * @return Return an array of BluetoothConnection instance
     */
    @SuppressLint("MissingPermission")
    @Nullable
    @SuppressWarnings("deprecation")
    public BluetoothConnection[] getList() {
        if (this.bluetoothAdapter == null) {
            return null;
        }

        if (!this.bluetoothAdapter.isEnabled()) {
            return null;
        }

        Set<BluetoothDevice> bluetoothDevicesList = this.bluetoothAdapter.getBondedDevices();
        BluetoothConnection[] bluetoothDevices = new BluetoothConnection[bluetoothDevicesList.size()];

        if (bluetoothDevicesList.size() > 0) {
            int i = 0;
            Context context = contextRef != null ? contextRef.get() : null;
            for (BluetoothDevice device : bluetoothDevicesList) {
                bluetoothDevices[i++] = new BluetoothConnection(device, context);
            }
        }

        return bluetoothDevices;
    }
}
