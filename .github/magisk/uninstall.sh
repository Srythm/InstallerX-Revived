#!/system/bin/sh
# Drop the stale package cache so the stock installer is picked up again after
# the module is removed and the device reboots.
rm -rf /data/system/package_cache/* 2>/dev/null
