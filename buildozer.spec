[app]
title = Payment Tracker
package.name = tracker
package.domain = org.pay
source.dir = .
source.include_exts = py
version = 1.0
requirements = python3,kivy==2.3.0,pyjnius,android
orientation = portrait
fullscreen = 0
android.api = 33
android.minapi = 26
android.archs = arm64-v8a
android.accept_sdk_license = True
android.permissions = RECEIVE_SMS,READ_SMS,SYSTEM_ALERT_WINDOW,FOREGROUND_SERVICE,POST_NOTIFICATIONS,RECEIVE_BOOT_COMPLETED
android.add_src = java
android.extra_manifest_xml = ./xml/extra_manifest.xml

[buildozer]
log_level = 2
warn_on_root = 1
