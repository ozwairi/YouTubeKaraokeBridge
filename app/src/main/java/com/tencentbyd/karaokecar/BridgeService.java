package com.tencentbyd.karaokecar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class BridgeService extends Service {
    static final String ACTION_START = "com.tencentbyd.karaokecar.action.START";
    static final String ACTION_STOP = "com.tencentbyd.karaokecar.action.STOP";
    private static final String CHANNEL_ID = "karaoke_bridge";
    private static final int NOTIFICATION_ID = 7107;

    private AudioTrack audioTrack;
    private Thread audioThread;
    private volatile boolean running;
    private MediaSession mediaSession;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Starting compatibility bridge…"));
        acquireWakeLock();
        KaraokeSignals.enter(this);
        BridgeLog.i(this, "BridgeService created; package=" + getPackageName());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        BridgeLog.i(this, "onStartCommand action=" + action);
        if (ACTION_STOP.equals(action)) {
            KaraokeSignals.exit(this);
            stopSelf();
            return START_NOT_STICKY;
        }
        KaraokeSignals.enter(this);
        updateNotification("Bridge active — open YouTube and test Karaoke");
        return START_STICKY;
    }

    private void createMediaSession() {
        mediaSession = new MediaSession(this, "YouTube Karaoke Bridge");
        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, "YouTube Karaoke Bridge")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "全民K歌 compatibility mode")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, -1)
                .build());
        PlaybackState state = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP)
                .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                .build();
        mediaSession.setPlaybackState(state);
        mediaSession.setActive(true);
        BridgeLog.i(this, "MediaSession active");
    }

    private void startSilentPlayback() {
        if (running) return;
        running = true;
        int sampleRate = 8000;
        int minBuffer = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(minBuffer, 4096);
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();
            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(bufferSize)
                    .build();
            audioTrack.setVolume(0.0f);
            audioTrack.play();
            audioThread = new Thread(() -> {
                byte[] silence = new byte[bufferSize];
                while (running && audioTrack != null) {
                    int result = audioTrack.write(silence, 0, silence.length, AudioTrack.WRITE_BLOCKING);
                    if (result < 0) {
                        BridgeLog.i(this, "AudioTrack write stopped with code=" + result);
                        break;
                    }
                }
            }, "BridgeSilentAudio");
            audioThread.start();
            BridgeLog.i(this, "Silent AudioTrack started, sessionId=" + audioTrack.getAudioSessionId());
        } catch (Exception e) {
            BridgeLog.e(this, "Could not start silent AudioTrack", e);
        }
    }

    private Notification buildNotification(String text) {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(this, 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent = new Intent(this, BridgeService.class).setAction(ACTION_STOP);
        PendingIntent stop = PendingIntent.getService(this, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setContentTitle("全民K歌 — YouTube Bridge")
                .setContentText(text)
                .setSmallIcon(com.tencentbyd.karaokecar.R.drawable.ic_bridge)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause, "Stop", stop).build())
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YTBridge:KeepAlive");
        wakeLock.acquire(8 * 60 * 60 * 1000L);
    }

    @Override
    public void onDestroy() {
        BridgeLog.i(this, "BridgeService destroyed");
        running = false;
        if (audioTrack != null) {
            try { audioTrack.stop(); } catch (Exception ignored) {}
            audioTrack.release();
            audioTrack = null;
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
