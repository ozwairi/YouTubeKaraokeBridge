package com.tencentbyd.karaokecar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BridgeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "null" : intent.getAction();
        BridgeLog.i(context, "Receiver action=" + action);
        if ("com.tencentbyd.karaokecar.STOP_BRIDGE".equals(action)
                || "byd.intent.action.EXIT_KARAOKE_MODE".equals(action)) {
            KaraokeSignals.exit(context);
            context.stopService(new Intent(context, BridgeService.class));
            return;
        }
        Intent service = new Intent(context, BridgeService.class)
                .setAction(BridgeService.ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
        else context.startService(service);
    }
}
