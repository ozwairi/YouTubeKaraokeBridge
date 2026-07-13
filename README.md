# YouTube Karaoke Bridge v1.0 (experimental)

Experimental Android compatibility bridge for a BYD infotainment unit.

## Package identity

- Application ID: `com.tencentbyd.karaokecar`
- Visible label: `全民K歌`
- Target YouTube package: `com.android.youtube.premium`

## What it does

1. Sends several known BYD/Android karaoke enter broadcasts to `com.byd.minikaraoke`.
2. Starts a foreground service with a silent `AudioTrack`.
3. Publishes an active Android `MediaSession` owned by this package.
4. Opens YouTube Premium.
5. Writes a diagnostic log in the app external-files directory.

## Important limitation

This cannot grant itself vendor-signature or privileged system permissions. If the BYD firmware requires a trusted signature, shared UID, Binder authorization, or protected audio routing, installation as a normal APK will not overcome that restriction.

## Build

GitHub Actions builds `app-debug.apk` on every push to `main`.
