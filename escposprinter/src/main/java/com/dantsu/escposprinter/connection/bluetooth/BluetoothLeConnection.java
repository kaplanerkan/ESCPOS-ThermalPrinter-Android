package com.dantsu.escposprinter.connection.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import timber.log.Timber;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Bluetooth Low Energy (BLE) connection for ESC/POS printers.
 */
public class BluetoothLeConnection extends DeviceConnection {

    private static final String TAG = "BluetoothLeConnection";

    // Common BLE printer service UUIDs
    private static final UUID[] PRINTER_SERVICE_UUIDS = {
            UUID.fromString("0000FF00-0000-1000-8000-00805F9B34FB"), // Common Chinese printers
            UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455"), // Microchip/ISSC
            UUID.fromString("E7810A71-73AE-499D-8C15-FAA9AEF0C3F2"), // Nordic UART
            UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"), // Nordic UART Service
            UUID.fromString("000018F0-0000-1000-8000-00805F9B34FB"), // Some thermal printers
    };

    // Common BLE printer write characteristic UUIDs
    private static final UUID[] WRITE_CHARACTERISTIC_UUIDS = {
            UUID.fromString("0000FF02-0000-1000-8000-00805F9B34FB"), // Common Chinese printers
            UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3"), // Microchip/ISSC
            UUID.fromString("BEF8D6C9-9C21-4C9E-B632-BD58C1009F9F"), // Some printers
            UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"), // Nordic UART TX
            UUID.fromString("0000FF01-0000-1000-8000-00805F9B34FB"), // Alternate
    };

    // Client Characteristic Configuration Descriptor
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothDevice device;
    private WeakReference<Context> contextRef;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic notifyCharacteristic;

    private volatile boolean isConnected = false;
    private volatile boolean servicesDiscovered = false;
    private CountDownLatch connectionLatch;
    private CountDownLatch servicesLatch;
    private CountDownLatch writeLatch;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ByteArrayOutputStream pendingData = new ByteArrayOutputStream();

    // BLE MTU size (default is 20 bytes for data, we request higher)
    private int mtuSize = 20;
    private static final int REQUESTED_MTU = 512;

    /**
     * Create an instance of BluetoothLeConnection.
     *
     * @param device  the BLE device
     * @param context the application context
     */
    public BluetoothLeConnection(BluetoothDevice device, Context context) {
        super();
        this.device = device;
        this.contextRef = context != null ? new WeakReference<>(context.getApplicationContext()) : null;

        // BLE-optimized settings
        this.chunkSize = 20;       // Default BLE packet size
        this.chunkDelayMs = 50;    // Delay between chunks (BLE needs more time)
        this.bytesPerMs = 2;       // Very conservative rate for BLE
    }

    /**
     * Get the BluetoothDevice.
     */
    public BluetoothDevice getDevice() {
        return this.device;
    }

    @Override
    public boolean isConnected() {
        return isConnected && bluetoothGatt != null && writeCharacteristic != null;
    }

