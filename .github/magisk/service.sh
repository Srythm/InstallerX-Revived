#!/system/bin/sh
# The system package cache still refers to the replaced apk; clearing it forces
# PackageManager to re-scan the new one on the next boot.
rm -rf /data/system/package_cache/* 2>/dev/null
# One-shot: this service script removes itself after running.
rm -f "$0"
