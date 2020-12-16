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
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.Set;

/**
 * A fake BluetoothAdapter implementation.
 */
public class FakeBluetoothAdapter {
    private boolean mIsEnabled = true;
    private int mHfpProfileConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private final Set<BluetoothDevice> mConnectedBluetoothDevices = new ArraySet<>();
    private final BluetoothHeadsetClient mMockedBluetoothHeadsetClient =
            mock(BluetoothHeadsetClient.class);

    private final BluetoothAdapter mSpiedBluetoothAdapter;
    private BluetoothProfile.ServiceListener mServiceListener;

    public FakeBluetoothAdapter() {
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
    public void connectHfpDevice(BluetoothDevice device) {
        new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> {
            mHfpProfileConnectionState = BluetoothProfile.STATE_CONNECTED;
            mConnectedBluetoothDevices.add(device);
            updateFake();
            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(BluetoothProfile.HEADSET_CLIENT,
                        mMockedBluetoothHeadsetClient);
            }
        });
    }

    /**
     * Gets a fake BluetoothAdapter.
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mSpiedBluetoothAdapter;
    }

    private void updateFake() {
        when(mMockedBluetoothHeadsetClient.getConnectedDevices())
                .thenReturn(new ArrayList<>(mConnectedBluetoothDevices));
        when(mSpiedBluetoothAdapter.isEnabled()).thenReturn(mIsEnabled);
        doReturn(mHfpProfileConnectionState).when(mSpiedBluetoothAdapter)
                .getProfileConnectionState(BluetoothProfile.HEADSET_CLIENT);
        when(mSpiedBluetoothAdapter.getBondedDevices()).thenReturn(mConnectedBluetoothDevices);
    }
}
