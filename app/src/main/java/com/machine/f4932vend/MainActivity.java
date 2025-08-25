package com.machine.f4932vend;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public class MainActivity extends Activity implements SerialPortController.Listener {

    private Spinner spPort;
    private EditText etBaud, etSlot, etPrice, etPayMode, etCard, etITime, etKAmount, etL, etOMenu, etPMenu, etPData;
    private TextView tvLog;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // works because we extend Activity

        spPort  = (Spinner)  findViewById(R.id.spPort);
        etBaud  = (EditText) findViewById(R.id.etBaud);
        tvLog   = (TextView) findViewById(R.id.tvLog);
        tvLog.setMovementMethod(ScrollingMovementMethod.getInstance());

        etSlot   = (EditText) findViewById(R.id.etSlot);
        etPrice  = (EditText) findViewById(R.id.etPrice);
        etPayMode= (EditText) findViewById(R.id.etPayMode);
        etCard   = (EditText) findViewById(R.id.etCard);
        etITime  = (EditText) findViewById(R.id.etITime);
        etKAmount= (EditText) findViewById(R.id.etKAmount);
        etL      = (EditText) findViewById(R.id.etL);
        etOMenu  = (EditText) findViewById(R.id.etOMenu);
        etPMenu  = (EditText) findViewById(R.id.etPMenu);
        etPData  = (EditText) findViewById(R.id.etPData);

        populatePorts();

        ((Button) findViewById(R.id.btnRefreshPorts)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { populatePorts(); }
        });

        ((Button) findViewById(R.id.btnOpen)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String port = spPort.getSelectedItem() != null ? (String) spPort.getSelectedItem() : "/dev/ttymxc2";
                int baud = parseIntOr(etBaud, 115200);
                SerialPortController.open(port, baud);
            }
        });

        ((Button) findViewById(R.id.btnClose)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { SerialPortController.close(); }
        });

        // Queries
        ((Button) findViewById(R.id.btnB)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.getPayInfo()); }
        });
        ((Button) findViewById(R.id.btnC)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.getSlotInfo()); }
        });
        ((Button) findViewById(R.id.btnD)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.getTemperature()); }
        });
        ((Button) findViewById(R.id.btnE)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.getKeyInfo()); }
        });
        ((Button) findViewById(R.id.btnF)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.getOtherInfo()); }
        });
        ((Button) findViewById(R.id.btnG)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.getBalance()); }
        });
        ((Button) findViewById(R.id.btnJ)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.clearBalance()); }
        });

        // Select / payment
        ((Button) findViewById(R.id.btnSelect)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Integer slot = parseIntOrNull(etSlot);
                if (slot != null) send(Protocol.selectGoods(slot)); else toast("请输入货道号");
            }
        });
        ((Button) findViewById(R.id.btnOK)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.selectOk()); }
        });
        ((Button) findViewById(R.id.btnCancel)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.selectCancel()); }
        });

        // Ship (A)
        ((Button) findViewById(R.id.btnShip)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String price = textOr(etPrice, "0.01");
                String slot  = textOr(etSlot, "1");
                int mode     = parseIntOr(etPayMode, 16);
                String card  = etCard.getText().toString().trim();
                if (card.isEmpty()) card = null;
                send(Protocol.ship(price, slot, mode, card));
            }
        });

        // I/K/L
        ((Button) findViewById(R.id.btnI)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.reTime(parseIntOr(etITime, 490))); }
        });
        ((Button) findViewById(R.id.btnK)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.returnCoin(textOr(etKAmount, "0.10"))); }
        });
        ((Button) findViewById(R.id.btnL)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.networkConnected(parseIntOr(etL, 20))); }
        });

        // M/N
        ((Button) findViewById(R.id.btnLightOn)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.lightOn()); }
        });
        ((Button) findViewById(R.id.btnLightOff)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.lightOff()); }
        });
        ((Button) findViewById(R.id.btnCool)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.coolOn()); }
        });
        ((Button) findViewById(R.id.btnHeat)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.heatOn()); }
        });
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(Protocol.hvacStop()); }
        });

        // O/P
        ((Button) findViewById(R.id.btnO)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Integer menu = parseIntOrNull(etOMenu);
                if (menu != null) send(Protocol.readMenu(menu)); else toast("请输入O菜单号");
            }
        });
        ((Button) findViewById(R.id.btnP)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Integer menu = parseIntOrNull(etPMenu);
                String data = etPData.getText().toString().trim();
                if (menu != null && !data.isEmpty()) send(Protocol.writeMenu(menu, data)); else toast("请输入P菜单号和数据");
            }
        });

        // One-tap setup 26/39
        ((Button) findViewById(R.id.btnSetupVend)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sendSeq(Arrays.asList(
                        Protocol.readMenu(26),
                        Protocol.writeMenu(26, "1"),
                        Protocol.readMenu(39),
                        Protocol.writeMenu(39, "1")
                ), 120);
            }
        });

        // Check 26/39
        ((Button) findViewById(R.id.btnCheckVendFlags)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sendSeq(Arrays.asList(Protocol.readMenu(26), Protocol.readMenu(39)), 80);
            }
        });

        // One-tap vend: slot 1 @ 88.88, payMode=16
        ((Button) findViewById(R.id.btnVend_1_8888)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                send(Protocol.ship("88.88", "1", 16, null));
            }
        });

        ((Button) findViewById(R.id.btnClear)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { tvLog.setText(""); }
        });
    }

    // Spinner data
    private void populatePorts() {
        List<String> ports = scanSerialPorts();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                MainActivity.this, android.R.layout.simple_spinner_item, ports);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPort.setAdapter(adapter);
        int idx = ports.indexOf("/dev/ttymxc2");
        spPort.setSelection(idx >= 0 ? idx : 0);
    }

    private List<String> scanSerialPorts() {
        List<String> found = new ArrayList<String>();

        // /dev
        try {
            String[] names = new File("/dev").list();
            if (names != null) {
                for (String n : names) {
                    if (n.matches("^ttymxc\\d+$") || n.matches("^ttyS\\d+$") || n.matches("^ttyUSB\\d+$") ||
                            n.matches("^ttyACM\\d+$") || n.matches("^ttyAMA\\d+$") || n.matches("^ttyHS\\d+$")  ||
                            n.matches("^ttyHSL\\d+$") || n.matches("^ttyMSM\\d+$") || n.matches("^ttyMT\\d+$")  ||
                            n.matches("^ttyMFD\\d+$") || n.matches("^ttyGS\\d+$")) {
                        found.add("/dev/" + n);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // /sys/class/tty
        try {
            File ttyDir = new File("/sys/class/tty");
            File[] entries = ttyDir.listFiles();
            if (entries != null) {
                for (File e : entries) {
                    String name = e.getName();
                    if (name.matches("^tty(mxc|S|USB|ACM|AMA|HS|HSL|MSM|MT|MFD|GS)\\d+$")) {
                        File dev = new File("/dev/" + name);
                        if (dev.exists()) found.add(dev.getAbsolutePath());
                    }
                }
            }
        } catch (Throwable ignored) {}

        // de-dup
        found = new ArrayList<String>(new LinkedHashSet<String>(found));

        // sort (Collections.sort works on API 22)
        Collections.sort(found, new Comparator<String>() {
            @Override public int compare(String a, String b) {
                int pa = a.startsWith("/dev/ttymxc") ? 0 : 1;
                int pb = b.startsWith("/dev/ttymxc") ? 0 : 1;
                if (pa != pb) return pa - pb;
                int na = trailingNumber(a);
                int nb = trailingNumber(b);
                if (na != nb) return na - nb;
                return a.compareTo(b);
            }
        });

        if (found.isEmpty()) {
            found.add("/dev/ttymxc2");
            found.add("/dev/ttymxc1");
            found.add("/dev/ttymxc3");
        }
        return found;
    }

    private int trailingNumber(String s) {
        try {
            String num = s.replaceAll(".*?(\\d+)$", "$1");
            return Integer.parseInt(num);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    // Utilities
    private void send(String frame) { SerialPortController.writeData(frame); }
    private void toast(String s) { Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show(); }
    private static String textOr(EditText et, String def) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? def : s;
    }
    private static Integer parseIntOrNull(EditText et) {
        try { return Integer.parseInt(et.getText().toString().trim()); } catch (Exception e) { return null; }
    }
    private static int parseIntOr(EditText et, int def) {
        Integer v = parseIntOrNull(et);
        return v == null ? def : v;
    }

    private void sendSeq(final List<String> frames, final long delayMs) {
        new Thread(new Runnable() {
            @Override public void run() {
                for (String f : frames) {
                    SerialPortController.writeData(f);
                    try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
                }
            }
        }, "seq-sender").start();
    }

    // Listener
    @Override public void onLog(final String line) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                tvLog.append(line + "\n");
                if (tvLog.getLayout() != null) {
                    int scrollAmount = tvLog.getLayout().getLineTop(tvLog.getLineCount()) - tvLog.getHeight();
                    tvLog.scrollTo(0, Math.max(scrollAmount, 0));
                }
            }
        });
    }
    @Override public void onOpened(boolean ok, String msg) { onLog(msg); }
    @Override public void onClosed() { onLog("Closed"); }

    @Override protected void onStart() { super.onStart(); SerialPortController.addListener(this); }
    @Override protected void onStop()  { SerialPortController.removeListener(this); super.onStop(); }
}