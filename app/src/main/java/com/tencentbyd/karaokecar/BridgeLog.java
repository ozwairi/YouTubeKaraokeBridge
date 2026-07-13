package com.tencentbyd.karaokecar;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class BridgeLog {
    static final String TAG = "YT-KaraokeBridge";
    private static final Object LOCK = new Object();

    private BridgeLog() {}

    static void i(Context context, String message) {
        Log.i(TAG, message);
        append(context, "I", message);
    }

    static void e(Context context, String message, Throwable error) {
        Log.e(TAG, message, error);
        append(context, "E", message + " | " + error);
    }

    static File getLogFile(Context context) {
        File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();
        return new File(base, "bridge.log");
    }

    static String read(Context context) {
        File file = getLogFile(context);
        if (!file.exists()) return "No bridge log yet.";
        try {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            return "Could not read log: " + e;
        }
    }

    private static void append(Context context, String level, String message) {
        synchronized (LOCK) {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            try (FileWriter writer = new FileWriter(getLogFile(context), true)) {
                writer.write(time + " " + level + "/" + TAG + ": " + message + "\n");
            } catch (IOException ignored) {
                // Logcat remains available even if file writing fails.
            }
        }
    }
}