    /**
     * Connect to the BLE printer.
     */
    @SuppressLint("MissingPermission")
    public BluetoothLeConnection connect() throws EscPosConnectionException {
        if (isConnected()) {
            return this;
        }

        Context context = contextRef != null ? contextRef.get() : null;
        if (context == null) {
            throw new EscPosConnectionException("Context is required for BLE connection");
        }

        if (device == null) {
            throw new EscPosConnectionException("BLE device is null");
        }

        connectionLatch = new CountDownLatch(1);
        servicesLatch = new CountDownLatch(1);

        // Connect on main thread
        mainHandler.post(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    bluetoothGatt = device.connectGatt(context, false, gattCallback);
                }
            } catch (Exception e) {
                Timber.tag(TAG).e( "Failed to connect GATT", e);
                connectionLatch.countDown();
            }
        });

        try {
            // Wait for connection (timeout 10 seconds)
            if (!connectionLatch.await(10, TimeUnit.SECONDS)) {
                disconnect();
                throw new EscPosConnectionException("BLE connection timeout");
            }

            if (!isConnected) {
                throw new EscPosConnectionException("Failed to connect to BLE device");
            }

            // Wait for service discovery (timeout 10 seconds)
            if (!servicesLatch.await(10, TimeUnit.SECONDS)) {
                disconnect();
                throw new EscPosConnectionException("BLE service discovery timeout");
            }

            if (!servicesDiscovered || writeCharacteristic == null) {
                disconnect();
                throw new EscPosConnectionException("No compatible print service found on BLE device");
            }

            // Update chunk size based on MTU
            this.chunkSize = Math.max(20, mtuSize - 3);
            Timber.tag(TAG).i( "BLE connected with MTU: " + mtuSize + ", chunk size: " + chunkSize);

        } catch (InterruptedException e) {
            disconnect();
            throw new EscPosConnectionException("BLE connection interrupted");
        }

        return this;
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Timber.tag(TAG).d( "onConnectionStateChange: status=" + status + ", newState=" + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                connectionLatch.countDown();

                // Request higher MTU
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    gatt.requestMtu(REQUESTED_MTU);
                }

                // Discover services
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                servicesDiscovered = false;
                connectionLatch.countDown();
                servicesLatch.countDown();
                if (writeLatch != null) {
                    writeLatch.countDown();
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuSize = mtu;
                chunkSize = Math.max(20, mtu - 3);
                Timber.tag(TAG).i( "MTU changed to: " + mtu);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Timber.tag(TAG).d( "onServicesDiscovered: status=" + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Find printer service and characteristic
                findPrinterCharacteristics(gatt);
                servicesDiscovered = true;
            }
            servicesLatch.countDown();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (writeLatch != null) {
                writeLatch.countDown();
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Timber.tag(TAG).e( "Characteristic write failed: " + status);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void findPrinterCharacteristics(BluetoothGatt gatt) {
        List<BluetoothGattService> services = gatt.getServices();

        Timber.tag(TAG).d( "Found " + services.size() + " services");

        // First, try to find known printer services
        for (UUID serviceUuid : PRINTER_SERVICE_UUIDS) {
            BluetoothGattService service = gatt.getService(serviceUuid);
            if (service != null) {
                Timber.tag(TAG).i( "Found known printer service: " + serviceUuid);
                findWriteCharacteristic(service);
                if (writeCharacteristic != null) {
                    return;
                }
            }
        }

        // If not found, search all services for writable characteristics
        for (BluetoothGattService service : services) {
            Timber.tag(TAG).d( "Service: " + service.getUuid());
            findWriteCharacteristic(service);
            if (writeCharacteristic != null) {
                Timber.tag(TAG).i( "Found write characteristic in service: " + service.getUuid());
                return;
            }
        }
    }

    private void findWriteCharacteristic(BluetoothGattService service) {
        // First try known characteristic UUIDs
        for (UUID charUuid : WRITE_CHARACTERISTIC_UUIDS) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUuid);
            if (characteristic != null && isWritable(characteristic)) {
                writeCharacteristic = characteristic;
                Timber.tag(TAG).i( "Found known write characteristic: " + charUuid);
                return;
            }
        }

        // Search for any writable characteristic
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            Timber.tag(TAG).d( "  Characteristic: " + characteristic.getUuid() + ", properties: " + characteristic.getProperties());
            if (isWritable(characteristic)) {
                writeCharacteristic = characteristic;
                Timber.tag(TAG).i( "Found writable characteristic: " + characteristic.getUuid());
                return;
            }
        }
    }

    private boolean isWritable(BluetoothGattCharacteristic characteristic) {
        int properties = characteristic.getProperties();
        return (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0;
    }

    /**
     * Send data to the BLE printer.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void write(byte[] bytes) {
        if (!isConnected() || writeCharacteristic == null || bluetoothGatt == null) {
            Timber.tag(TAG).e( "BLE printer not connected");
            return;
        }

        Timber.tag(TAG).d( "Writing " + bytes.length + " bytes, chunk size: " + chunkSize);

        // Split data into chunks based on MTU
        int offset = 0;
        while (offset < bytes.length) {
            int length = Math.min(chunkSize, bytes.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(bytes, offset, chunk, 0, length);

            // Determine write type - prefer NO_RESPONSE for printers (faster, more reliable)
            int properties = writeCharacteristic.getProperties();
            boolean supportsNoResponse = (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0;
            boolean supportsWrite = (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;

            // For printers, prefer WRITE_TYPE_NO_RESPONSE as it's faster and printers don't usually need confirmation
            int writeType;
            boolean waitForCallback;
            if (supportsNoResponse) {
                writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
                waitForCallback = false;
            } else if (supportsWrite) {
                // Try NO_RESPONSE anyway - many printers work better with it even if not advertised
                writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
                waitForCallback = false;
            } else {
                writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
                waitForCallback = true;
            }

            if (waitForCallback) {
                writeLatch = new CountDownLatch(1);
            }

            boolean writeResult;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int result = bluetoothGatt.writeCharacteristic(writeCharacteristic, chunk, writeType);
                writeResult = (result == BluetoothGatt.GATT_SUCCESS);
            } else {
                @SuppressWarnings("deprecation")
                boolean result = writeCharacteristicLegacy(chunk, writeType);
                writeResult = result;
            }

            if (!writeResult) {
                Timber.tag(TAG).e( "Failed to write characteristic at offset " + offset);
            }

            if (waitForCallback) {
                try {
                    // Wait for write callback (timeout 2 seconds)
                    if (!writeLatch.await(2, TimeUnit.SECONDS)) {
                        Timber.tag(TAG).w( "Write callback timeout at offset " + offset + " - continuing anyway");
                    }
                } catch (InterruptedException e) {
                    Timber.tag(TAG).e( "Write interrupted", e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            offset += length;

            // Delay between chunks - important for BLE stability
            int delayMs = waitForCallback ? chunkDelayMs : Math.max(chunkDelayMs, 50);
            if (offset < bytes.length && delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        Timber.tag(TAG).d( "Write complete: " + bytes.length + " bytes");
    }

    /**
     * Legacy write characteristic method for Android < 13.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("MissingPermission")
    private boolean writeCharacteristicLegacy(byte[] chunk, int writeType) {
        writeCharacteristic.setValue(chunk);
        writeCharacteristic.setWriteType(writeType);
        return bluetoothGatt.writeCharacteristic(writeCharacteristic);
    }

    /**
     * Disconnect from the BLE device.
     */
    @SuppressLint("MissingPermission")
    public BluetoothLeConnection disconnect() {
        isConnected = false;
        servicesDiscovered = false;
        writeCharacteristic = null;
        notifyCharacteristic = null;

        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception e) {
                Timber.tag(TAG).e( "Error disconnecting GATT", e);
            }
            bluetoothGatt = null;
        }

        return this;
    }
}
