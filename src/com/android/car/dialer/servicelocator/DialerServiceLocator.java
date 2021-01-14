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

package com.android.car.dialer.servicelocator;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.telecom.TelecomManager;

import com.android.car.dialer.framework.AndroidFramework;
import com.android.car.dialer.framework.AndroidFrameworkImpl;

/**
 * Locates all kinds of services that Dialer app needs.
 */
public class DialerServiceLocator {
    private static DialerServiceLocator sDialerServiceLocator = new DialerServiceLocator();

    /**
     * Returns the single instance of DialerServiceLocator.
     */
    public static DialerServiceLocator get() {
        return sDialerServiceLocator;
    }

    private AndroidFramework mAndroidFramework;

    /**
     * A wrapper function which returns the BluetoothAdapter provided by {@link AndroidFramework}.
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mAndroidFramework.getBluetoothAdapter();
    }

    /**
     * A wrapper function which returns the TelecomManager provided by {@link AndroidFramework}.
     */
    public TelecomManager getTelecomManager() {
        return mAndroidFramework.getTelecomManager();
    }

    /**
     * Returns the AndroidFramework.
     */
    public AndroidFramework getAndroidFramework() {
        return mAndroidFramework;
    }

    /**
     * Initials the service locator with all dependencies.
     */
    public void init(Application applicationContext) {
        mAndroidFramework = AndroidFrameworkImpl.get();
        mAndroidFramework.init(applicationContext);
    }
}
