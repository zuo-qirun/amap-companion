# AMap Companion

AMap Companion is an Android floating window auxiliary application for AMap car edition broadcast data.
It listens to navigation and cruise-related broadcasts and displays turn-by-turn prompts, traffic light countdowns, lane information, destinations, ETA info, road reminders, electronic eye alerts, speed, and some protocol details in a draggable floating window.

> This project is written and maintained by AI; code and documentation should still be verified alongside real car-machine environments.

## Cruise Traffic Light Custom Map

In cruise mode, multi-direction traffic lights (e.g., left-turn and straight-through countdowns at the same intersection) require AMap car edition to additionally broadcast the full list of `CameraLightInfoWrapper`. The original AMap usually only exposes a single direction field, so AMap Companion cannot reliably obtain the complete cruise traffic light sequence.

- Custom map skill repository: <https://github.com/zuo-qirun/amap-cruise-wrapper-skill>
- Custom map skill ZIP mirror: <https://gh-proxy.com/https://github.com/zuo-qirun/amap-cruise-wrapper-skill/archive/refs/heads/master.zip>
- Modified AMap: <https://github.com/zuo-qirun/amap-cruise-wrapper-skill/releases/download/v20260523-cruise-wrapper/amap-auto-cruise-wrapper-20260523.apk>
- Modified AMap mirror: <https://gh.llkk.cc/https://github.com/zuo-qirun/amap-cruise-wrapper-skill/releases/download/v20260523-cruise-wrapper/amap-auto-cruise-wrapper-20260523.apk>

This custom map sends additional broadcast fields `lightsData`, `lightsCount`, and `clearLights` to display and promptly clear cruise traffic light countdowns.

## Main Features

- Draggable floating window; tapping the floating window opens the main interface.
- Supports user-selected target application package names; the current target app list filters out `com.autonavi.*`.
- Supports navigation mode and cruise mode status display.
- Supports displaying lane information from the AMap AMapAuto broadcast protocol.
- Uses AMap-style lane icon resources to render complex lanes, avoiding overlap and confusion caused by simple arrow reuse.
- Traffic light countdowns use a prominent capsule style and support direction-related traffic light info; cruise multi-direction traffic lights require the custom map.
- Supports displaying destination name, remaining time, remaining distance, current road, speed, road type, electronic eyes, and road reminders.
- Supports automatically checking for updates after entering the app; when a new version is found, it shows update details and installs via Android `PackageInstaller`.
- Supports real-time floating window size adjustment via a slider; the floating window and content scale synchronously.

## Build Method

This project keeps things lightweight and does not use Gradle; it is built directly via Android SDK build-tools.
Run on a local Windows machine:

```powershell
.\build.ps1
```

After building, the following is generated:

```text
amap_companion_signed.apk
```

During server-side automated builds, the version code can be overridden via environment variables:

```bash
APP_VERSION_CODE=1760000000 APP_VERSION_NAME=20260504-abcdef0 pwsh -NoProfile -ExecutionPolicy Bypass -File ./build.ps1
```

## Automated Build and Release

The repository has GitHub Actions configured.
Each push to the `master` branch triggers GitHub Actions to automatically:

- Install Android SDK and build-tools.
- Build and sign the APK.
- Upload the APK artifact.
- Create a GitHub Release with the installable APK file attached.

Builds can also be manually triggered on the GitHub Actions page.

## Check for Updates and Auto Upgrade

The app supports setting an update server address, for example:

```text
http://your-server-ip:8787/update.json
```

After setting it, the app automatically checks for updates when entering the main interface; you can also manually tap "Check for Updates." If the server returns a `versionCode` greater than the current app version, the app first displays the version number and update log. After tapping "Update," it downloads the APK, verifies the SHA-256, and submits the installation via Android `PackageInstaller`.

If the system requires user confirmation for installation, the app automatically opens the system confirmation screen. Some car-machine ROMs may restrict ordinary apps from installing unknown-source APKs; on failure, the app displays the `PackageInstaller` return status for troubleshooting permission issues.

The server-side code is located at:

```text
update_server/
```

For Raspberry Pi Debian arm64, it is recommended to only distribute the Release APK built by GitHub Actions and not build locally on the Raspberry Pi. For detailed deployment, see:

```text
update_server/README.md
```

## Plugin Market and Local DIY Plugins

The app supports declarative `.acplugin` plugin packages for extending fonts, icon resources, UI templates, and floating window styles. Plugin packages are essentially ZIP files and do not execute third-party Java/JS code.

Minimal `plugin.json`:

```json
{
  "schemaVersion": 1,
  "id": "example.clean.dashboard",
  "name": "Clean Dashboard Style",
  "versionCode": 1,
  "versionName": "1.0.0",
  "developer": {
    "name": "Developer Name",
    "homepage": "https://example.com"
  },
  "capabilities": ["font", "icons", "ui", "overlayStyle"],
  "entry": {
    "font": "fonts/main.ttf",
    "icons": "icons/icons.json",
    "ui": "ui/dashboard.json",
    "overlayStyle": "ui/dashboard.json"
  },
  "minAppVersionCode": 1,
  "description": "Plugin description"
}
```

Plugins are installed to the public directory `/sdcard/amap_companion/plugins/{pluginId}/` first; if the device has not granted storage permission or the public directory is not writable, it automatically falls back to the app's private directory `files/plugins/{pluginId}/`. The app reads from both directories simultaneously. Font, icon, and UI template capabilities can be mixed and matched independently; only one plugin per capability type is enabled. `overlayStyle` appears as a new style in the "Floating Window Style" list; when selected, the floating window is rendered according to that plugin template. The old `/sdcard/amap_companion/diy/` fonts and cruise arrows are retained as a low-priority compatibility layer.

UI templates support fixed components: `column`, `row`, `text`, `image`, `badge`, `turnIcon`, `laneBar`, `trafficLights`, `edog`, `spacer`. Text can bind to `mode`, `roadName`, `heading`, `turnText`, `turnDistance`, `turnRoad`, `turnIcon`, `eta`, `alert`, `detail`, `limitSpeed`, `currentSpeed`, `cameraType`, and `raw.keyType`.

Example plugin source code:

```text
plugin_examples/clean_dashboard/
```

## Changelog

Project changelog:

```text
CHANGELOG.md
```

After each automated build, the server also generates a client-facing version:

```text
update_server/public/CHANGELOG.md
```

## Signing Notes

The current APK is signed with `debug.keystore` in the repository so that subsequent builds can overwrite-install on the same device.
Signing information is explained in:

```text
SIGNING.md
```

Do not replace `debug.keystore` casually, or already-installed versions may not be able to upgrade and overwrite directly.

## Applicable Scenarios

This app is mainly used for floating window display and debugging in conjunction with the AMap car edition broadcast protocol.
If using virtual location, modified AMap car edition, or different system ROMs, broadcast fields and permission behaviors may differ, and testing should be combined with the actual environment.
