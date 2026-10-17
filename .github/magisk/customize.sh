# Let Magisk extract the module files automatically.
SKIPUNZIP=0

ui_print "- Installing InstallerX Revived as system/priv-app..."
ui_print "- Target package: com.android.packageinstaller"

# The APK is shipped as a privileged system app so the platform treats it as the
# stock package installer. Permissions of the replaced AOSP entry are already
# allowlisted by the ROM, which is why no privapp-permissions XML is shipped.
#
# set_perm_recursive <dir> <owner> <group> <dir_permission> <file_permission>
# 0 0     -> root:root
# 0755    -> rwxr-xr-x, required for directories
# 0644    -> rw-r--r--, required for system apks
ui_print "- Setting permissions..."
set_perm_recursive "$MODPATH/system" 0 0 0755 0644

ui_print "- Reboot to apply the new system package installer."
