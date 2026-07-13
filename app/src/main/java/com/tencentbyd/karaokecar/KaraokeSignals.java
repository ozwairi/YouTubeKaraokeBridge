package com.tencentbyd.karaokecar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

final class KaraokeSignals {
    private static final String MINI_KARAOKE_PACKAGE = "com.byd.minikaraoke";
    private static final String MINI_KARAOKE_RECEIVER = "com.byd.minikaraoke.main.KaraokeReceiver";

    private KaraokeSignals() {}

    static void enter(Context context) {
        send(context, "byd.intent.action.ENTER_KARAOKE_MODE");
        send(context, "android.app.action.ENTER_KARAOKE_MODE");
        send(context, "byd.intent.action.SHOW_KARAOKE_VIEW");
    }

    static void exit(Context context) {
        send(context, "byd.intent.action.EXIT_KARAOKE_MODE");
        send(context, "android.app.action.EXIT_KARAOKE_MODE");
        send(context, "byd.intent.action.HIDE_KARAOKE_VIEW");
    }

    private static void send(Context context, String action) {
        Intent base = buildIntent(context, action);
        try {
            context.sendBroadcast(base);
            BridgeLog.i(context, "Sent package broadcast: " + action);
        } catch (Exception e) {
            BridgeLog.e(context, "Package broadcast failed: " + action, e);
        }

        try {
            Intent explicit = new Intent(base);
            explicit.setComponent(new ComponentName(MINI_KARAOKE_PACKAGE, MINI_KARAOKE_RECEIVER));
            context.sendBroadcast(explicit);
            BridgeLog.i(context, "Sent explicit receiver broadcast: " + action);
        } catch (Exception e) {
            BridgeLog.e(context, "Explicit receiver broadcast failed: " + action, e);
        }
    }

    private static Intent buildIntent(Context context, String action) {
        String pkg = context.getPackageName();
        Intent intent = new Intent(action);
        intent.setPackage(MINI_KARAOKE_PACKAGE);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra("packageName", pkg);
        intent.putExtra("package_name", pkg);
        intent.putExtra("callingPackageName", pkg);
        intent.putExtra("calling_package", pkg);
        intent.putExtra("calling_package_name", pkg);
        intent.putExtra("extra_calling_package_name", pkg);
        intent.putExtra("data_package_name", pkg);
        intent.putExtra("android.support.v4.app.EXTRA_CALLING_PACKAGE", pkg);
        intent.putExtra("androidx.core.app.EXTRA_CALLING_PACKAGE", pkg);
        intent.putExtra("karaoke_extra", 0);
        return intent;
    }
}
