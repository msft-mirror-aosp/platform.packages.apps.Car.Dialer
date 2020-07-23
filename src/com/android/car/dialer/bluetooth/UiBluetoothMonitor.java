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

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.android.car.dialer.livedata.BluetoothHfpStateLiveData;
import com.android.car.dialer.livedata.BluetoothPairListLiveData;
import com.android.car.dialer.livedata.BluetoothStateLiveData;
import com.android.car.dialer.livedata.HfpDeviceListLiveData;
import com.android.car.dialer.log.L;

/**
 * Class that responsible for getting status of bluetooth connections.
 */
public class UiBluetoothMonitor {
    private static final String TAG = "CD.BtMonitor";

    private static UiBluetoothMonitor sUiBluetoothMonitor;

    private final Context mContext;

    private BluetoothHfpStateLiveData mHfpStateLiveData;
    private BluetoothPairListLiveData mPairListLiveData;
    private BluetoothStateLiveData mBluetoothStateLiveData;
    private HfpDeviceListLiveData mHfpDeviceListLiveData;

    private Observer mHfpStateObserver;
    private Observer mPairListObserver;
    private Observer mBluetoothStateObserver;
    private Observer mHfpDeviceListObserver;

    /**
     * Initialized a globally accessible {@link UiBluetoothMonitor} which can be retrieved by {@link
     * #get}.
     *
     * @param applicationContext Application context.
     */
    public static UiBluetoothMonitor init(Context applicationContext) {
        if (sUiBluetoothMonitor == null) {
            sUiBluetoothMonitor = new UiBluetoothMonitor(applicationContext);
        }

        return get();
    }

    /**
     * Gets the global {@link UiBluetoothMonitor} instance. Make sure {@link #init(Context)} is
     * called before calling this method.
     */
    public static UiBluetoothMonitor get() {
        if (sUiBluetoothMonitor == null) {
            throw new IllegalStateException(
                    "Call UiBluetoothMonitor.init(Context) before calling this function");
        }
        return sUiBluetoothMonitor;
    }

    private UiBluetoothMonitor(Context applicationContext) {
        mContext = applicationContext;
        mHfpStateLiveData = new BluetoothHfpStateLiveData(mContext);
        mPairListLiveData = new BluetoothPairListLiveData(mContext);
        mBluetoothStateLiveData = new BluetoothStateLiveData(mContext);
        mHfpDeviceListLiveData = new HfpDeviceListLiveData(mContext);

        mHfpStateObserver = o -> L.i(TAG, "HfpState is updated");
        mPairListObserver = o -> L.i(TAG, "PairList is updated");
        mBluetoothStateObserver = o -> L.i(TAG, "BluetoothState is updated");
        mHfpDeviceListObserver = o -> L.i(TAG, "HfpDeviceList is updated");

        mHfpStateLiveData.observeForever(mHfpStateObserver);
        mPairListLiveData.observeForever(mPairListObserver);
        mBluetoothStateLiveData.observeForever(mBluetoothStateObserver);
        mHfpDeviceListLiveData.observeForever(mHfpDeviceListObserver);
    }

    /**
     * Stops the {@link UiBluetoothMonitor}. Call this function when Dialer goes to background.
     * {@link #get()} won't return a valid {@link UiBluetoothMonitor} after calling this function.
     */
    public void tearDown() {
        removeObserver(mHfpStateLiveData, mHfpStateObserver);
        removeObserver(mPairListLiveData, mPairListObserver);
        removeObserver(mBluetoothStateLiveData, mBluetoothStateObserver);
        removeObserver(mHfpDeviceListLiveData, mHfpDeviceListObserver);

        sUiBluetoothMonitor = null;
    }

    /**
     * Returns a LiveData which monitors the HFP profile state changes.
     */
    public BluetoothHfpStateLiveData getHfpStateLiveData() {
        return mHfpStateLiveData;
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
    public HfpDeviceListLiveData getHfpDeviceListLiveData() {
        return mHfpDeviceListLiveData;
    }

    private void removeObserver(LiveData liveData, Observer observer) {
        if (liveData != null && liveData.hasObservers()) {
            liveData.removeObserver(observer);
        }
    }
}