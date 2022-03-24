# Drift Bottle User Manual

## Overview
Project Drift Bottle aims to enable the Car Dialer app to run without a paired phone and real
devices.
<!-- More details can be found here: [go/aae-project-drift-bottle](go/aae-project-drift-bottle). -->

## User Manual
Project Drift Bottle implementation is only available on **Android S+**. To run Drift Bottle, please
build and install `CarDialerAppForTesting` target by following the steps below:

```
cd %rRepoRoo%/packages/apps/Car/Dialer
m CarDialerAppForTesting -j32
adb install %rRepoRoo%/out/target/product/%buildTarget%/testcases/CarDialerAppForTesting/arm64/CarDialerAppForTesting.apk
```
**<sub><sup>Note: the apk path above depends on your build of aosp. If the commands does not work,
try to find the .apk file under `out/target/product/%buildTarget%/testcases/CarDialerAppForTesting`
directory or run `installmod CarDialerAppForTesting`.</sup></sub>**

`CarDialerAppForTesting` will replace the original Dialer app. And you are ready to use adb commands
to control the Dialer behaviors.

One caveat is that `CarDialerAppForTesting` doesn’t work with real devices. If you want to test with
a real device, please reinstall the `CarDialerApp.apk`. (It is technically feasible to allow
Drift Bottle to run with both fake and real data but this is a feature considered to be built
later).

### Supported Commands
Adb commands only work after Dialer is launched for the first time. It requires the broadcast
receiver to be initialized. After launching Dialer, a fake bluetooth device must be connected. This
is possible with the connect command below.

* Simulate Bluetooth Connection
  * Connect a device

    This command is used to establish a fake bluetooth connection to Dialer. It must be called first
    to enable access to all call related commands. The fake connection will be assigned with a fake
    device id incrementally starting from 0.
    ```
    adb shell am broadcast -a com.android.car.dialer.intent.action.connect
    ```

  * Disconnect a device

    This command is used to disconnect a fake bluetooth connection to Dialer.
    ```
    adb shell am broadcast -a com.android.car.dialer.intent.action.disconnect
    ```
    Or with a device id when multiple devices are connected:
    ```
    adb shell am broadcast -a com.android.car.dialer.intent.action.disconnect --es device_id 0
    ```

* Call Management

  All commands below may be used after a fake device has been connected:
    * Place an outgoing call
      ```
      adb shell am broadcast -a com.android.car.dialer.intent.action.addCall --es id 511
      ```
    * Receive an incoming call
      ```
      adb shell am broadcast -a com.android.car.dialer.intent.action.rcvCall --es id 511
      ```
    * End a call
      ```
      adb shell am broadcast -a com.android.car.dialer.intent.action.endCall --es id 511
      ```
    * Hold current call
      ```
      adb shell am broadcast -a com.android.car.dialer.intent.action.holdCall
      ```
    * Unhold current call
      ```
      adb shell am broadcast -a com.android.car.dialer.intent.action.unholdCall
      ```
    * Merge calls
      Merge the primary and secondary calls into a conference call. This command will only work if
      both the primary and secondary calls exist. An existing conference call is considered a
      single entity.
      ```
      adb shell am broadcast -a com.android.car.dialer.intent.action.mergeCall
      ```
    * Clear all calls
      This command will remove **ALL** calls in the call list.
      ```
      adb shell am broadcast -a com.android.car.dialer.intent.action.clearAll
      ```
* Insert Test Data
  * Insert Contact
    The following command allows user to insert test contacts for the simulated bluetooth
    connection.
    ```
    adb shell am broadcast -a com.android.car.dialer.intent.action.addContact --es name
    TestContact --es number 511 --es address "1600\ Amphitheatre\ Parkway,\ MountainView,\ CA"
    ```
    Parameters:
    * User can specify a device id when multiple devices are connected: `--es device_id 0`
    * User can specify phone number label: `--es number_label Work`
    * User can specify address label: `--es address_label Work`
    * User can create contacts with phone number only or with address only
