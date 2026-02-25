package com.dantsu.escposprinter.connection.ap80;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Connection to AP80 built-in thermal printer via AIDL service (net.nyx.aposservice).
 */
public class Ap80Connection extends DeviceConnection {

    private static final String TAG = "Ap80Connection";
    private static final String SERVICE_PACKAGE = "net.nyx.aposservice";
    private static final String SERVICE_ACTION = "net.nyx.aposservice.IAPosService";

    private final WeakReference<Context> contextRef;
    private IBinder serviceBinder;
    private volatile boolean bound = false;
    private CountDownLatch bindLatch;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Timber.tag(TAG).i("APosService connected: %s", name);
            serviceBinder = service;
            bound = true;
            if (bindLatch != null) {
                bindLatch.countDown();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.tag(TAG).w("APosService disconnected: %s", name);
            serviceBinder = null;
            bound = false;
        }
    };

    public Ap80Connection(Context context) {
        super();
        this.contextRef = new WeakReference<>(context.getApplicationContext());
    }

    @Override
    public boolean isConnected() {
        return bound && serviceBinder != null;
    }

    @Override
    public Ap80Connection connect() throws EscPosConnectionException {
        if (isConnected()) {
            return this;
        }

        Context context = contextRef.get();
        if (context == null) {
            throw new EscPosConnectionException("Context is null for AP80 connection");
        }

        bindLatch = new CountDownLatch(1);

        Intent intent = new Intent();
        intent.setPackage(SERVICE_PACKAGE);
        intent.setAction(SERVICE_ACTION);

        boolean bindResult;
        try {
            bindResult = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            throw new EscPosConnectionException("Failed to bind APosService: " + e.getMessage());
        }

        if (!bindResult) {
            throw new EscPosConnectionException(
                    "APosService not found. Make sure net.nyx.aposservice is installed on this device.");
        }

        try {
            if (!bindLatch.await(5, TimeUnit.SECONDS)) {
                disconnect();
                throw new EscPosConnectionException("APosService bind timeout");
            }
        } catch (InterruptedException e) {
            disconnect();
            throw new EscPosConnectionException("APosService bind interrupted");
        }

        if (!isConnected()) {
            throw new EscPosConnectionException("Failed to connect to APosService");
        }

        Timber.tag(TAG).i("AP80 printer connected via APosService");
        return this;
    }

    @Override
    public Ap80Connection disconnect() {
        bound = false;

        Context context = contextRef.get();
        if (context != null) {
            try {
                context.unbindService(serviceConnection);
            } catch (Exception e) {
                Timber.tag(TAG).e("Error unbinding APosService: %s", e.getMessage());
            }
        }

        serviceBinder = null;
        Timber.tag(TAG).i("AP80 printer disconnected");
        return this;
    }

    /**
     * Send accumulated data to the AP80 printer via AIDL.
     * write() is NOT overridden — base class accumulates data in this.data.
     * send() transmits the accumulated data via sendRawPrintData().
     */
    @Override
    public void send(int addWaitingTime) throws EscPosConnectionException {
        if (this.batchMode) {
            this.batchWaitingTime += addWaitingTime;
            return;
        }

        if (!isConnected()) {
            throw new EscPosConnectionException("AP80 printer not connected");
        }

        if (this.data.length == 0) {
            return;
        }

        byte[] dataToSend = this.data;
        this.data = new byte[0];

        try {
            Timber.tag(TAG).d("Sending %d bytes via APosService", dataToSend.length);
            int result = sendRawPrintData(dataToSend);
            if (result != 0) {
                Timber.tag(TAG).w("sendRawPrintData returned: %d", result);
            }
        } catch (RemoteException e) {
            throw new EscPosConnectionException("AP80 print failed: " + e.getMessage());
        }

        int waitingTime = addWaitingTime + dataToSend.length / this.bytesPerMs;
        if (waitingTime > 0) {
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Send raw ESC/POS data to the printer via AIDL transact.
     * Transaction ID 51 = sendRawPrintData
     */
    private int sendRawPrintData(byte[] data) throws RemoteException {
        android.os.Parcel request = android.os.Parcel.obtain();
        android.os.Parcel response = android.os.Parcel.obtain();
        try {
            request.writeInterfaceToken("net.nyx.aposservice.print.IAPosService");
            request.writeByteArray(data);
            serviceBinder.transact(51, request, response, 0);
            response.readException();
            return response.readInt();
        } finally {
            request.recycle();
            response.recycle();
        }
    }

    /**
     * Send ESC/POS command data to the printer via AIDL transact.
     * Transaction ID 19 = printEscposData
     */
    public int printEscposData(byte[] cmd) throws RemoteException {
        if (!isConnected()) {
            return -1;
        }
        android.os.Parcel request = android.os.Parcel.obtain();
        android.os.Parcel response = android.os.Parcel.obtain();
        try {
            request.writeInterfaceToken("net.nyx.aposservice.print.IAPosService");
            request.writeByteArray(cmd);
            serviceBinder.transact(19, request, response, 0);
            response.readException();
            return response.readInt();
        } finally {
            request.recycle();
            response.recycle();
        }
    }

    /**
     * Get printer status.
     * Transaction ID 4 = getPrinterStatus
     */
    public int getPrinterStatus() throws RemoteException {
        if (!isConnected()) {
            return -1;
        }
        android.os.Parcel request = android.os.Parcel.obtain();
        android.os.Parcel response = android.os.Parcel.obtain();
        try {
            request.writeInterfaceToken("net.nyx.aposservice.print.IAPosService");
            request.writeInt(8); // buffer size
            serviceBinder.transact(4, request, response, 0);
            response.readException();
            return response.readInt();
        } finally {
            request.recycle();
            response.recycle();
        }
    }

    /**
     * Print a bitmap image.
     * Transaction ID 11 = printBitmap
     *
     * @param bitmap the image to print
     * @param type   0 = normal, 1 = black/white (threshold)
     * @param align  0 = left, 1 = center, 2 = right
     * @return 0 on success
     */
    public int printBitmap(android.graphics.Bitmap bitmap, int type, int align) throws RemoteException {
        if (!isConnected()) {
            return -1;
        }
        android.os.Parcel request = android.os.Parcel.obtain();
        android.os.Parcel response = android.os.Parcel.obtain();
        try {
            request.writeInterfaceToken("net.nyx.aposservice.print.IAPosService");
            // writeTypedObject format: non-null flag (1) + object data
            request.writeInt(1);
            bitmap.writeToParcel(request, 0);
            request.writeInt(type);
            request.writeInt(align);
            serviceBinder.transact(11, request, response, 0);
            response.readException();
            return response.readInt();
        } finally {
            request.recycle();
            response.recycle();
        }
    }

    /**
     * Print text with formatting.
     * Transaction ID 7 = printText
     *
     * @param text   the text to print
     * @param format PrintTextFormat parcelable (can be null for defaults)
     * @return 0 on success
     */
    public int printText(String text, android.os.Parcelable format) throws RemoteException {
        if (!isConnected()) {
            return -1;
        }
        android.os.Parcel request = android.os.Parcel.obtain();
        android.os.Parcel response = android.os.Parcel.obtain();
        try {
            request.writeInterfaceToken("net.nyx.aposservice.print.IAPosService");
            request.writeString(text);
            if (format != null) {
                request.writeInt(1);
                format.writeToParcel(request, 0);
            } else {
                request.writeInt(0);
            }
            serviceBinder.transact(7, request, response, 0);
            response.readException();
            return response.readInt();
        } finally {
            request.recycle();
            response.recycle();
        }
    }

    /**
     * Feed paper by specified pixels.
     * Transaction ID 5 = paperOut
     *
     * @param px pixels to feed
     * @return 0 on success
     */
    public int paperOut(int px) throws RemoteException {
        if (!isConnected()) {
            return -1;
        }
        android.os.Parcel request = android.os.Parcel.obtain();
        android.os.Parcel response = android.os.Parcel.obtain();
        try {
            request.writeInterfaceToken("net.nyx.aposservice.print.IAPosService");
            request.writeInt(px);
            serviceBinder.transact(5, request, response, 0);
            response.readException();
            return response.readInt();
        } finally {
            request.recycle();
            response.recycle();
        }
    }

    /**
     * Open cash box/drawer.
     * Transaction ID 24 = openCashBox
     *
     * @return 0 on success
     */
    public int openCashBox() throws RemoteException {
        if (!isConnected()) {
            return -1;
        }
        android.os.Parcel request = android.os.Parcel.obtain();
        android.os.Parcel response = android.os.Parcel.obtain();
        try {
            request.writeInterfaceToken("net.nyx.aposservice.print.IAPosService");
            serviceBinder.transact(24, request, response, 0);
            response.readException();
            return response.readInt();
        } finally {
            request.recycle();
            response.recycle();
        }
    }

    /**
     * Set print density.
     * Transaction ID 26 = setDensity
     *
     * @param density 70-130 (default 100)
     * @return 0 on success
     */
    public int setDensity(int density) throws RemoteException {
        if (!isConnected()) {
            return -1;
        }
        android.os.Parcel request = android.os.Parcel.obtain();
        android.os.Parcel response = android.os.Parcel.obtain();
        try {
            request.writeInterfaceToken("net.nyx.aposservice.print.IAPosService");
            request.writeInt(density);
            serviceBinder.transact(26, request, response, 0);
            response.readException();
            return response.readInt();
        } finally {
            request.recycle();
            response.recycle();
        }
    }

    /**
     * Get the raw service binder for advanced operations.
     */
    public IBinder getServiceBinder() {
        return serviceBinder;
    }
}
