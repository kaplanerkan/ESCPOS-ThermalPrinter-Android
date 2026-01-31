package com.dantsu.escposprinter.connection;

import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.IOException;
import java.io.OutputStream;

public abstract class DeviceConnection {
    protected OutputStream outputStream;
    protected byte[] data;

    /**
     * Chunk size for sending large data (default: 256 bytes).
     * Smaller chunks can improve reliability on slow connections.
     */
    protected int chunkSize = 256;

    /**
     * Delay between chunks in milliseconds (default: 10ms).
     */
    protected int chunkDelayMs = 10;

    /**
     * Bytes per millisecond for calculating wait time (default: 16).
     * Lower values = longer wait times = more reliable but slower.
     */
    protected int bytesPerMs = 16;

    public DeviceConnection() {
        this.outputStream = null;
        this.data = new byte[0];
    }

    public abstract DeviceConnection connect() throws EscPosConnectionException;
    public abstract DeviceConnection disconnect();

    /**
     * Check if OutputStream is open.
     *
     * @return true if is connected
     */
    public boolean isConnected() {
        return this.outputStream != null;
    }

    /**
     * Set the chunk size for sending large data.
     * Smaller chunks improve reliability on slow connections (like Bluetooth).
     *
     * @param chunkSize Size of each chunk in bytes (default: 256)
     * @return Fluent interface
     */
    public DeviceConnection setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * Set the delay between chunks.
     *
     * @param delayMs Delay in milliseconds (default: 10)
     * @return Fluent interface
     */
    public DeviceConnection setChunkDelay(int delayMs) {
        this.chunkDelayMs = delayMs;
        return this;
    }

    /**
     * Set the bytes per millisecond for calculating wait time.
     * Lower values result in longer wait times.
     *
     * @param bytesPerMs Bytes per millisecond (default: 16)
     * @return Fluent interface
     */
    public DeviceConnection setBytesPerMs(int bytesPerMs) {
        this.bytesPerMs = bytesPerMs;
        return this;
    }

    /**
     * Add data to send.
     */
    public void write(byte[] bytes) {
        byte[] data = new byte[bytes.length + this.data.length];
        System.arraycopy(this.data, 0, data, 0, this.data.length);
        System.arraycopy(bytes, 0, data, this.data.length, bytes.length);
        this.data = data;
    }


    /**
     * Send data to the device.
     */
    public void send() throws EscPosConnectionException {
        this.send(0);
    }

    /**
     * Send data to the device.
     */
    public void send(int addWaitingTime) throws EscPosConnectionException {
        if (!this.isConnected()) {
            throw new EscPosConnectionException("Unable to send data to device.");
        }
        try {
            // Send data in chunks for better reliability
            if (this.data.length > this.chunkSize && this.chunkSize > 0) {
                int offset = 0;
                while (offset < this.data.length) {
                    int length = Math.min(this.chunkSize, this.data.length - offset);
                    this.outputStream.write(this.data, offset, length);
                    this.outputStream.flush();
                    offset += length;

                    // Delay between chunks
                    if (offset < this.data.length && this.chunkDelayMs > 0) {
                        Thread.sleep(this.chunkDelayMs);
                    }
                }
            } else {
                this.outputStream.write(this.data);
                this.outputStream.flush();
            }

            int waitingTime = addWaitingTime + this.data.length / this.bytesPerMs;
            this.data = new byte[0];
            if (waitingTime > 0) {
                Thread.sleep(waitingTime);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new EscPosConnectionException(e.getMessage());
        }
    }
}
