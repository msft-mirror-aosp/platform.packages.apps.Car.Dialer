## Build and Test Configuration

This project uses a centralized Gradle wrapper located in the `aaos-apps-gradle-project` directory.

*   **Project Name:** The Gradle project name for the Dialer application is `:car-dialer-app`.

### Robust Build and Install Command

To reliably build a specific flavor and install it, use the provided bash script located in the `tools/` directory. This script automatically handles setting the required environment variables (via `tools/setup_env.sh` if needed), stopping lingering Gradle daemons, clearing the output directory, and installing the APK.

From the root of the Dialer project, run:
```bash
./tools/build_and_install.sh [flavor] [optional_device_id]
```

If `[flavor]` is omitted, the script will interactively prompt you to select the dialer flavor (e.g., emu or fake).
If `[optional_device_id]` is omitted, the script will automatically install the APK on all connected devices/emulators. The script will automatically source `tools/setup_env.sh` to configure `ANDROID_BUILD_TOP` if it is not already set.

### Running Tests

To build and run the unit tests, execute:
```bash
cd ../libs/aaos-apps-gradle-project && ./gradlew :car-dialer-app:test
```

## Development Workflow Rules

*   **Always Run Tests:** After implementing any new feature, modifying existing logic, or applying bug fixes in the Dialer application, you **MUST** run the unit tests to ensure that everything passes and no regressions were introduced.
*   **Git Commit Policy:** Never change a commit or commit files unless explicitly stated by the user to do so.

## Fake Implementation and ADB Testing (Drift Bottle)

Project Drift Bottle enables the Car Dialer app to run without a paired phone and real devices (Android S+).

To use the fake implementation:
1. Build and install `CarDialerAppForTesting`:
   ```bash
   m CarDialerAppForTesting -j32
   adb install out/target/product/<buildTarget>/testcases/CarDialerAppForTesting/arm64/CarDialerAppForTesting.apk
   ```
   *Note: `CarDialerAppForTesting` does not work with real devices. Reinstall `CarDialerApp.apk` to test with a real device.*

2. Launch Dialer first to initialize the broadcast receiver.

3. Control the fake implementation via ADB commands:
   * **Simulate Bluetooth Connection:**
     * Connect a device: `adb shell am broadcast -a com.android.car.dialer.intent.action.connect`
     * Disconnect a device: `adb shell am broadcast -a com.android.car.dialer.intent.action.disconnect`
   * **Call Management (requires connected device):**
     * Place outgoing call: `adb shell am broadcast -a com.android.car.dialer.intent.action.addCall --es id 511`
     * Receive incoming call: `adb shell am broadcast -a com.android.car.dialer.intent.action.rcvCall --es id 511`
     * Answer call: `adb shell am broadcast -a com.android.car.dialer.intent.action.answerCall --es id 511`
     * End call: `adb shell am broadcast -a com.android.car.dialer.intent.action.endCall --es id 511`
     * Hold call: `adb shell am broadcast -a com.android.car.dialer.intent.action.holdCall --es id 511`
     * Unhold call: `adb shell am broadcast -a com.android.car.dialer.intent.action.unholdCall --es id 511`
     * Merge calls: `adb shell am broadcast -a com.android.car.dialer.intent.action.mergeCall`
     * Clear all calls: `adb shell am broadcast -a com.android.car.dialer.intent.action.clearAll`
   * **Insert Test Data:**
     * Insert Contact: `adb shell am broadcast -a com.android.car.dialer.intent.action.addContact --es name TestContact --es number 511 --es address "1600 Amphitheatre Parkway, MountainView, CA"`

For more detailed command options (like initial call states or specifying device IDs), refer to `framework/DriftBottle.md`.
