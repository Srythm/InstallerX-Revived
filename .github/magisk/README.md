# Magisk module template

Template used by `.github/scripts/build-magisk-module.sh` to turn a built APK into a
flashable Magisk module that replaces the stock package installer. The layout matches
the modules published by the project (`com-android-packageinstaller-<version>.zip`).

## What it installs

| Path in module | Purpose |
| --- | --- |
| `system/priv-app/PackageInstaller/PackageInstaller.apk` | the app, built with `-PAPP_ID=com.android.packageinstaller` |
| `module.prop` | module metadata; `@VERSION_NAME@`, `@VERSION_CODE@`, `@FLAVOR_LABEL@` are filled in at package time |
| `customize.sh` | sets `root:root 0755/0644` on the shipped system tree |
| `service.sh` | clears `/data/system/package_cache` once after the first boot, then deletes itself |
| `uninstall.sh` | clears the package cache so the stock installer is rescanned after removal |
| `action.sh` | Magisk "Action" button → opens `com.rosan.installer.ui.activity.SettingsActivity` |
| `launcher.png` | module icon |
| `META-INF/com/google/android/{update-binary,updater-script}` | standard Magisk installer entry point |

## Version scheme

| Artifact | Version | Example |
| --- | --- | --- |
| stable APK | `<yy.MM>` — same shape as the upstream stable tag | `26.10` |
| module | `<yy.MM>.<yyyyMMdd>.<seq>` | `26.10.20261006.1` |

- `yy.MM` comes from the last commit date (or an explicit override), so it stays in
  sync with the Gradle-generated `versionName`.
- `yyyyMMdd` is the UTC build date; `seq` is the workflow input and only needs raising
  for a second build on the same day.
- The module's inner APK is built with `-PVERSION_NAME=<module version>`, so
  `module.prop` and the APK always report the same string.
- `versionCode` is the git commit count, taken from the APK itself.

## Why `com.android.packageinstaller`

The platform only treats an app as *the* system package installer when it owns that
package name. The ROM already allowlists the stock entry's privileged permissions, so
no `privapp-permissions` XML has to be shipped, and the app inherits the installer role
that `Settings > Apps > Default apps` and third-party installers target.

## Caveats

- Switching from another build of the same module with a **different signing key** can
  make the platform reject the replacement. Clear the app's data (or uninstall the
  previous module, reboot, then flash) if you hit a signature mismatch.
- `service.sh` deletes `/data/system/package_cache/*` on first boot; that is what forces
  `PackageManager` to rescan the replaced privileged app.
- Tested flow: flash in Magisk → reboot → the system installer is InstallerX.
