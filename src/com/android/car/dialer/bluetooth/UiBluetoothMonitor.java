/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.car.dialer.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.android.car.dialer.livedata.BluetoothPairListLiveData;
import com.android.car.dialer.livedata.BluetoothStateLiveData;
import com.android.car.dialer.livedata.HfpDeviceListLiveData;
import com.android.car.dialer.log.L;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Class that responsible for getting status of bluetooth connections.
 */
@Singleton
public final class UiBluetoothMonitor {
    private static final String TAG = "CD.BtMonitor";

    private final Context mContext;

    private BluetoothPairListLiveData mPairListLiveData;
    private BluetoothStateLiveData mBluetoothStateLiveData;
    private HfpDeviceListLiveData mHfpDeviceListLiveData;

    private Observer mPairListObserver;
    private Observer mBluetoothStateObserver;
    private Observer mHfpDeviceListObserver;

    @Inject
    public UiBluetoothMonitor(@ApplicationContext Context applicationContext) {
        mContext = applicationContext;

        mPairListLiveData = new BluetoothPairListLiveData(mContext);
        mBluetoothStateLiveData = new BluetoothStateLiveData(mContext);
        mHfpDeviceListLiveData = new HfpDeviceListLiveData(mContext);

        mPairListObserver = o -> L.i(TAG, "PairList is updated");
        mBluetoothStateObserver = o -> L.i(TAG, "BluetoothState is updated");
        mHfpDeviceListObserver = o -> L.i(TAG, "HfpDeviceList is updated");

        mPairListLiveData.observeForever(mPairListObserver);
        mBluetoothStateLiveData.observeForever(mBluetoothStateObserver);
        mHfpDeviceListLiveData.observeForever(mHfpDeviceListObserver);
    }

    /**
     * Stops the {@link UiBluetoothMonitor}. Call this function when Dialer goes to background.
     */
    public void tearDown() {
        removeObserver(mPairListLiveData, mPairListObserver);
        removeObserver(mBluetoothStateLiveData, mBluetoothStateObserver);
        removeObserver(mHfpDeviceListLiveData, mHfpDeviceListObserver);
    }

    /**
     * Returns a LiveData which monitors the paired device list changes.
     */
    public BluetoothPairListLiveData getPairListLiveData() {
        return mPairListLiveData;
    }

    /**
     * Returns a LiveData which monitors the Bluetooth state changes.
     */
    public BluetoothStateLiveData getBluetoothStateLiveData() {
        return mBluetoothStateLiveData;
    }

    /**
     * Returns a SingleLiveEvent which monitors whether to refresh Dialer.
     */
    public LiveData<List<BluetoothDevice>> getHfpDeviceListLiveData() {
        return mHfpDeviceListLiveData;
    }

    /**
     * Returns a LiveData which monitors the first HFP Bluetooth device on the connected device
     * list.
     */
    public LiveData<BluetoothDevice> getFirstHfpConnectedDevice() {
        return Transformations.map(mHfpDeviceListLiveData, (devices) ->
                devices != null && !devices.isEmpty()
                        ? devices.get(0)
                        : null);
    }

    /** Returns a {@link LiveData} which monitors if there are any connected HFP devices. */
    public LiveData<Boolean> hasHfpDeviceConnected() {
        return Transformations.map(mHfpDeviceListLiveData,
                devices -> devices != null && !devices.isEmpty());
    }

    private void removeObserver(LiveData liveData, Observer observer) {
        if (liveData != null && liveData.hasObservers()) {
            liveData.removeObserver(observer);
        }
    }
}
