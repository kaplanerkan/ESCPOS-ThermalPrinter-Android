package com.dantsu.escposprinter.connection.tcp;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import timber.log.Timber;

public class TcpConnection extends DeviceConnection {
    private Socket socket = null;
    private String address;
    private int port;
    private int timeout;

    /**
     * Create un instance of TcpConnection.
     *
     * @param address IP address of the device
     * @param port    Port of the device
     */
    public TcpConnection(String address, int port) {
        this(address, port, 5000);
    }

    /**
     * Create un instance of TcpConnection.
     *
     * Overload of the above function TcpConnection()
     * Include timeout parameter in milliseconds.
     *
     * @param address IP address of the device
     * @param port    Port of the device
     * @param timeout Timeout in milliseconds to establish a connection (default: 5000ms)
     */
    public TcpConnection(String address, int port, int timeout) {
        super();
        this.address = address;
        this.port = port;
        this.timeout = timeout;

        // TCP is a reliable protocol - no need for chunking
        // Send all data at once for maximum performance
        this.chunkSize = 0;  // 0 = no chunking
        this.chunkDelayMs = 0;
    }

    /**
     * Check if the TCP device is connected by socket.
     *
     * @return true if is connected
     */
    public boolean isConnected() {
        return this.socket != null && this.socket.isConnected() && super.isConnected();
    }

    /**
     * Start socket connection with the TCP device.
     */
    public TcpConnection connect() throws EscPosConnectionException {
        if (this.isConnected()) {
            Timber.tag("TcpConnection").d("Already connected to %s:%d", this.address, this.port);
            return this;
        }

        Timber.tag("TcpConnection").i("Connecting to TCP %s:%d (timeout: %dms)", this.address, this.port, this.timeout);

        try {
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(InetAddress.getByName(this.address), this.port), this.timeout);
            this.outputStream = this.socket.getOutputStream();
            this.inputStream = this.socket.getInputStream();
            this.data = new byte[0];
            Timber.tag("TcpConnection").i("TCP connected successfully to %s:%d", this.address, this.port);
        } catch (SocketTimeoutException e) {
            Timber.tag("TcpConnection").e(e, "TCP connection timeout: %s:%d", this.address, this.port);
            this.disconnect();
            throw new EscPosConnectionException("TCP connection timeout to " + this.address + ":" + this.port);
        } catch (IOException e) {
            Timber.tag("TcpConnection").e(e, "TCP connection failed: %s:%d - %s", this.address, this.port, e.getMessage());
            this.disconnect();
            throw new EscPosConnectionException("Unable to connect to TCP device: " + e.getMessage());
        }
        return this;
    }

    /**
     * Close the socket connection with the TCP device.
     */
    public TcpConnection disconnect() {
        this.data = new byte[0];
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
                this.inputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (this.outputStream != null) {
            try {
                this.outputStream.close();
                this.outputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (this.socket != null) {
            try {
                this.socket.close();
                this.socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

}
