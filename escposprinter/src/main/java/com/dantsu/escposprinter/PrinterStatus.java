package com.dantsu.escposprinter;

/**
 * Represents the status of an ESC/POS printer.
 * Status is queried using DLE EOT commands.
 */
public class PrinterStatus {

    // Printer status flags (DLE EOT 1)
    private boolean drawerOpen = false;
    private boolean online = true;

    // Offline status flags (DLE EOT 2)
    private boolean coverOpen = false;
    private boolean paperFeedActive = false;
    private boolean paperEndError = false;
    private boolean errorOccurred = false;

    // Error status flags (DLE EOT 3)
    private boolean recoverableError = false;
    private boolean autocutterError = false;
    private boolean unrecoverableError = false;
    private boolean autoRecoverableError = false;

    // Paper status flags (DLE EOT 4)
    private boolean paperNearEnd = false;
    private boolean paperEnd = false;

    // Raw response bytes
    private byte[] rawPrinterStatus;
    private byte[] rawOfflineStatus;
    private byte[] rawErrorStatus;
    private byte[] rawPaperStatus;

    // Query success flags
    private boolean printerStatusQueried = false;
    private boolean offlineStatusQueried = false;
    private boolean errorStatusQueried = false;
    private boolean paperStatusQueried = false;

    public PrinterStatus() {
    }

    /**
     * Parse printer status response (DLE EOT 1).
     */
    public void parsePrinterStatus(byte[] response) {
        if (response != null && response.length > 0) {
            this.rawPrinterStatus = response;
            this.printerStatusQueried = true;
            byte status = response[0];
            this.drawerOpen = (status & 0x04) != 0;
            this.online = (status & 0x08) == 0;
        }
    }

    /**
     * Parse offline status response (DLE EOT 2).
     */
    public void parseOfflineStatus(byte[] response) {
        if (response != null && response.length > 0) {
            this.rawOfflineStatus = response;
            this.offlineStatusQueried = true;
            byte status = response[0];
            this.coverOpen = (status & 0x04) != 0;
            this.paperFeedActive = (status & 0x08) != 0;
            this.paperEndError = (status & 0x20) != 0;
            this.errorOccurred = (status & 0x40) != 0;
        }
    }

    /**
     * Parse error status response (DLE EOT 3).
     */
    public void parseErrorStatus(byte[] response) {
        if (response != null && response.length > 0) {
            this.rawErrorStatus = response;
            this.errorStatusQueried = true;
            byte status = response[0];
            this.recoverableError = (status & 0x04) != 0;
            this.autocutterError = (status & 0x08) != 0;
            this.unrecoverableError = (status & 0x20) != 0;
            this.autoRecoverableError = (status & 0x40) != 0;
        }
    }

    /**
     * Parse paper status response (DLE EOT 4).
     */
    public void parsePaperStatus(byte[] response) {
        if (response != null && response.length > 0) {
            this.rawPaperStatus = response;
            this.paperStatusQueried = true;
            byte status = response[0];
            this.paperNearEnd = (status & 0x0C) != 0;
            this.paperEnd = (status & 0x60) != 0;
        }
    }

    // Getters

    /**
     * Check if the cash drawer is open.
     * @return true if drawer is open
     */
    public boolean isDrawerOpen() {
        return drawerOpen;
    }

    /**
     * Check if the printer is online.
     * @return true if printer is online
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Check if the printer cover is open.
     * @return true if cover is open
     */
    public boolean isCoverOpen() {
        return coverOpen;
    }

    /**
     * Check if paper feed is active (button pressed).
     * @return true if paper feed is active
     */
    public boolean isPaperFeedActive() {
        return paperFeedActive;
    }

    /**
     * Check if paper end caused an error.
     * @return true if paper end error
     */
    public boolean isPaperEndError() {
        return paperEndError;
    }

    /**
     * Check if any error has occurred.
     * @return true if error occurred
     */
    public boolean isErrorOccurred() {
        return errorOccurred;
    }

    /**
     * Check if there is a recoverable error.
     * @return true if recoverable error
     */
    public boolean isRecoverableError() {
        return recoverableError;
    }

    /**
     * Check if the autocutter has an error.
     * @return true if autocutter error
     */
    public boolean isAutocutterError() {
        return autocutterError;
    }

    /**
     * Check if there is an unrecoverable error.
     * @return true if unrecoverable error
     */
    public boolean isUnrecoverableError() {
        return unrecoverableError;
    }

    /**
     * Check if there is an auto-recoverable error.
     * @return true if auto-recoverable error
     */
    public boolean isAutoRecoverableError() {
        return autoRecoverableError;
    }

    /**
     * Check if paper is near end.
     * @return true if paper near end
     */
    public boolean isPaperNearEnd() {
        return paperNearEnd;
    }

    /**
     * Check if paper has ended.
     * @return true if paper end
     */
    public boolean isPaperEnd() {
        return paperEnd;
    }

    /**
     * Check if printer status was successfully queried.
     * @return true if status was queried
     */
    public boolean isPrinterStatusQueried() {
        return printerStatusQueried;
    }

    /**
     * Check if offline status was successfully queried.
     * @return true if status was queried
     */
    public boolean isOfflineStatusQueried() {
        return offlineStatusQueried;
    }

    /**
     * Check if error status was successfully queried.
     * @return true if status was queried
     */
    public boolean isErrorStatusQueried() {
        return errorStatusQueried;
    }

    /**
     * Check if paper status was successfully queried.
     * @return true if status was queried
     */
    public boolean isPaperStatusQueried() {
        return paperStatusQueried;
    }

    /**
     * Get raw printer status bytes.
     * @return raw response bytes or null
     */
    public byte[] getRawPrinterStatus() {
        return rawPrinterStatus;
    }

    /**
     * Get raw offline status bytes.
     * @return raw response bytes or null
     */
    public byte[] getRawOfflineStatus() {
        return rawOfflineStatus;
    }

    /**
     * Get raw error status bytes.
     * @return raw response bytes or null
     */
    public byte[] getRawErrorStatus() {
        return rawErrorStatus;
    }

    /**
     * Get raw paper status bytes.
     * @return raw response bytes or null
     */
    public byte[] getRawPaperStatus() {
        return rawPaperStatus;
    }

    /**
     * Check if the printer is ready to print (online, no errors, has paper).
     * @return true if printer is ready
     */
    public boolean isReady() {
        return online && !coverOpen && !paperEnd && !errorOccurred && !unrecoverableError;
    }

    /**
     * Check if any status was successfully queried.
     * @return true if at least one status query succeeded
     */
    public boolean hasAnyStatus() {
        return printerStatusQueried || offlineStatusQueried || errorStatusQueried || paperStatusQueried;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PrinterStatus{");
        sb.append("online=").append(online);
        sb.append(", drawerOpen=").append(drawerOpen);
        sb.append(", coverOpen=").append(coverOpen);
        sb.append(", paperNearEnd=").append(paperNearEnd);
        sb.append(", paperEnd=").append(paperEnd);
        sb.append(", errorOccurred=").append(errorOccurred);
        sb.append(", recoverableError=").append(recoverableError);
        sb.append(", unrecoverableError=").append(unrecoverableError);
        sb.append(", autocutterError=").append(autocutterError);
        sb.append(", ready=").append(isReady());
        sb.append('}');
        return sb.toString();
    }
}
