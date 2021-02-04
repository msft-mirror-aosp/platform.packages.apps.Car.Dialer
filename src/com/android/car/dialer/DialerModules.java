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

package com.android.car.dialer;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.preference.PreferenceManager;

import com.android.car.dialer.bluetooth.UiBluetoothMonitor;
import com.android.car.dialer.inject.Qualifiers;

import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/** Dialer modules. */
public final class DialerModules {

    /** Application level module. */
    @InstallIn(SingletonComponent.class)
    @Module
    public static final class BaseModule {

        @Singleton
        @Provides
        static SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    /** Module providing dependencies for single hfp connection. */
    @InstallIn(SingletonComponent.class)
    @Module
    public static final class SingleHfpModule {
        @Singleton
        @Qualifiers.Bluetooth
        @Provides
        static LiveData<Integer> provideBluetoothStateLiveData(
                UiBluetoothMonitor uiBluetoothMonitor) {
            return uiBluetoothMonitor.getBluetoothStateLiveData();
        }

        @Singleton
        @Qualifiers.Bluetooth
        @Provides
        static LiveData<Set<BluetoothDevice>> provideBluetoothPairListLiveData(
                UiBluetoothMonitor uiBluetoothMonitor) {
            return uiBluetoothMonitor.getPairListLiveData();
        }

        @Singleton
        @Qualifiers.Hfp
        @Provides
        static LiveData<List<BluetoothDevice>> provideHfpDeviceListLiveData(
                UiBluetoothMonitor uiBluetoothMonitor) {
            return uiBluetoothMonitor.getHfpDeviceListLiveData();
        }

        @Singleton
        @Qualifiers.Hfp
        @Provides
        static LiveData<BluetoothDevice> provideCurrentHfpDeviceLiveData(
                @Qualifiers.Hfp LiveData<List<BluetoothDevice>> hfpDeviceListLiveData) {
            return Transformations.map(hfpDeviceListLiveData, (devices) ->
                    devices != null && !devices.isEmpty()
                            ? devices.get(0)
                            : null);
        }

        @Singleton
        @Qualifiers.Hfp
        @Provides
        static LiveData<Boolean> hasHfpDeviceConnectedLiveData(
                @Qualifiers.Hfp LiveData<List<BluetoothDevice>> hfpDeviceListLiveData) {
            return Transformations.map(hfpDeviceListLiveData,
                    devices -> devices != null && !devices.isEmpty());
        }
    }

    /** Do not initialize. */
    private DialerModules() {
    }
}
