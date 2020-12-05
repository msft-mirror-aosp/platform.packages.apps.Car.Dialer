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

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * A fake implementation of Android framework services provider.
 */
@Singleton
public class AndroidFrameworkImpl implements AndroidFramework {

    private final Context mContext;
    private final FakeBluetoothAdapter mFakeBluetoothAdapter;
    private final AdbBroadcastReceiver mAdbBroadcastReceiver;
    private final MockCallManager mMockCallManager;

    @Inject
    AndroidFrameworkImpl(
            @ApplicationContext Context context,
            FakeBluetoothAdapter fakeBluetoothAdapter,
            AdbBroadcastReceiver adbBroadcastReceiver,
            MockCallManager mockCallManager) {

        mContext = context;
        mFakeBluetoothAdapter = fakeBluetoothAdapter;
        mAdbBroadcastReceiver = adbBroadcastReceiver;
        mMockCallManager = mockCallManager;
    }

    @Override
    public void start() {
        mAdbBroadcastReceiver.registerReceiver(mContext);
    }
}
