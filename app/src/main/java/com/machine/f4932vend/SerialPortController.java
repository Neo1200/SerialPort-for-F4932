package com.machine.f4932vend;

import android.util.Log;
import android_serialport_api.SerialPort;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SerialPortController {

    public interface Listener {
        void onLog(String line);
        void onOpened(boolean ok, String msg);
        void onClosed();
    }

    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<Listener>();
    private static final String TAG = "SerialPortController";
    private static final Charset ASCII = StandardCharsets.US_ASCII;

    private static SerialPort port;
    private static InputStream in;
    private static OutputStream out;

    private static final AtomicBoolean READING = new AtomicBoolean(false);
    private static Thread readThread;
    private static final StringBuilder RX = new StringBuilder();

    private SerialPortController() {}

    // ===== Public API =====

    public static void addListener(Listener l) { LISTENERS.add(l); }
    public static void removeListener(Listener l) { LISTENERS.remove(l); }

    public static void open(String path, int baudrate) {
        try {
            port = new SerialPort(new File(path), baudrate, 0);
            in = port.getInputStream();
            out = port.getOutputStream();
            for (Listener l: LISTENERS) l.onOpened(true, "Opened " + path + " @ " + baudrate);
        } catch (Throwable t) {
            for (Listener l: LISTENERS) l.onOpened(false, "Open failed: " + t.getMessage());
            return;
        }

        READING.set(true);
        readThread = new Thread(new Runnable() {
            @Override public void run() {
                byte[] buf = new byte[4096];
                while (READING.get()) {
                    try {
                        int n = in.read(buf);
                        if (n > 0) onBytes(new String(buf, 0, n, ASCII));
                        else Thread.sleep(5);
                    } catch (Throwable t) {
                        Log.e(TAG, "read error", t);
                        break;
                    }
                }
            }
        }, "serial-read");
        readThread.setDaemon(true);
        readThread.start();
    }

    public static void close() {
        READING.set(false);
        try { if (in != null) in.close(); } catch (Throwable ignored) {}
        try { if (out != null) out.close(); } catch (Throwable ignored) {}
        try { if (port != null) port.close(); } catch (Throwable ignored) {}
        in = null; out = null; port = null;
        if (readThread != null) readThread.interrupt();
        readThread = null;
        for (Listener l: LISTENERS) l.onClosed();
    }

    /** Send a fully-wrapped frame (already like $$$...%%%). */
    public static void writeData(String frame) {
        if (out == null) {
            for (Listener l: LISTENERS) l.onLog("TX ► (port closed) " + frame);
            return;
        }
        byte[] bytes = frame.getBytes(ASCII);
        try {
            out.write(bytes);
            out.flush();
            for (Listener l: LISTENERS) l.onLog("TX ► " + frame + "  (bytes=" + bytes.length + ")");
        } catch (Throwable t) {
            for (Listener l: LISTENERS) l.onLog("TX ⛔ " + t.getMessage());
        }
    }

    // ----- Convenience helpers for the H|S| selection flow -----

    /** 选货：H|S|<slot>|  → $$$|H|S|slot|bcc%%% */
    public static void selectSlot(int slotNo) {
        writeData(Protocol.selectGoods(slotNo)); // now sends H|S|slot
    }

    /** 确定：H|K|10| */
    public static void selectOk() {
        writeData(Protocol.selectOk());
    }

    /** 取消：H|K|11| */
    public static void selectCancel() {
        writeData(Protocol.selectCancel());
    }

    // ===== RX framing & parsing =====

    private static void onBytes(String chunk) {
        RX.append(chunk);
        while (true) {
            int start = RX.indexOf("$$$");
            int end = RX.indexOf("%%%");
            if (start >= 0 && end > start) {
                String frame = RX.substring(start, end + 3);
                RX.delete(0, end + 3);
                handleFrame(frame);
            } else {
                if (start > 0) RX.delete(0, start); // trim junk before $$$
                break;
            }
        }
    }

    private static void handleFrame(String frame) {
        for (Listener l: LISTENERS) l.onLog("RX ◄ " + frame);

        if (!(frame.startsWith("$$$") && frame.endsWith("%%%"))) {
            for (Listener l: LISTENERS) l.onLog("   ⛔ Invalid framing");
            return;
        }

        final String body = frame.substring(3, frame.length() - 3); // between $$$ and %%%
        final int firstPipe = body.indexOf('|');
        final int lastPipe  = body.lastIndexOf('|');
        if (firstPipe < 0 || lastPipe <= firstPipe || lastPipe == body.length() - 1) {
            for (Listener l: LISTENERS) l.onLog("   ⛔ Malformed body");
            return;
        }

        final String bccStr = body.substring(lastPipe + 1);
        final String sumPart = body.substring(firstPipe, lastPipe + 1);
        final int calc = Protocol.asciiSum(sumPart);
        boolean ok = false;
        try { ok = Integer.parseInt(bccStr) == calc; } catch (NumberFormatException ignore) {}
        for (Listener l: LISTENERS) l.onLog("   ✓ BCC " + (ok ? "OK" : "BAD") + " (calc=" + calc + ", got=" + bccStr + ")");

        String[] raw = body.split("\\|");
        ArrayList<String> pieces = new ArrayList<String>();
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] != null && raw[i].length() > 0) pieces.add(raw[i]);
        }
        if (!pieces.isEmpty()) {
            for (Listener l: LISTENERS) l.onLog("   ⇢ Parsed head=" + pieces.get(0) + " fields=" + pieces.subList(1, pieces.size()));
        }

        // Pretty print for menu flags (reply head 27)
        if (pieces.size() >= 3 && "27".equals(pieces.get(0))) {
            final String menu = pieces.get(1);
            final String value = pieces.get(2);
            if ("26".equals(menu) || "39".equals(menu) || "40".equals(menu) || "41".equals(menu)) {
                String name;
                if ("26".equals(menu)) name = "APP出货开关";
                else if ("39".equals(menu)) name = "遥控出货开关";
                else if ("40".equals(menu)) name = "支付宝开关";
                else name = "微信开关";
                String pretty = "1".equals(value) ? "ON" : ("0".equals(value) ? "OFF" : value);
                for (Listener l: LISTENERS) l.onLog("   ⚙ " + name + " = " + pretty);
            }
        }

        // Optional: UI/state frame pretty print (reply head 38 …)
        if (!pieces.isEmpty() && "38".equals(pieces.get(0))) {
            for (Listener l: LISTENERS) l.onLog("   🖥 UI/state: " + pieces);
            // If you have exact meanings per field, map them here.
        }
    }
}