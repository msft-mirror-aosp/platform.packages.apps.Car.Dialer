#!/usr/bin/env python3

import subprocess
import sys

def print_menu():
    print("\n--- Drift Bottle CLI ---")
    print("1. Connect Bluetooth")
    print("2. Disconnect Bluetooth")
    print("3. Place Outgoing Call")
    print("4. Receive Incoming Call")
    print("5. Answer Call")
    print("6. End Call")
    print("7. Hold Call")
    print("8. Unhold Call")
    print("9. Merge Calls")
    print("10. Clear All Calls")
    print("11. Add Contact")
    print("12. Help (Extra Options)")
    print("13. Launch Dialer App")
    print("0. Exit")
    print("------------------------")

def print_help():
    print("\n--- Help: Extra Options ---")
    print("When placing or receiving a call, you will be prompted for a Call ID.")
    print("You can also optionally provide an initial Call State.")
    print("Valid Call States:")
    print("  0 - STATE_NEW (Call is being created)")
    print("  1 - STATE_DIALING (Outgoing call is dialing)")
    print("  2 - STATE_RINGING (Incoming call is ringing)")
    print("  3 - STATE_HOLDING (Call is on hold)")
    print("  4 - STATE_ACTIVE (Call is connected/active)")
    print("  7 - STATE_DISCONNECTED (Call has ended)")
    print("  9 - STATE_CONNECTING (Outgoing call is connecting)")
    print(" 10 - STATE_DISCONNECTING (Call is currently ending)")
    print("\nFor Add Contact, you can provide name, number, and address.")

def run_adb_broadcast(device, action, extras=None):
    cmd = ["adb", "-s", device, "shell", "am", "broadcast", "-a", action]
    if extras:
        for key, type_val, val in extras:
            cmd.extend([type_val, key, val])

    print(f"\nExecuting: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    print(result.stdout)
    if result.stderr:
        print("Error:", result.stderr)

def get_adb_device():
    result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
    lines = result.stdout.strip().split('\n')[1:] # Skip the first line 'List of devices attached'
    devices = [line.split()[0] for line in lines if line.strip() and not line.startswith('*')]

    if not devices:
        print("No ADB devices connected. Exiting...")
        sys.exit(1)

    if len(devices) == 1:
        print(f"Found 1 device: {devices[0]}. Using it automatically.")
        return devices[0]

    print("\nMultiple ADB devices found:")
    for i, dev in enumerate(devices):
        print(f"{i + 1}. {dev}")

    while True:
        try:
            choice = int(input("\nSelect a device by number: "))
            if 1 <= choice <= len(devices):
                selected = devices[choice - 1]
                print(f"Using device: {selected}")
                return selected
            print("Invalid selection.")
        except ValueError:
            print("Please enter a valid number.")

def main():
    print("Welcome to Drift Bottle interactive CLI.")
    device = get_adb_device()

    while True:
        print_menu()
        choice = input("Select an option: ").strip()

        if choice == '0':
            print("Exiting...")
            break

        elif choice == '1':
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.connect")

        elif choice == '2':
            dev_id = input("Enter device ID (leave blank to disconnect all/default): ").strip()
            extras = [("device_id", "--es", dev_id)] if dev_id else None
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.disconnect", extras)

        elif choice == '3':
            call_id = input("Enter Call ID (default: 511): ").strip() or "511"
            state = input("Enter initial state (optional, see Help for values): ").strip()
            extras = [("id", "--es", call_id)]
            if state:
                extras.append(("state", "--ei", state))
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.addCall", extras)

        elif choice == '4':
            call_id = input("Enter Call ID (default: 511): ").strip() or "511"
            state = input("Enter initial state (optional, see Help for values): ").strip()
            extras = [("id", "--es", call_id)]
            if state:
                extras.append(("state", "--ei", state))
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.rcvCall", extras)

        elif choice == '5':
            call_id = input("Enter Call ID (default: 511): ").strip() or "511"
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.answerCall", [("id", "--es", call_id)])

        elif choice == '6':
            call_id = input("Enter Call ID (default: 511): ").strip() or "511"
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.endCall", [("id", "--es", call_id)])

        elif choice == '7':
            call_id = input("Enter Call ID (default: 511): ").strip() or "511"
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.holdCall", [("id", "--es", call_id)])

        elif choice == '8':
            call_id = input("Enter Call ID (default: 511): ").strip() or "511"
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.unholdCall", [("id", "--es", call_id)])

        elif choice == '9':
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.mergeCall")

        elif choice == '10':
            run_adb_broadcast(device, "com.android.car.dialer.intent.action.clearAll")

        elif choice == '11':
            name = input("Enter Name (default: TestContact): ").strip() or "TestContact"
            number = input("Enter Number (default: 511): ").strip() or "511"
            address = input("Enter Address (optional): ").strip()

            extras = []
            if name:
                extras.append(("name", "--es", name))
            if number:
                extras.append(("number", "--es", number))
            if address:
                extras.append(("address", "--es", address))

            run_adb_broadcast(device, "com.android.car.dialer.intent.action.addContact", extras)

        elif choice == '12':
            print_help()

        elif choice == '13':
            cmd = ["adb", "-s", device, "shell", "am", "start", "-n", "com.android.car.dialer/.ui.TelecomActivity"]
            print(f"\nExecuting: {' '.join(cmd)}")
            result = subprocess.run(cmd, capture_output=True, text=True)
            print(result.stdout)
            if result.stderr:
                print("Error:", result.stderr)

        elif choice == '14':
            cmd = ["adb", "-s", device, "shell", "pm", "resolve-activity", "--components", "com.android.car.dialer/com.android.car.dialer.framework.ErrorDialogActivity"]
            result = subprocess.run(cmd, capture_output=True, text=True)
            if "No activity found" in result.stdout or result.stderr:
                print("\n❌ Fake Dialer is NOT installed on the device.")
            else:
                print("\n✅ Fake Dialer IS installed and ready to use.")

        else:
            print("Invalid option. Please try again.")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nExiting...")
        sys.exit(0)
