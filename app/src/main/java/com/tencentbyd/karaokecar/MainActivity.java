package com.tencentbyd.karaokecar;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        requestNotificationPermission();
        refreshStatus();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(42, 42, 42, 42);
        root.setBackgroundColor(Color.rgb(18, 18, 18));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("全民K歌 — YouTube Karaoke Bridge", 25, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView warning = text(
                "Experimental BYD compatibility test. It does not bypass vendor signatures or protected permissions.",
                15, Color.LTGRAY);
        warning.setPadding(0, 18, 0, 24);
        warning.setGravity(Gravity.CENTER);
        root.addView(warning);

        status = text("", 14, Color.rgb(180, 220, 180));
        status.setPadding(0, 0, 0, 20);
        root.addView(status);

        root.addView(button("1. Start karaoke bridge", v -> startBridge()));
        root.addView(button("2. Open YouTube Premium", v -> openYouTube()));
        root.addView(button("Start bridge + open YouTube", v -> startAndOpen()));
        root.addView(button("Stop bridge / exit karaoke", v -> stopBridge()));
        root.addView(button("Copy diagnostic log", v -> copyDiagnostics()));
        root.addView(button("Refresh status", v -> refreshStatus()));
        return scroll;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private Button button(String label, android.view.View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        button.setLayoutParams(params);
        return button;
    }

    private void startBridge() {
        Intent service = new Intent(this, BridgeService.class).setAction(BridgeService.ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
        else startService(service);
        KaraokeSignals.enter(this);
        toast("Bridge start request sent");
        handler.postDelayed(this::refreshStatus, 500);
    }

    private void startAndOpen() {
        startBridge();
        handler.postDelayed(this::openYouTube, 900);
    }

    private void stopBridge() {
        KaraokeSignals.exit(this);
        startService(new Intent(this, BridgeService.class).setAction(BridgeService.ACTION_STOP));
        stopService(new Intent(this, BridgeService.class));
        toast("Exit request sent");
        handler.postDelayed(this::refreshStatus, 500);
    }

    private void openYouTube() {
        String[] packages = {
                "com.android.youtube.premium",
                "com.google.android.youtube",
                "com.android.youtube.music.premium"
        };
        for (String packageName : packages) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                BridgeLog.i(this, "Launching " + packageName);
                startActivity(launch);
                return;
            }
        }
        toast("No supported YouTube package was found");
    }

    private void refreshStatus() {
        boolean youtube = isInstalled("com.android.youtube.premium") || isInstalled("com.google.android.youtube");
        boolean mini = isInstalled("com.byd.minikaraoke");
        String text = "Bridge package: " + getPackageName()
                + "\nVersion: " + BuildConfig.VERSION_NAME
                + "\nYouTube installed: " + youtube
                + "\nMiniKaraoke installed: " + mini
                + "\nLog: " + BridgeLog.getLogFile(this).getAbsolutePath();
        status.setText(text);
    }

    private boolean isInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void copyDiagnostics() {
        String data = status.getText() + "\n\n" + BridgeLog.read(this);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("YouTube Karaoke Bridge diagnostics", data));
        toast("Diagnostics copied");
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 91);
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
