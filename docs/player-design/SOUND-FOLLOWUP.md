# Sound controls follow-up — 18 October 2026

Changes:
- Equalizer updates are sent while dragging, rather than only after release.
- Stable sheet height and a pinned header keep Close accessible while the controls scroll.
- Mode labels are centered; slider values share a right-aligned column.
- Profile/preset dropdowns are anchored to their full-width fields.
- Adaptive-mode bass and stereo-width controls are disabled with an explanation because adaptive tuning overwrites these values when songs change. Manual mode enables them.
- An off-state message explains how to enable editing.

Verification:
- Debug APK and instrumentation APK compiled successfully.
- All 32 unit tests passed.
- Added regression tests for updates before releasing a slider, stable header placement across mode changes, preset selection, access to lower controls, and large-text adaptive controls.
- These new UI tests have not run: the phone is disconnected, and the test emulator did not complete startup during verification.
- The updated APK is app/build/outputs/apk/debug/app-debug.apk. This follow-up build has not been installed on the phone.

Evidence: app/build/sound-fix-build.log, app/build/sound-test-build.log, app/src/androidTest/java/com/example/auramusic/PlayerDesignTest.kt.
