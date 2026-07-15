#!/bin/bash

if [ -z "$ANDROID_BUILD_TOP" ]; then
    ANDROID_BUILD_TOP="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)"
fi

FLAVOR=$1
DEVICE_ID=$2

if [ -z "$FLAVOR" ]; then
    echo "Please select the dialer flavor to build:"
    options=("emu" "fake" "production" "quit")
    select opt in "${options[@]}"
    do
        choice="${opt:-$REPLY}"
        choice="${choice,,}"

        case $choice in
            "emu")
                FLAVOR="emu"
                break
                ;;
            "fake")
                FLAVOR="fake"
                break
                ;;
            "production")
                FLAVOR="production"
                break
                ;;
            "quit")
                exit 0
                ;;
            *) echo "Invalid option $REPLY. Please enter a number or name.";;
        esac
    done
fi

GRADLE_DIR="$ANDROID_BUILD_TOP/packages/apps/Car/libs/aaos-apps-gradle-project"
OUT_DIR="$ANDROID_BUILD_TOP/out/aaos-apps-gradle-build"

# Convert flavor to proper casing for tasks
if [ "$FLAVOR" == "emu" ] || [ "$FLAVOR" == "emulator" ]; then
    FLAVOR="emulator"
    FLAVOR_CAP="Emulator"
elif [ "$FLAVOR" == "fake" ]; then
    FLAVOR="fake"
    FLAVOR_CAP="Fake"
else
    # Default to assuming standard flavor naming
    FLAVOR=$(echo "$FLAVOR" | tr '[:upper:]' '[:lower:]')
    FLAVOR_CAP="$(tr '[:lower:]' '[:upper:]' <<< ${FLAVOR:0:1})${FLAVOR:1}"
fi

APK_PATH="$OUT_DIR/car-dialer-app/outputs/apk/$FLAVOR/debug/car-dialer-app-$FLAVOR-debug.apk"
ASSEMBLE_TASK=":car-dialer-app:assemble${FLAVOR_CAP}Debug"

cd "$GRADLE_DIR" || { echo "Failed to navigate to Gradle directory: $GRADLE_DIR"; exit 1; }

echo "Stopping any existing Gradle daemons to release file locks..."
./gradlew --stop
sleep 2

echo "Cleaning project via Gradle (running with --no-daemon to prevent future locking)..."
./gradlew :car-dialer-app:clean :car-dialer-app:testing:clean :car-dialer-app:framework:clean :car-ui-lib:clean :car-telephony-common:clean :car-messenger-common:clean :car-assist-lib:clean :oem-token-lib:clean :oem-apis:clean :car-rotary-lib:clean :car-apps-common:clean --no-daemon

echo "Building ${FLAVOR_CAP}Debug APK from scratch..."
./gradlew $ASSEMBLE_TASK --no-build-cache --no-configuration-cache --no-daemon

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

install_apk() {
    local dev=$1
    echo "Installing on $dev..."
    adb -s "$dev" install -r -g -d "$APK_PATH"
}

if [ -n "$DEVICE_ID" ]; then
    install_apk "$DEVICE_ID"
else
    # Install on all connected devices if no specific device ID is provided
    for dev in $(adb devices | grep -v "List of devices attached" | grep "device$" | awk '{print $1}'); do
        install_apk "$dev"
    done
fi
