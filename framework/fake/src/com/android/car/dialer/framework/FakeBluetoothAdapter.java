/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.dialer.framework;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.spy;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.when;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;

import com.android.car.dialer.framework.testdata.CallLogDataHandler;
import com.android.car.dialer.framework.testdata.ContactDataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A fake BluetoothAdapter implementation.
 */
@Singleton
public final class FakeBluetoothAdapter {
    private static final String CONTACT_DATA_FILE = "ContactsForPhone%d.json";
    private static final String CALL_LOG_DATA_FILE = "CallLogForPhone%d.json";

    private boolean mIsEnabled = true;
    private int mHfpProfileConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private Map<String, SimulatedBluetoothDevice> mDeviceMap = new HashMap<>();
    private final BluetoothHeadsetClient mMockedBluetoothHeadsetClient =
            mock(BluetoothHeadsetClient.class);

    private final BluetoothAdapter mSpiedBluetoothAdapter;
    private BluetoothProfile.ServiceListener mServiceListener;

    private final CallLogDataHandler mCallLogDataHandler;
    private final ContactDataHandler mContactDataHandler;

    @Inject
    public FakeBluetoothAdapter(CallLogDataHandler callLogDataHandler,
            ContactDataHandler contactDataHandler) {
        mCallLogDataHandler = callLogDataHandler;
        mContactDataHandler = contactDataHandler;

        mSpiedBluetoothAdapter = spy(BluetoothAdapter.getDefaultAdapter());
        updateFake();
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            mServiceListener = ((BluetoothProfile.ServiceListener) args[1]);
            mServiceListener.onServiceConnected(BluetoothProfile.HEADSET_CLIENT,
                    mMockedBluetoothHeadsetClient);
            return null;
        }).when(mSpiedBluetoothAdapter)
                .getProfileProxy(any(), any(), eq(BluetoothProfile.HEADSET_CLIENT));
    }

    /**
     * Virtually connect a BluetoothDevice. Calling this function will update the Bluetooth state.
     */
    public void connectHfpDevice() {
        new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> {
            SimulatedBluetoothDevice device = prepareNewDevice();
            device.connect();
            mHfpProfileConnectionState = BluetoothProfile.STATE_CONNECTED;
            mDeviceMap.put(String.valueOf(mDeviceMap.size()), device);
            updateFake();
            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(BluetoothProfile.HEADSET_CLIENT,
                        mMockedBluetoothHeadsetClient);
            }
        });
    }

    private SimulatedBluetoothDevice prepareNewDevice() {
        String contactDataFile = String.format(CONTACT_DATA_FILE, mDeviceMap.size() + 1);
        String callLogDataFile = String.format(CALL_LOG_DATA_FILE, mDeviceMap.size() + 1);
        SimulatedBluetoothDevice simulatedBluetoothDevice = new SimulatedBluetoothDevice(
                mContactDataHandler, mCallLogDataHandler, contactDataFile, callLogDataFile);

        return simulatedBluetoothDevice;
    }

    /**
     * Virtually disconnect a BluetoothDevice.
     */
    public void disconnectHfpDevice(String id) {
        new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> {
            SimulatedBluetoothDevice simulatedBluetoothDevice = mDeviceMap.remove(id);
            simulatedBluetoothDevice.disconnect();
            if (mDeviceMap.isEmpty()) {
                mHfpProfileConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                if (mServiceListener != null) {
                    mServiceListener.onServiceDisconnected(BluetoothProfile.HEADSET_CLIENT);
                }
            }
            updateFake();
        });
    }

    /**
     * Gets a fake BluetoothAdapter.
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mSpiedBluetoothAdapter;
    }

    private void updateFake() {
        Set<BluetoothDevice> bluetoothDevices = mDeviceMap.values().stream().map(
                SimulatedBluetoothDevice::getBluetoothDevice).collect(Collectors.toSet());
        when(mMockedBluetoothHeadsetClient.getConnectedDevices())
                .thenReturn(new ArrayList<>(bluetoothDevices));
        when(mSpiedBluetoothAdapter.isEnabled()).thenReturn(mIsEnabled);
        doReturn(mHfpProfileConnectionState).when(mSpiedBluetoothAdapter)
                .getProfileConnectionState(BluetoothProfile.HEADSET_CLIENT);
        when(mSpiedBluetoothAdapter.getBondedDevices()).thenReturn(bluetoothDevices);
    }
}
