package com.karsu.thermalprinter.dialogs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Toast;

import com.dantsu.escposprinter.connection.ap80.Ap80Connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import timber.log.Timber;

/**
 * AP80 native test print helper.
 * Uses AP80's AIDL service directly for bitmap and ESC/POS text printing.
 */
public class Ap80TestPrintHelper {

    private static final String TAG = "Ap80TestPrint";
    private static final int PRINT_WIDTH = 360;

    // ESC/POS alignment
    private static final byte ALIGN_LEFT = 0;
    private static final byte ALIGN_CENTER = 1;
    private static final byte ALIGN_RIGHT = 2;

    private final Context context;
    private final Ap80Connection connection;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public Ap80TestPrintHelper(Context context, Ap80Connection connection, ExecutorService executor) {
        this.context = context;
        this.connection = connection;
        this.executor = executor;
    }

    public void execute() {
        if (connection == null || !connection.isConnected()) {
            showToast("AP80 printer not connected");
            return;
        }

        executor.execute(() -> {
            try {
                Timber.tag(TAG).i("Starting AP80 native test print...");

                // Initialize printer
                sendRaw(new byte[]{0x1B, 0x40}); // ESC @ = Initialize printer
                // GS L nL nH = Set left margin (16 dots ≈ 2mm)
                sendRaw(new byte[]{0x1D, 0x4C, 16, 0});

                // 1. Print demo logo bitmap
                Bitmap logo = createDemoLogo(PRINT_WIDTH, 160);
                int result = connection.printBitmap(logo, 1, 1);
                Timber.tag(TAG).d("printBitmap logo result: %d", result);
                logo.recycle();

                // 2. Header
                printLine("AP80 TEST PRINT", ALIGN_CENTER, true, true);
                printLine("================================", ALIGN_CENTER, false, false);

                // 3. Date/time
                String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                printLine(dateTime, ALIGN_CENTER, false, false);
                printLine("", ALIGN_LEFT, false, false);

                // 4. Item list
                printLine("Item           Qty   Price", ALIGN_LEFT, false, false);
                printLine("------------------------------", ALIGN_LEFT, false, false);
                printLine("Espresso        2     4.00", ALIGN_LEFT, false, false);
                printLine("Cappuccino      1     3.50", ALIGN_LEFT, false, false);
                printLine("Cheesecake      1     5.50", ALIGN_LEFT, false, false);
                printLine("------------------------------", ALIGN_LEFT, false, false);

                // 5. Total
                printLine("TOTAL:    13.00 EUR", ALIGN_RIGHT, true, false);
                printLine("", ALIGN_LEFT, false, false);

                // 6. Footer bitmap
                Bitmap footer = createReceiptFooter(PRINT_WIDTH, 80);
                result = connection.printBitmap(footer, 1, 1);
                Timber.tag(TAG).d("printBitmap footer result: %d", result);
                footer.recycle();

                // 7. Thank you + feed
                printLine("", ALIGN_CENTER, false, false);
                printLine("Thank you!", ALIGN_CENTER, false, false);
                printLine("", ALIGN_LEFT, false, false);

                // Feed paper
                connection.paperOut(80);

                showToast("AP80 test print completed!");
                Timber.tag(TAG).i("AP80 test print completed successfully");

            } catch (RemoteException e) {
                Timber.tag(TAG).e(e, "AP80 print remote error");
                showToast("AP80 print error: " + e.getMessage());
            } catch (IOException e) {
                Timber.tag(TAG).e(e, "AP80 print IO error");
                showToast("AP80 print error: " + e.getMessage());
            }
        });
    }

    /**
     * Print a single line using ESC/POS commands via sendRawPrintData.
     */
    private void printLine(String text, byte align, boolean bold, boolean big) throws RemoteException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // ESC a n = Set alignment
        out.write(new byte[]{0x1B, 0x61, align});

        // ESC E n = Bold on/off
        out.write(new byte[]{0x1B, 0x45, (byte) (bold ? 1 : 0)});

        // GS ! n = Character size (0x00=normal, 0x11=double width+height)
        out.write(new byte[]{0x1D, 0x21, (byte) (big ? 0x11 : 0x00)});

        // Text + line feed
        out.write(text.getBytes(StandardCharsets.ISO_8859_1));
        out.write(0x0A); // LF

        // Reset bold after printing
        if (bold) {
            out.write(new byte[]{0x1B, 0x45, 0x00});
        }
        if (big) {
            out.write(new byte[]{0x1D, 0x21, 0x00});
        }

        sendRaw(out.toByteArray());
    }

    /**
     * Send raw bytes to printer via AIDL.
     */
    private void sendRaw(byte[] data) throws RemoteException {
        connection.printEscposData(data);
    }

    /**
     * Creates a demo logo bitmap.
     */
    private Bitmap createDemoLogo(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        float cx = width / 2f;

        // Border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);
        RectF rect = new RectF(10, 8, width - 10, height - 8);
        canvas.drawRoundRect(rect, 12, 12, borderPaint);

        // Printer icon
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(Color.BLACK);
        iconPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(cx - 30, 30, cx + 30, 55, iconPaint);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(2);
        canvas.drawRect(cx - 20, 20, cx + 20, 35, iconPaint);
        iconPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(cx - 38, 55, cx + 38, 63, iconPaint);

        // Title
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("THERMAL PRINTER", cx, 95, textPaint);

        // Subtitle
        textPaint.setTextSize(16);
        textPaint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("AP80 - Demo Test Page", cx, 118, textPaint);

        // Line
        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(2);
        canvas.drawLine(30, 135, width - 30, 135, linePaint);

        return bitmap;
    }

    /**
     * Creates a small receipt footer bitmap.
     */
    private Bitmap createReceiptFooter(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        float cx = width / 2f;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(14);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.MONOSPACE);

        canvas.drawText("* * * * * * * * * * *", cx, 22, textPaint);
        canvas.drawText("Powered by ESC/POS Thermal Printer", cx, 42, textPaint);
        canvas.drawText("github.com/kaplanerkan", cx, 60, textPaint);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(1);
        canvas.drawLine(30, 72, width - 30, 72, linePaint);

        return bitmap;
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}
