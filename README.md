# Aria Bridge Lite

A tiny Android utility designed for a single recovery workflow: connect to **your own Android phone** through Wireless ADB, have `adbd` open Chrome's `localabstract:chrome_devtools_remote*` sockets, list live Chrome targets, and perform a read-only Chrome DevTools Protocol capture of any live Workday target.

## Why

Android SELinux can prevent a normal shell/Shizuku process from directly opening Chrome's private DevTools socket. A host-side ADB connection can request the `localabstract:` service from `adbd`, which is the supported boundary we need.

## Phone-only use

1. Install the APK produced by GitHub Actions.
2. Keep Wireless Debugging enabled.
3. Use split screen: Aria Bridge Lite + Android **Pair device with pairing code** dialog.
4. Enter the six-digit code in Aria Bridge Lite and tap **Pair using code**. The app auto-discovers the temporary pairing port through mDNS.
5. Tap **Connect + Scan Chrome Tabs**.
6. If the Workday target is still alive, tap **Recover Workday State**.
7. Share the generated JSON back to ChatGPT for analysis.

## Recovery capture

The current capture is read-only. It collects selected/checked option text, elements whose attributes/text look skill-related, Workday-ish `data-automation-id` content, localStorage/sessionStorage, and basic page metadata. It cannot reconstruct state that Chrome/Workday already destroyed during a refresh.

## Upstream

The build vendors `DP-Hridayan/aShellYou`'s `libadb` module at build time and adapts its ADB key/certificate manager. See `THIRD_PARTY_NOTICES.md`.
