# Generating the required assets.

In order to function, this project needs the following assets in the
process-explorer-web project:

assets/process-explorer.zip
assets/process-explorer.zip.md5sum

The project generated without those assets will not function properly.

# Logcat fix

The READ_LOGS permission is not automatically granted to applications
even when the require it. It needs to be manually granted to the
application right in the shell using the following command _as root_:

    # adb shell pm grant com.opersys.processexplorer android.permission.READ_LOGS

Of course, the device must be connected and accessible through adb
for this command to work.

# Contributors

* François-Denis Gonthier francois-denis.gonthier@opersys.com -- main developer and maintainer
* Karim Yaghmour karim.yaghmour@opersys.com -- ideas and other forms of entertainment
