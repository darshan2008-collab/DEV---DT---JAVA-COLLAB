package com.smartambulance.controller;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etEspIp;
    private Button btnConnect;
    private TextView tvConnStatus;
    private TextView tvLogs;

    private Button btnRed, btnYellow, btnGreen, btnBlue, btnSiren, btnOff;
    private Button btnCorridorWave, btnCancelWave;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Bind Views
        etEspIp = findViewById(R.id.etEspIp);
        btnConnect = findViewById(R.id.btnConnect);
        tvConnStatus = findViewById(R.id.tvConnStatus);
        tvLogs = findViewById(R.id.tvLogs);

        btnRed = findViewById(R.id.btnRed);
        btnYellow = findViewById(R.id.btnYellow);
        btnGreen = findViewById(R.id.btnGreen);
        btnBlue = findViewById(R.id.btnBlue);
        btnSiren = findViewById(R.id.btnSiren);
        btnOff = findViewById(R.id.btnOff);

        btnCorridorWave = findViewById(R.id.btnCorridorWave);
        btnCancelWave = findViewById(R.id.btnCancelWave);

        // 1. Connection Ping
        btnConnect.setOnClickListener(v -> {
            vibrate(30);
            String ip = getIpAddress();
            appendLog("Checking connection to http://" + ip + "/status ...");
            testConnection(ip);
        });

        // 2. Direct RGB Light Controllers
        btnRed.setOnClickListener(v -> sendEspCommand("/red", "RED LIGHT (STOP)"));
        btnYellow.setOnClickListener(v -> sendEspCommand("/yellow", "YELLOW LIGHT (CLEARANCE)"));
        btnGreen.setOnClickListener(v -> sendEspCommand("/green", "GREEN LIGHT (PRIORITY)"));
        btnBlue.setOnClickListener(v -> sendEspCommand("/blue", "BLUE LIGHT"));
        btnSiren.setOnClickListener(v -> sendEspCommand("/siren", "AMBULANCE SIREN FLASH"));
        btnOff.setOnClickListener(v -> sendEspCommand("/off", "ALL LIGHTS OFF"));

        // 3. Emergency Corridor Wave Automation
        btnCorridorWave.setOnClickListener(v -> {
            vibrate(80);
            appendLog("[CORRIDOR WAVE] Initiating Emergency Preemption Protocol...");
            // Safe phase transition: Yellow -> Green
            sendEspCommand("/yellow", "1. Initiating Yellow Clearance (3s)");
            mainHandler.postDelayed(() -> {
                sendEspCommand("/green", "2. Corridor Green Priority Enforced");
            }, 3000);
        });

        btnCancelWave.setOnClickListener(v -> {
            vibrate(40);
            appendLog("[CORRIDOR] Aborting Preemption. Restoring normal fixed signal cycle.");
            sendEspCommand("/green", "Normal Fixed Cycle Restored");
        });
    }

    private String getIpAddress() {
        String ip = etEspIp.getText().toString().trim();
        if (ip.isEmpty()) {
            ip = "192.168.4.1";
        }
        return ip;
    }

    private void vibrate(long ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(ms);
        }
    }

    private void testConnection(String ip) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + ip + "/status");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    final String resp = sb.toString();

                    mainHandler.post(() -> {
                        tvConnStatus.setText("Status: Online (HTTP 200 OK)");
                        tvConnStatus.setTextColor(Color.parseColor("#10B981"));
                        appendLog("[OK] Connected to ESP32: " + resp);
                        Toast.makeText(MainActivity.this, "Connected to ESP32!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    mainHandler.post(() -> {
                        tvConnStatus.setText("Status: Error HTTP " + code);
                        tvConnStatus.setTextColor(Color.parseColor("#EF4444"));
                        appendLog("[ERR] Server returned code: " + code);
                    });
                }
            } catch (Exception e) {
                final String err = e.getMessage();
                mainHandler.post(() -> {
                    tvConnStatus.setText("Status: Connection Failed");
                    tvConnStatus.setTextColor(Color.parseColor("#EF4444"));
                    appendLog("[ERR] Failed to reach http://" + ip + " (" + err + ")");
                    Toast.makeText(MainActivity.this, "Check ESP32 Wi-Fi Connection!", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void sendEspCommand(String endpoint, String label) {
        vibrate(40);
        String ip = getIpAddress();
        appendLog("[SEND] " + label + " -> http://" + ip + endpoint);

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + ip + endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                int code = conn.getResponseCode();
                mainHandler.post(() -> {
                    if (code == 200 || code == 303) {
                        appendLog("[ACK] ESP32 executed: " + label + " (HTTP " + code + ")");
                    } else {
                        appendLog("[WARN] Response code: " + code);
                    }
                });
            } catch (Exception e) {
                final String err = e.getMessage();
                mainHandler.post(() -> {
                    appendLog("[FAIL] Could not send to " + ip + endpoint + " (" + err + ")");
                    Toast.makeText(MainActivity.this, "ESP32 Unreachable: " + ip, Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void appendLog(String msg) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String current = tvLogs.getText().toString();
        String newLine = "[" + timestamp + "] " + msg;

        // Keep last 6 lines
        String[] lines = (current + "\n" + newLine).split("\n");
        int start = Math.max(0, lines.length - 7);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append("\n");
        }
        tvLogs.setText(sb.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
