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

import static com.android.dx.mockito.inline.extended.ExtendedMockito.mock;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.telecom.TelecomManager;

import androidx.annotation.NonNull;

/**
 * A fake implementation of Android framework services provider.
 */
public class AndroidFrameworkImpl implements AndroidFramework {

    private static AndroidFrameworkImpl sFakeAndroidFramework;

    private FakeBluetoothAdapter mFakeBluetoothAdapter;
    private BluetoothAdapter mSpiedBluetoothAdapter;

    private FakeTelecomManager mFakeTelecomManager;
    private TelecomManager mSpiedTelecomManager;

    /**
     * Returns the single instance of {@link AndroidFrameworkImpl}.
     */
    public static AndroidFrameworkImpl get() {
        if (sFakeAndroidFramework == null) {
            sFakeAndroidFramework = new AndroidFrameworkImpl();
        }
        return sFakeAndroidFramework;
    }

    private AndroidFrameworkImpl() {}

    @Override
    public void init(@NonNull Application applicationContext) {
        mFakeBluetoothAdapter = new FakeBluetoothAdapter();
        mSpiedBluetoothAdapter = mFakeBluetoothAdapter.getBluetoothAdapter();

        mFakeTelecomManager = new FakeTelecomManager(applicationContext);
        mSpiedTelecomManager = mFakeTelecomManager.getTelecomManager();

        AdbBroadcastReceiver adbBroadcastReceiver = new AdbBroadcastReceiver();
        adbBroadcastReceiver.registerReceiver(applicationContext);
    }

    /**
     * Virtually connect a Bluetooth phone to the fake framework.
     */
    public void connectBluetoothPhone() {
        BluetoothDevice device = mock(BluetoothDevice.class);
        mFakeBluetoothAdapter.connectHfpDevice(device);
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return mSpiedBluetoothAdapter;
    }

    @Override
    public TelecomManager getTelecomManager() {
        return mSpiedTelecomManager;
    }

    /**
     * Returns the faked TelecomManager.
     */
    public FakeTelecomManager getFakeTelecomManager() {
        return mFakeTelecomManager;
    }
}
